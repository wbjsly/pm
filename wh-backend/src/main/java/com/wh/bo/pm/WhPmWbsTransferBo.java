package com.wh.bo.pm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wh.dao.pm.ErpModuleDao;
import com.wh.dao.pm.ErpProductDao;
import com.wh.dao.pm.WhPmWbsElementDao;
import com.wh.dao.system.SysUserDao;
import com.wh.entity.pm.ErpModule;
import com.wh.entity.pm.ErpProduct;
import com.wh.entity.pm.WhPmWbsElement;
import com.wh.entity.system.SysUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * WBS 导入/导出业务逻辑（CSV 模板下载、导入解析、导出）。
 * 从 WhPmWbsElementBo 抽取，节点创建相关能力（编码生成、初始版本）复用主 BO。
 */
@Service
public class WhPmWbsTransferBo {

    private final WhPmWbsElementDao wbsDao;
    private final ErpProductDao productDao;
    private final ErpModuleDao moduleDao;
    private final SysUserDao sysUserDao;
    private final WhPmWbsElementBo wbsElementBo;

    public WhPmWbsTransferBo(WhPmWbsElementDao wbsDao,
                             ErpProductDao productDao,
                             ErpModuleDao moduleDao,
                             SysUserDao sysUserDao,
                             WhPmWbsElementBo wbsElementBo) {
        this.wbsDao = wbsDao;
        this.productDao = productDao;
        this.moduleDao = moduleDao;
        this.sysUserDao = sysUserDao;
        this.wbsElementBo = wbsElementBo;
    }

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
        // Build name→id from existing nodes
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

                    String wbsCode = wbsElementBo.generateWbsCode(projectId, null);
                    element.setWbsCode(wbsCode);

                    wbsDao.insert(element);
                    wbsElementBo.createVersion(element.getId(), new BigDecimal("0.1"), null, null, null, null);

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
        // Note: total is already counted in Pass 1 for all rows, no recount needed here
        for (CsvRow row : childRows) {
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

                String wbsCode = wbsElementBo.generateWbsCode(projectId, parentId);
                element.setWbsCode(wbsCode);

                wbsDao.insert(element);
                wbsElementBo.createVersion(element.getId(), new BigDecimal("0.1"), null, null, null, null);

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

    public void exportWbs(String projectId, javax.servlet.http.HttpServletResponse response) throws java.io.IOException {
        List<WhPmWbsElement> tree = wbsElementBo.buildDisplayTree(projectId);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
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
        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=wbs_import_template.csv");
        java.io.PrintWriter writer = response.getWriter();
        writer.println("序号,名称,父节点名称,产品类型,产品编码,模块编码,优先级,技术难度,计划责任人,估算工时(小时),估算成本(元),描述");
        writer.println("1,需求分析,,功能需求,PROD-PM,REQ,HIGH,MEDIUM,张三,40,10000,需求分析阶段");
        writer.println("2,概要设计,需求分析,,PROD-PM,DES,HIGH,HIGH,李四,80,20000,概要设计阶段");
        writer.println("3,父节点不存在的子节点,,功能需求,PROD-PM,TASK,LOW,LOW,王五,20,5000,将降级为根节点");
        writer.flush();
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
