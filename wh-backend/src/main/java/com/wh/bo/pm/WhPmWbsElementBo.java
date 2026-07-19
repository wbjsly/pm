package com.wh.bo.pm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wh.common.ServiceException;
import com.wh.bo.system.SysUserBo;
import com.wh.dao.pm.*;
import com.wh.entity.pm.*;
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
    private final SysUserBo sysUserBo;
    private final SequenceService sequenceService;

    public WhPmWbsElementBo(WhPmWbsElementDao wbsDao, WhPmWbsVersionDao versionDao,
                             ErpProductDao productDao, ErpModuleDao moduleDao,
                             SysUserBo sysUserBo, SequenceService sequenceService) {
        this.wbsDao = wbsDao;
        this.versionDao = versionDao;
        this.productDao = productDao;
        this.moduleDao = moduleDao;
        this.sysUserBo = sysUserBo;
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

    /**
     * 查询项目全部节点、填充展示字段并构建树（供导出等场景复用）。
     */
    List<WhPmWbsElement> buildDisplayTree(String projectId) {
        List<WhPmWbsElement> allElements = wbsDao.selectByProjectId(projectId);
        fillDisplayFields(allElements);
        return buildTree(allElements);
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

        // 批量加载产品、模块、用户名称，避免逐条 N+1 查询
        // 注意：使用 HashMap 以容忍 null key 查询（外键字段可能为 null）与 null value
        Map<String, String> productNameMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            for (ErpProduct p : productDao.selectBatchIds(productIds)) {
                productNameMap.put(p.getId(), p.getProductName());
            }
        }
        Map<String, String> moduleNameMap = new HashMap<>();
        if (!moduleIds.isEmpty()) {
            for (ErpModule m : moduleDao.selectBatchIds(moduleIds)) {
                moduleNameMap.put(m.getId(), m.getModuleName());
            }
        }
        Map<String, String> userNameMap = sysUserBo.getRealNameMap(userIds);
        // 批量加载各 WBS 最新版本（结果按 VERSION_NUMBER DESC 排序，每个 wbsId 首条即最新）
        Map<String, WhPmWbsVersion> latestVersionMap = new HashMap<>();
        if (!wbsIds.isEmpty()) {
            for (WhPmWbsVersion v : versionDao.selectByWbsIds(wbsIds)) {
                latestVersionMap.putIfAbsent(v.getWbsId(), v);
            }
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

    void createVersion(String wbsId, BigDecimal versionNumber, String plannedStart,
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

    String generateWbsCode(String projectId, String parentId) {
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

}
