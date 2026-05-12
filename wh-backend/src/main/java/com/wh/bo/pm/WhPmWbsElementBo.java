package com.wh.bo.pm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wh.common.ServiceException;
import com.wh.dao.pm.*;
import com.wh.entity.pm.*;
import com.wh.dao.system.SysUserDao;
import com.wh.entity.system.SysUser;
import com.wh.service.SequenceService;
import com.wh.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WhPmWbsElementBo {

    private final WhPmWbsElementDao wbsDao;
    private final WhPmWbsVersionDao versionDao;
    private final ErpProductDao productDao;
    private final ErpModuleDao moduleDao;
    private final SysUserDao sysUserDao;
    private final SequenceService sequenceService;

    public WhPmWbsElementBo(WhPmWbsElementDao wbsDao, WhPmWbsVersionDao versionDao,
                            ErpProductDao productDao, ErpModuleDao moduleDao,
                            SysUserDao sysUserDao, SequenceService sequenceService) {
        this.wbsDao = wbsDao;
        this.versionDao = versionDao;
        this.productDao = productDao;
        this.moduleDao = moduleDao;
        this.sysUserDao = sysUserDao;
        this.sequenceService = sequenceService;
    }

    /**
     * Get WBS tree for a project. Returns flat list with parent-child resolved via parentId.
     */
    public List<WhPmWbsElement> getTreeByProjectId(String projectId, String status, String keyword) {
        List<WhPmWbsElement> allElements;
        if (keyword != null && !keyword.isEmpty()) {
            allElements = wbsDao.selectByProjectIdWithKeyword(projectId, keyword);
        } else if (status != null && !status.isEmpty()) {
            allElements = wbsDao.selectByProjectIdAndStatus(projectId, status);
        } else {
            allElements = wbsDao.selectByProjectId(projectId);
        }
        fillDisplayFields(allElements);
        return buildTree(allElements);
    }

    private List<WhPmWbsElement> buildTree(List<WhPmWbsElement> flatList) {
        Map<String, WhPmWbsElement> map = new LinkedHashMap<>();
        List<WhPmWbsElement> roots = new ArrayList<>();
        for (WhPmWbsElement e : flatList) {
            map.put(e.getId(), e);
            e.setChildren(new ArrayList<>());
        }
        for (WhPmWbsElement e : flatList) {
            if (e.getParentId() != null && map.containsKey(e.getParentId())) {
                map.get(e.getParentId()).getChildren().add(e);
            } else {
                roots.add(e);
            }
        }
        return roots;
    }

    private void fillDisplayFields(List<WhPmWbsElement> elements) {
        Set<String> productIds = new HashSet<>();
        Set<String> moduleIds = new HashSet<>();
        Set<String> userIds = new HashSet<>();
        Set<String> wbsIds = new HashSet<>();

        for (WhPmWbsElement e : elements) {
            if (e.getProductId() != null) productIds.add(e.getProductId());
            if (e.getModuleId() != null) moduleIds.add(e.getModuleId());
            if (e.getOwnerId() != null) userIds.add(e.getOwnerId());
            if (e.getPlannedOwnerId() != null) userIds.add(e.getPlannedOwnerId());
            wbsIds.add(e.getId());
        }

        Map<String, String> productNameMap = new HashMap<>();
        for (String pid : productIds) {
            ErpProduct p = productDao.selectById(pid);
            if (p != null) productNameMap.put(pid, p.getProductName());
        }
        Map<String, String> moduleNameMap = new HashMap<>();
        for (String mid : moduleIds) {
            ErpModule m = moduleDao.selectById(mid);
            if (m != null) moduleNameMap.put(mid, m.getModuleName());
        }
        Map<String, String> userNameMap = new HashMap<>();
        for (String uid : userIds) {
            SysUser u = sysUserDao.selectById(uid);
            if (u != null) userNameMap.put(uid, u.getRealName());
        }
        Map<String, WhPmWbsVersion> latestVersionMap = new HashMap<>();
        for (String wid : wbsIds) {
            List<WhPmWbsVersion> versions = versionDao.selectByWbsId(wid);
            if (!versions.isEmpty()) latestVersionMap.put(wid, versions.get(0));
        }

        for (WhPmWbsElement e : elements) {
            e.setProductName(productNameMap.get(e.getProductId()));
            e.setModuleName(moduleNameMap.get(e.getModuleId()));
            e.setOwnerName(userNameMap.get(e.getOwnerId()));
            e.setPlannedOwnerName(userNameMap.get(e.getPlannedOwnerId()));
            e.setLatestVersion(latestVersionMap.get(e.getId()));
        }
    }

    public WhPmWbsElement getDetail(String id) {
        WhPmWbsElement element = wbsDao.selectById(id);
        if (element == null || "1".equals(element.getDelFlag())) {
            throw new ServiceException(404, "WBS节点不存在");
        }
        fillDisplayFields(List.of(element));
        return element;
    }

    @Transactional
    public WhPmWbsElement create(WbsCreateRequest req) {
        WhPmWbsElement element = new WhPmWbsElement();
        element.setProjectId(req.getProjectId());
        element.setParentId(req.getParentId());
        element.setName(req.getName());
        element.setDescription(req.getDescription());
        element.setElementType(req.getElementType() != null ? req.getElementType() : "TASK");
        element.setEffortEstimate(req.getEffortEstimate());
        element.setBudgetEstimate(req.getBudgetEstimate());
        element.setOwnerId(req.getOwnerId());
        element.setPlannedOwnerId(req.getPlannedOwnerId());
        element.setProductId(req.getProductId());
        element.setModuleId(req.getModuleId());
        element.setPriority(req.getPriority());
        element.setTechDifficulty(req.getTechDifficulty());
        element.setLatestPlannedEndDate(req.getPlannedEndDate());
        element.setRemarks(req.getRemarks());
        element.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        element.setStatus("NOT_STARTED");
        element.setDelFlag("0");
        element.setVerNo(0);

        // Auto-generate WBS code
        String wbsCode = generateWbsCode(req.getProjectId(), req.getParentId());
        element.setWbsCode(wbsCode);

        // Auto-set level
        if (req.getParentId() == null || req.getParentId().isEmpty()) {
            element.setLevel(1);
        } else {
            WhPmWbsElement parent = wbsDao.selectById(req.getParentId());
            element.setLevel(parent != null ? parent.getLevel() + 1 : 1);
        }

        wbsDao.insert(element);

        // Create initial version 0.1
        createVersion(element.getId(), new BigDecimal("0.1"), req.getPlannedStartDate(), req.getPlannedEndDate(), null, null);

        // Recalculate parent effort
        if (req.getParentId() != null && !req.getParentId().isEmpty()) {
            recalculateEffort(req.getParentId());
        }

        return element;
    }

    @Transactional
    public void update(String id, WbsUpdateRequest req) {
        WhPmWbsElement element = getDetail(id);

        if ("NOT_STARTED".equals(element.getStatus()) || "IN_DEVELOPMENT".equals(element.getStatus())) {
            // NOT_STARTED/IN_DEVELOPMENT: edit freely
            applyUpdate(element, req);
            wbsDao.updateById(element);

            // If planned dates changed, create new version
            if (datesChanged(element, req)) {
                createNewVersion(element, req);
            }

            // Recalculate effort up the tree
            if (element.getParentId() != null && !element.getParentId().isEmpty()) {
                recalculateEffort(element.getParentId());
            }
        } else if ("SUSPENDED".equals(element.getStatus())) {
            applyUpdate(element, req);
            wbsDao.updateById(element);
        } else {
            throw new ServiceException("已完成/已取消/已提测状态的WBS不可修改");
        }
    }

    private void applyUpdate(WhPmWbsElement element, WbsUpdateRequest req) {
        element.setName(req.getName());
        element.setDescription(req.getDescription());
        element.setElementType(req.getElementType());
        element.setEffortEstimate(req.getEffortEstimate());
        element.setBudgetEstimate(req.getBudgetEstimate());
        element.setOwnerId(req.getOwnerId());
        element.setPlannedOwnerId(req.getPlannedOwnerId());
        element.setProductId(req.getProductId());
        element.setModuleId(req.getModuleId());
        element.setPriority(req.getPriority());
        element.setTechDifficulty(req.getTechDifficulty());
        element.setRemarks(req.getRemarks());
        element.setPlannedStartDate(req.getPlannedStartDate());
        element.setLatestPlannedEndDate(req.getPlannedEndDate());
    }

    private boolean datesChanged(WhPmWbsElement element, WbsUpdateRequest req) {
        return !Objects.equals(element.getPlannedStartDate(), req.getPlannedStartDate())
            || !Objects.equals(element.getLatestPlannedEndDate(), req.getPlannedEndDate());
    }

    private void createNewVersion(WhPmWbsElement element, WbsUpdateRequest req) {
        BigDecimal maxVer = versionDao.selectMaxVersionNumber(element.getId());
        BigDecimal newVer = (maxVer != null ? maxVer : new BigDecimal("0.1")).add(new BigDecimal("0.1"));

        // Get current version's planned dates (before update) for actual dates
        List<WhPmWbsVersion> versions = versionDao.selectByWbsId(element.getId());
        String actualStart = versions.isEmpty() ? null : versions.get(0).getActualStartDate();
        String actualEnd = versions.isEmpty() ? null : versions.get(0).getActualEndDate();

        createVersion(element.getId(), newVer, req.getPlannedStartDate(), req.getPlannedEndDate(), actualStart, actualEnd);
    }

    private void createVersion(String wbsId, BigDecimal versionNumber, String plannedStart,
                               String plannedEnd, String actualStart, String actualEnd) {
        WhPmWbsVersion version = new WhPmWbsVersion();
        version.setWbsId(wbsId);
        version.setVersionNumber(versionNumber);
        version.setPlannedStartDate(plannedStart);
        version.setPlannedEndDate(plannedEnd);
        version.setActualStartDate(actualStart);
        version.setActualEndDate(actualEnd);
        version.setDelFlag("0");
        version.setVerNo(0);
        versionDao.insert(version);
    }

    @Transactional
    public void delete(String id) {
        WhPmWbsElement element = getDetail(id);
        if (!"NOT_STARTED".equals(element.getStatus()) && !"SUSPENDED".equals(element.getStatus())) {
            throw new ServiceException("只有未开始或已暂停状态的WBS可以删除");
        }
        if (wbsDao.countChildren(id) > 0) {
            throw new ServiceException("存在子节点，禁止删除");
        }
        wbsDao.physicalDeleteById(id);

        // Recalculate parent effort
        if (element.getParentId() != null && !element.getParentId().isEmpty()) {
            recalculateEffort(element.getParentId());
        }
    }

    /**
     * 100% rule: recalculate parent effort = sum of children efforts
     */
    @Transactional
    public void recalculateEffort(String nodeId) {
        WhPmWbsElement node = wbsDao.selectById(nodeId);
        if (node == null) return;

        List<WhPmWbsElement> children = wbsDao.selectChildrenByParentId(nodeId);
        if (children == null || children.isEmpty()) return;

        double totalEffort = 0;
        double totalBudget = 0;
        for (WhPmWbsElement child : children) {
            totalEffort += parseDoubleOrZero(child.getEffortEstimate());
            totalBudget += parseDoubleOrZero(child.getBudgetEstimate());
        }

        node.setEffortEstimate(String.valueOf(totalEffort));
        node.setBudgetEstimate(String.valueOf(totalBudget));
        wbsDao.updateById(node);

        // Recurse up
        if (node.getParentId() != null && !node.getParentId().isEmpty()) {
            recalculateEffort(node.getParentId());
        }
    }

    private double parseDoubleOrZero(String value) {
        if (value == null || value.isEmpty()) return 0;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Transactional
    public void suspend(String id) {
        WhPmWbsElement element = getDetail(id);
        if (!"IN_DEVELOPMENT".equals(element.getStatus())) {
            throw new ServiceException("只有开发中状态的WBS可以暂停");
        }
        element.setStatus("SUSPENDED");
        wbsDao.updateById(element);
    }

    @Transactional
    public void resume(String id) {
        WhPmWbsElement element = getDetail(id);
        if (!"SUSPENDED".equals(element.getStatus())) {
            throw new ServiceException("只有已暂停状态的WBS可以恢复");
        }
        element.setStatus("IN_DEVELOPMENT");
        wbsDao.updateById(element);
    }

    @Transactional
    public void reopen(String id) {
        WhPmWbsElement element = getDetail(id);
        if (!"COMPLETED".equals(element.getStatus()) && !"CANCELLED".equals(element.getStatus())) {
            throw new ServiceException("只有已完成或已取消状态的WBS可以重新打开");
        }
        element.setStatus("NOT_STARTED");
        wbsDao.updateById(element);
        wbsDao.update(null, new LambdaUpdateWrapper<WhPmWbsElement>()
            .eq(WhPmWbsElement::getId, id)
            .set(WhPmWbsElement::getActualEndDate, null));
    }

    @Transactional
    public void start(String id) {
        WhPmWbsElement element = getDetail(id);
        if (!"NOT_STARTED".equals(element.getStatus())) {
            throw new ServiceException("只有未开始状态的WBS可以开始");
        }
        element.setStatus("IN_DEVELOPMENT");
        element.setActualStartDate(new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()));
        wbsDao.updateById(element);
    }

    @Transactional
    public void test(String id) {
        WhPmWbsElement element = getDetail(id);
        if (!"IN_DEVELOPMENT".equals(element.getStatus())) {
            throw new ServiceException("只有开发中状态的WBS可以提测");
        }
        element.setStatus("TESTING");
        wbsDao.updateById(element);
    }

    @Transactional
    public void complete(String id) {
        WhPmWbsElement element = getDetail(id);
        if (!"TESTING".equals(element.getStatus())) {
            throw new ServiceException("只有已提测状态的WBS可以完成");
        }
        element.setStatus("COMPLETED");
        element.setActualEndDate(new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()));
        wbsDao.updateById(element);
    }

    @Transactional
    public void cancel(String id) {
        WhPmWbsElement element = getDetail(id);
        if (!"NOT_STARTED".equals(element.getStatus()) && !"IN_DEVELOPMENT".equals(element.getStatus()) && !"SUSPENDED".equals(element.getStatus())) {
            throw new ServiceException("只有未开始、开发中或已暂停状态的WBS可以取消");
        }
        element.setStatus("CANCELLED");
        wbsDao.updateById(element);
    }

    /**
     * Get version history for a WBS element
     */
    public List<WhPmWbsVersion> getVersionHistory(String wbsId) {
        WhPmWbsElement element = wbsDao.selectById(wbsId);
        if (element == null || "1".equals(element.getDelFlag())) {
            throw new ServiceException(404, "WBS节点不存在");
        }
        return versionDao.selectByWbsId(wbsId);
    }

    // ========== WBS Code Generation ==========

    private String generateWbsCode(String projectId, String parentId) {
        if (parentId == null || parentId.isEmpty()) {
            // Root node: use sequence
            long seq = sequenceService.getNextSequence("PM_WBS");
            return String.valueOf(seq);
        } else {
            // Child node: parentCode.childIndex
            WhPmWbsElement parent = wbsDao.selectById(parentId);
            if (parent == null) {
                throw new ServiceException("父节点不存在");
            }
            int childIndex = wbsDao.countChildren(parentId) + 1;
            return parent.getWbsCode() + "." + childIndex;
        }
    }

    // ========== Import/Export ==========

    @Transactional
    public WbsImportResult importWbs(org.springframework.web.multipart.MultipartFile file, String projectId) throws java.io.IOException {
        String csv = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
        String[] lines = csv.split("\n");

        WbsImportResult result = new WbsImportResult();
        result.setTotal(0);
        result.setSuccess(0);
        result.setDegraded(0);
        result.setFailed(0);
        List<WbsImportResult.ImportDetail> details = new ArrayList<>();

        // Skip header line
        // CSV format: 序号,名称,父节点名称,产品类型,产品编码,模块编码,优先级,技术难度,计划责任人,估算工时(小时),估算成本(元),描述
        List<CsvRow> csvRows = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            String[] cols = line.split(",", -1);
            CsvRow row = new CsvRow();
            row.rowNum = i + 1;
            row.name = cols.length > 1 ? cols[1].trim() : "";
            row.parentName = cols.length > 2 ? cols[2].trim() : "";
            row.productCode = cols.length > 4 ? cols[4].trim() : "";
            row.moduleCode = cols.length > 5 ? cols[5].trim() : "";
            row.priority = cols.length > 6 ? cols[6].trim() : "";
            row.techDifficulty = cols.length > 7 ? cols[7].trim() : "";
            row.plannedOwnerName = cols.length > 8 ? cols[8].trim() : "";
            row.effortEstimate = cols.length > 9 ? cols[9].trim() : "";
            row.budgetEstimate = cols.length > 10 ? cols[10].trim() : "";
            row.description = cols.length > 11 ? cols[11].trim() : "";
            csvRows.add(row);
        }

        // Pass 1: Create root nodes (parentName empty)
        Map<String, String> nameToIdMap = new LinkedHashMap<>();
        List<WhPmWbsElement> existingNodes = wbsDao.selectByProjectId(projectId);
        for (WhPmWbsElement e : existingNodes) {
            existingNodes.stream().filter(n -> n.getName().equals(e.getName())).findFirst()
                    .ifPresent(n -> nameToIdMap.putIfAbsent(n.getName(), n.getId()));
        }
        // Simpler: just build name→id from existing
        nameToIdMap.clear();
        for (WhPmWbsElement e : existingNodes) {
            nameToIdMap.put(e.getName(), e.getId());
        }

        List<CsvRow> childRows = new ArrayList<>();
        for (CsvRow row : csvRows) {
            result.setTotal(result.getTotal() + 1);
            if (row.name.isEmpty()) {
                WbsImportResult.ImportDetail d = new WbsImportResult.ImportDetail();
                d.setRow(row.rowNum);
                d.setName(row.name);
                d.setStatus("FAILED");
                d.setReason("名称为空");
                details.add(d);
                result.setFailed(result.getFailed() + 1);
                continue;
            }

            if (row.parentName.isEmpty()) {
                // Root node
                try {
                    String productId = resolveProduct(row.productCode);
                    String moduleId = resolveModule(productId, row.moduleCode);
                    String plannedOwnerId = resolveUser(row.plannedOwnerName);

                    WhPmWbsElement element = new WhPmWbsElement();
                    element.setProjectId(projectId);
                    element.setName(row.name);
                    element.setDescription(row.description);
                    element.setElementType("TASK");
                    element.setEffortEstimate(row.effortEstimate);
                    element.setBudgetEstimate(row.budgetEstimate);
                    element.setPriority(row.priority);
                    element.setTechDifficulty(row.techDifficulty);
                    element.setPlannedOwnerId(plannedOwnerId);
                    element.setProductId(productId);
                    element.setModuleId(moduleId);
                    element.setStatus("NOT_STARTED");
                    element.setDelFlag("0");
                    element.setVerNo(0);
                    element.setLevel(1);
                    element.setSortOrder(0);

                    String wbsCode = generateWbsCode(projectId, null);
                    element.setWbsCode(wbsCode);

                    wbsDao.insert(element);
                    createVersion(element.getId(), new BigDecimal("0.1"), null, null, null, null);

                    nameToIdMap.put(row.name, element.getId());
                    result.setSuccess(result.getSuccess() + 1);

                    WbsImportResult.ImportDetail d = new WbsImportResult.ImportDetail();
                    d.setRow(row.rowNum);
                    d.setName(row.name);
                    d.setStatus("SUCCESS");
                    details.add(d);
                } catch (Exception e) {
                    result.setFailed(result.getFailed() + 1);
                    WbsImportResult.ImportDetail d = new WbsImportResult.ImportDetail();
                    d.setRow(row.rowNum);
                    d.setName(row.name);
                    d.setStatus("FAILED");
                    d.setReason(e.getMessage());
                    details.add(d);
                }
            } else {
                childRows.add(row);
            }
        }

        // Pass 2: Create child nodes with 3-level parent resolution
        for (CsvRow row : childRows) {
            result.setTotal(result.getTotal() + 1); // recount
            if (row.name.isEmpty()) {
                WbsImportResult.ImportDetail d = new WbsImportResult.ImportDetail();
                d.setRow(row.rowNum);
                d.setName(row.name);
                d.setStatus("FAILED");
                d.setReason("名称为空");
                details.add(d);
                result.setFailed(result.getFailed() + 1);
                continue;
            }

            // 3-level parent resolution
            String parentId = null;
            if (nameToIdMap.containsKey(row.parentName)) {
                // Level 1: in-batch map
                parentId = nameToIdMap.get(row.parentName);
            } else {
                // Level 2: existing nodes by name
                LambdaQueryWrapper<WhPmWbsElement> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(WhPmWbsElement::getProjectId, projectId);
                wrapper.eq(WhPmWbsElement::getName, row.parentName);
                wrapper.eq(WhPmWbsElement::getDelFlag, "0");
                wrapper.last("LIMIT 1");
                WhPmWbsElement existing = wbsDao.selectOne(wrapper);
                if (existing != null) {
                    parentId = existing.getId();
                    nameToIdMap.put(row.parentName, parentId);
                }
                // Level 3: degrade to root (parentId stays null)
            }

            try {
                String productId = resolveProduct(row.productCode);
                String moduleId = resolveModule(productId, row.moduleCode);
                String plannedOwnerId = resolveUser(row.plannedOwnerName);

                WhPmWbsElement element = new WhPmWbsElement();
                element.setProjectId(projectId);
                element.setParentId(parentId);
                element.setName(row.name);
                element.setDescription(row.description);
                element.setElementType("TASK");
                element.setEffortEstimate(row.effortEstimate);
                element.setBudgetEstimate(row.budgetEstimate);
                element.setPriority(row.priority);
                element.setTechDifficulty(row.techDifficulty);
                element.setPlannedOwnerId(plannedOwnerId);
                element.setProductId(productId);
                element.setModuleId(moduleId);
                element.setStatus("NOT_STARTED");
                element.setDelFlag("0");
                element.setVerNo(0);

                if (parentId != null) {
                    WhPmWbsElement parent = wbsDao.selectById(parentId);
                    element.setLevel(parent != null ? parent.getLevel() + 1 : 1);
                } else {
                    element.setLevel(1);
                }
                element.setSortOrder(0);

                String wbsCode = generateWbsCode(projectId, parentId);
                element.setWbsCode(wbsCode);

                wbsDao.insert(element);
                createVersion(element.getId(), new BigDecimal("0.1"), null, null, null, null);

                nameToIdMap.put(row.name, element.getId());

                if (parentId == null) {
                    result.setDegraded(result.getDegraded() + 1);
                    WbsImportResult.ImportDetail d = new WbsImportResult.ImportDetail();
                    d.setRow(row.rowNum);
                    d.setName(row.name);
                    d.setStatus("DEGRADED");
                    d.setReason("未找到父节点[" + row.parentName + "]，降级为根节点");
                    details.add(d);
                } else {
                    result.setSuccess(result.getSuccess() + 1);
                    WbsImportResult.ImportDetail d = new WbsImportResult.ImportDetail();
                    d.setRow(row.rowNum);
                    d.setName(row.name);
                    d.setStatus("SUCCESS");
                    details.add(d);
                }
            } catch (Exception e) {
                result.setFailed(result.getFailed() + 1);
                WbsImportResult.ImportDetail d = new WbsImportResult.ImportDetail();
                d.setRow(row.rowNum);
                d.setName(row.name);
                d.setStatus("FAILED");
                d.setReason(e.getMessage());
                details.add(d);
            }
        }

        result.setDetails(details);
        return result;
    }

    private String resolveProduct(String productCode) {
        if (productCode == null || productCode.isEmpty()) return null;
        ErpProduct p = productDao.selectOne(new LambdaQueryWrapper<ErpProduct>()
                .eq(ErpProduct::getProductCode, productCode).eq(ErpProduct::getDelFlag, "0").last("LIMIT 1"));
        if (p != null) return p.getId();
        // Create placeholder
        ErpProduct placeholder = new ErpProduct();
        placeholder.setProductCode(productCode);
        placeholder.setProductName(productCode);
        placeholder.setStatus("PLACEHOLDER");
        placeholder.setDelFlag("0");
        placeholder.setVerNo(0);
        productDao.insert(placeholder);
        return placeholder.getId();
    }

    private String resolveModule(String productId, String moduleCode) {
        if (moduleCode == null || moduleCode.isEmpty() || productId == null) return null;
        ErpModule m = moduleDao.selectOne(new LambdaQueryWrapper<ErpModule>()
                .eq(ErpModule::getProductId, productId)
                .eq(ErpModule::getModuleCode, moduleCode)
                .eq(ErpModule::getDelFlag, "0").last("LIMIT 1"));
        if (m != null) return m.getId();
        // Create placeholder module
        ErpModule placeholder = new ErpModule();
        placeholder.setProductId(productId);
        placeholder.setModuleCode(moduleCode);
        placeholder.setModuleName(moduleCode);
        placeholder.setDelFlag("0");
        placeholder.setVerNo(0);
        moduleDao.insert(placeholder);
        return placeholder.getId();
    }

    private String resolveUser(String userName) {
        if (userName == null || userName.isEmpty()) return null;
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getRealName, userName).eq(SysUser::getDelFlag, "0").last("LIMIT 1");
        SysUser user = sysUserDao.selectOne(wrapper);
        return user != null ? user.getId() : null;
    }

    public void exportWbs(String projectId, javax.servlet.http.HttpServletResponse response) throws java.io.IOException {
        List<WhPmWbsElement> allElements = wbsDao.selectByProjectId(projectId);
        fillDisplayFields(allElements);
        List<WhPmWbsElement> tree = buildTree(allElements);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=wbs_export.xlsx");

        // Simple CSV fallback (no POI dependency)
        // For full Excel export, would need Apache POI
        java.io.PrintWriter writer = response.getWriter();
        writer.println("序号,名称,父节点,产品,模块,优先级,技术难度,计划责任人,估算工时,估算成本,状态,描述");
        exportNode(writer, tree, 0, null);
        writer.flush();
    }

    private void exportNode(java.io.PrintWriter writer, List<WhPmWbsElement> nodes, int depth, String parentName) {
        for (WhPmWbsElement e : nodes) {
            String indent = "  ".repeat(depth) + e.getName();
            writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    e.getWbsCode(), indent, parentName != null ? parentName : "",
                    e.getProductName() != null ? e.getProductName() : "",
                    e.getModuleName() != null ? e.getModuleName() : "",
                    e.getPriority() != null ? e.getPriority() : "",
                    e.getTechDifficulty() != null ? e.getTechDifficulty() : "",
                    e.getPlannedOwnerName() != null ? e.getPlannedOwnerName() : "",
                    e.getEffortEstimate() != null ? e.getEffortEstimate() : "",
                    e.getBudgetEstimate() != null ? e.getBudgetEstimate() : "",
                    e.getStatus(),
                    e.getDescription() != null ? e.getDescription() : "");
            if (e.getChildren() != null && !e.getChildren().isEmpty()) {
                exportNode(writer, e.getChildren(), depth + 1, e.getName());
            }
        }
    }

    public void downloadTemplate(javax.servlet.http.HttpServletResponse response) throws java.io.IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=wbs_import_template.csv");
        java.io.PrintWriter writer = response.getWriter();
        writer.println("序号,名称,父节点名称,产品类型,产品编码,模块编码,优先级,技术难度,计划责任人,估算工时(小时),估算成本(元),描述");
        writer.println("1,需求分析,,功能需求,PROD-PM,REQ,HIGH,MEDIUM,张三,40,10000,需求分析阶段");
        writer.println("2,概要设计,需求分析,,PROD-PM,DES,HIGH,HIGH,李四,80,20000,概要设计阶段");
        writer.println("3,父节点不存在的子节点,,功能需求,PROD-PM,TASK,LOW,LOW,王五,20,5000,将降级为根节点");
        writer.flush();
    }

    /**
     * Helper class for CSV row parsing
     */
    private static class CsvRow {
        int rowNum;
        String name;
        String parentName;
        String productCode;
        String moduleCode;
        String priority;
        String techDifficulty;
        String plannedOwnerName;
        String effortEstimate;
        String budgetEstimate;
        String description;
    }
}
