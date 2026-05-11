package com.wh.bo.pm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wh.common.ServiceException;
import com.wh.dao.pm.*;
import com.wh.entity.pm.WhPmActualCost;
import com.wh.entity.pm.WhPmBudget;
import com.wh.entity.pm.WhPmBudgetItem;
import com.wh.entity.pm.WhPmBudgetItemLabor;
import com.wh.entity.pm.WhPmBudgetItemOther;
import com.wh.entity.pm.WhPmBudgetItemProcurement;
import com.wh.service.SequenceService;
import com.wh.util.SecurityUtils;
import com.wh.vo.pm.BudgetComparisonVO;
import com.wh.vo.pm.BudgetDetailVO;
import com.wh.vo.pm.BudgetItemVO;
import com.wh.vo.pm.BudgetVersionVO;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WhPmBudgetBo {

    private final WhPmBudgetDao budgetDao;
    private final WhPmBudgetItemDao budgetItemDao;
    private final WhPmBudgetItemLaborDao budgetItemLaborDao;
    private final WhPmBudgetItemProcurementDao budgetItemProcurementDao;
    private final WhPmBudgetItemOtherDao budgetItemOtherDao;
    private final WhPmActualCostDao actualCostDao;
    private final com.wh.dao.pm.WhPmCharterDao charterDao;
    private final com.wh.dao.system.SysUserDao sysUserDao;
    private final com.wh.dao.system.SysPositionDao sysPositionDao;
    private final SequenceService sequenceService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;

    public WhPmBudgetBo(WhPmBudgetDao budgetDao, WhPmBudgetItemDao budgetItemDao,
                        WhPmBudgetItemLaborDao budgetItemLaborDao,
                        WhPmBudgetItemProcurementDao budgetItemProcurementDao,
                        WhPmBudgetItemOtherDao budgetItemOtherDao,
                        WhPmActualCostDao actualCostDao,
                        com.wh.dao.pm.WhPmCharterDao charterDao,
                        com.wh.dao.system.SysUserDao sysUserDao,
                        com.wh.dao.system.SysPositionDao sysPositionDao,
                        SequenceService sequenceService,
                        RuntimeService runtimeService, TaskService taskService) {
        this.budgetDao = budgetDao;
        this.budgetItemDao = budgetItemDao;
        this.budgetItemLaborDao = budgetItemLaborDao;
        this.budgetItemProcurementDao = budgetItemProcurementDao;
        this.budgetItemOtherDao = budgetItemOtherDao;
        this.actualCostDao = actualCostDao;
        this.charterDao = charterDao;
        this.sysUserDao = sysUserDao;
        this.sysPositionDao = sysPositionDao;
        this.sequenceService = sequenceService;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
    }

    public IPage<WhPmBudget> pageList(int pageNum, int pageSize, String projectId, String pmId, String status) {
        Page<WhPmBudget> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WhPmBudget> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WhPmBudget::getDelFlag, "0");
        if (projectId != null && !projectId.isEmpty()) {
            wrapper.eq(WhPmBudget::getProjectId, projectId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(WhPmBudget::getStatus, status);
        }
        wrapper.orderByDesc(WhPmBudget::getCreateDate);
        IPage<WhPmBudget> result = budgetDao.selectPage(page, wrapper);
        fillUserNames(result.getRecords());
        fillActualCosts(result.getRecords());

        // Filter by PM if needed (after loading charters)
        if (pmId != null && !pmId.isEmpty()) {
            result.getRecords().removeIf(b -> {
                if (b.getProjectId() == null) return true;
                var charter = charterDao.selectById(b.getProjectId());
                return charter == null || !pmId.equals(charter.getPmId());
            });
        }
        return result;
    }

    public WhPmBudget getById(String id) {
        WhPmBudget budget = budgetDao.selectById(id);
        if (budget == null || "1".equals(budget.getDelFlag())) {
            throw new ServiceException(404, "预算不存在");
        }
        fillUserNames(List.of(budget));
        return budget;
    }

    public BudgetDetailVO getDetailWithItems(String id) {
        WhPmBudget budget = getById(id);
        BudgetDetailVO vo = new BudgetDetailVO();
        vo.setBudget(budget);

        // Load items
        List<WhPmBudgetItem> items = budgetItemDao.selectAllByBudgetId(id);
        Map<String, WhPmBudgetItemLabor> laborMap = loadLaborDetails(items);
        Map<String, WhPmBudgetItemProcurement> procurementMap = loadProcurementDetails(items);
        Map<String, WhPmBudgetItemOther> otherMap = loadOtherDetails(items);

        List<BudgetItemVO> itemVOs = new ArrayList<>();
        for (WhPmBudgetItem item : items) {
            BudgetItemVO voItem = buildSingleItemVO(item, laborMap, procurementMap, otherMap);
            itemVOs.add(voItem);
        }
        vo.setItems(itemVOs);
        return vo;
    }

    @Transactional
    public WhPmBudget create(BudgetCreateRequest req) {
        // Check if project already has an active budget
        LambdaQueryWrapper<WhPmBudget> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WhPmBudget::getProjectId, req.getProjectId())
               .eq(WhPmBudget::getDelFlag, "0");
        long count = budgetDao.selectCount(wrapper);
        if (count > 0) {
            throw new ServiceException("该项目已编制预算，不能重复编制");
        }

        WhPmBudget budget = new WhPmBudget();
        budget.setBudgetCode(sequenceService.generateCode("PM_BUDGET", "BUDGET-"));
        budget.setProjectId(req.getProjectId());
        budget.setVersion("v0.1");
        budget.setCostBaseline("0");
        budget.setTotalBudget("0");
        budget.setManagementReserve(req.getManagementReserve() != null ? req.getManagementReserve() : "0");
        budget.setStatus("DRAFT");
        budget.setDelFlag("0");
        budget.setVerNo(0);
        budgetDao.insert(budget);

        // Create budget items
        if (req.getItems() != null && !req.getItems().isEmpty()) {
            createBudgetItems(budget.getId(), req.getItems());
        }

        // Recalculate totals
        recalculateTotals(budget);

        return budget;
    }

    @Transactional
    public void update(String id, BudgetUpdateRequest req) {
        WhPmBudget budget = getById(id);
        if (!"DRAFT".equals(budget.getStatus())) {
            throw new ServiceException("只有草稿状态的预算可以修改");
        }

        if (req.getManagementReserve() != null) {
            budget.setManagementReserve(req.getManagementReserve());
        }

        budgetDao.updateById(budget);

        // Replace all items
        deleteAllItems(budget.getId());
        if (req.getItems() != null && !req.getItems().isEmpty()) {
            createBudgetItems(budget.getId(), req.getItems());
        }

        recalculateTotals(budget);
    }

    @Transactional
    public void delete(String id) {
        WhPmBudget budget = getById(id);
        if (!"DRAFT".equals(budget.getStatus())) {
            throw new ServiceException("只有草稿状态的预算可以删除");
        }
        // Soft delete items
        deleteAllItems(id);
        budgetDao.physicalDeleteById(id);
    }

    @Transactional
    public void submit(String id) {
        WhPmBudget budget = getById(id);
        if (!"DRAFT".equals(budget.getStatus())) {
            throw new ServiceException("只有草稿状态的预算可以提交审批");
        }

        // Update version and status
        budget.setVersion(nextDraftVersion(budget.getVersion()));
        budget.setStatus("PENDING");
        budgetDao.updateById(budget);

        // Start Flowable process
        String approverId = getApproverId(budget);

        Map<String, Object> variables = new HashMap<>();
        variables.put("flowCode", "PM_BUDGET_APPROVAL");
        variables.put("bizId", id);
        variables.put("approverId", approverId);

        org.flowable.engine.runtime.ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                "PM_BUDGET_APPROVAL", id, variables);

        budget.setProcessInstanceId(instance.getId());
        budgetDao.updateById(budget);

        log.info("Budget {} submitted for approval, process: {}", id, instance.getId());
    }

    @Transactional
    public void approve(String id, String comment) {
        WhPmBudget budget = getById(id);
        if (!"PENDING".equals(budget.getStatus())) {
            throw new ServiceException("预算不在审批中");
        }

        Task task = taskService.createTaskQuery()
                .processInstanceId(budget.getProcessInstanceId())
                .singleResult();
        if (task == null) {
            throw new ServiceException("未找到审批任务");
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("approvalResult", "APPROVED");
        variables.put("comment", comment);
        taskService.complete(task.getId(), variables);
    }

    @Transactional
    public void reject(String id, String comment) {
        WhPmBudget budget = getById(id);
        if (!"PENDING".equals(budget.getStatus())) {
            throw new ServiceException("预算不在审批中");
        }

        Task task = taskService.createTaskQuery()
                .processInstanceId(budget.getProcessInstanceId())
                .singleResult();
        if (task == null) {
            throw new ServiceException("未找到审批任务");
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("approvalResult", "REJECTED");
        variables.put("rejectReason", comment);
        taskService.complete(task.getId(), variables);
    }

    @Transactional
    public BudgetComparisonVO getComparison(String budgetId, String version) {
        WhPmBudget budget;
        if (version != null && !version.isEmpty()) {
            LambdaQueryWrapper<WhPmBudget> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(WhPmBudget::getId, budgetId)
                   .eq(WhPmBudget::getVersion, version)
                   .eq(WhPmBudget::getDelFlag, "0");
            budget = budgetDao.selectOne(wrapper);
        } else {
            WhPmBudget inputBudget = budgetDao.selectById(budgetId);
            if (inputBudget == null || "1".equals(inputBudget.getDelFlag())) {
                throw new ServiceException(404, "预算不存在");
            }
            budget = getLatestApprovedBudget(inputBudget.getProjectId());
        }

        if (budget == null) {
            throw new ServiceException(404, "预算不存在");
        }

        BudgetComparisonVO vo = new BudgetComparisonVO();
        vo.setBudgetId(budget.getId());
        vo.setVersion(budget.getVersion());
        vo.setProjectId(budget.getProjectId());

        // Get project name
        var charter = charterDao.selectById(budget.getProjectId());
        if (charter != null) {
            vo.setProjectName(charter.getProjectName());
        }

        // Build budget item tree
        List<WhPmBudgetItem> items = budgetItemDao.selectAllByBudgetId(budget.getId());
        Map<String, List<WhPmBudgetItem>> childrenMap = items.stream()
                .filter(i -> i.getParentId() != null)
                .collect(Collectors.groupingBy(WhPmBudgetItem::getParentId));

        List<WhPmBudgetItem> rootItems = items.stream()
                .filter(i -> i.getParentId() == null)
                .sorted(Comparator.comparing(WhPmBudgetItem::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        // Load detail for each item
        Map<String, WhPmBudgetItemLabor> laborMap = loadLaborDetails(items);
        Map<String, WhPmBudgetItemProcurement> procurementMap = loadProcurementDetails(items);
        Map<String, WhPmBudgetItemOther> otherMap = loadOtherDetails(items);

        List<BudgetItemVO> itemVOs = buildItemVOs(rootItems, childrenMap, laborMap, procurementMap, otherMap);
        vo.setItems(itemVOs);

        // Calculate totals
        BigDecimal directBudget = new BigDecimal(budget.getCostBaseline());
        BigDecimal managementReserve = new BigDecimal(budget.getManagementReserve() != null ? budget.getManagementReserve() : "0");
        BigDecimal totalBudget = directBudget.add(managementReserve);
        BigDecimal totalActual = sumActualForItems(itemVOs);
        vo.setDirectBudget(directBudget);
        vo.setTotalBudget(totalBudget);
        vo.setManagementReserve(managementReserve);
        vo.setTotalActual(totalActual);
        vo.setTotalRatio(directBudget.compareTo(BigDecimal.ZERO) > 0
                ? totalActual.divide(directBudget, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO);

        return vo;
    }

    public List<BudgetVersionVO> getVersionHistory(String projectId) {
        LambdaQueryWrapper<WhPmBudget> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WhPmBudget::getProjectId, projectId)
               .eq(WhPmBudget::getDelFlag, "0")
               .orderByDesc(WhPmBudget::getCreateDate);
        List<WhPmBudget> budgets = budgetDao.selectList(wrapper);
        return budgets.stream().map(BudgetVersionVO::from).collect(Collectors.toList());
    }

    private void createBudgetItems(String budgetId, List<BudgetItemRequest> items) {
        for (BudgetItemRequest req : items) {
            createBudgetItemRecursive(budgetId, req, null, 0);
        }
    }

    private void createBudgetItemRecursive(String budgetId, BudgetItemRequest req, String parentId, int sortOrder) {
        WhPmBudgetItem item = new WhPmBudgetItem();
        item.setBudgetId(budgetId);
        item.setCategory(req.getCategory());
        item.setParentId(parentId);
        item.setAmount(req.getAmount() != null ? req.getAmount() : "0");
        item.setLevel(req.getLevel() != null ? req.getLevel() : 1);
        item.setSortOrder(sortOrder);
        budgetItemDao.insert(item);

        // Create sub-table record
        if ("LABOR".equals(req.getCategory()) && req.getRoleCode() != null) {
            WhPmBudgetItemLabor labor = new WhPmBudgetItemLabor();
            labor.setBudgetItemId(item.getId());
            labor.setRoleCode(req.getRoleCode());
            labor.setPositionId(req.getPositionId());
            labor.setHours(req.getHours() != null ? req.getHours() : "0");
            labor.setCostRate(req.getCostRate() != null ? req.getCostRate() : "0");
            labor.setAmount(req.getAmount() != null ? req.getAmount() : "0");
            budgetItemLaborDao.insert(labor);
        } else if ("PROCUREMENT".equals(req.getCategory()) && req.getBomItem() != null) {
            WhPmBudgetItemProcurement procurement = new WhPmBudgetItemProcurement();
            procurement.setBudgetItemId(item.getId());
            procurement.setBomItem(req.getBomItem());
            procurement.setQty(req.getQty() != null ? req.getQty() : "0");
            procurement.setUnitPrice(req.getUnitPrice() != null ? req.getUnitPrice() : "0");
            procurement.setAmount(req.getAmount() != null ? req.getAmount() : "0");
            budgetItemProcurementDao.insert(procurement);
        } else if (isOtherCategory(req.getCategory())) {
            WhPmBudgetItemOther other = new WhPmBudgetItemOther();
            other.setBudgetItemId(item.getId());
            other.setCategory(req.getCategory());
            other.setDescription(req.getDescription());
            other.setAmount(req.getAmount() != null ? req.getAmount() : "0");
            budgetItemOtherDao.insert(other);
        }

        // Create children
        if (req.getChildren() != null && !req.getChildren().isEmpty()) {
            int childOrder = 0;
            for (BudgetItemRequest child : req.getChildren()) {
                createBudgetItemRecursive(budgetId, child, item.getId(), childOrder++);
            }
        }
    }

    private void deleteAllItems(String budgetId) {
        // Get all items
        List<WhPmBudgetItem> items = budgetItemDao.selectAllByBudgetId(budgetId);
        for (WhPmBudgetItem item : items) {
            // Delete sub-table records
            budgetItemLaborDao.delete(new LambdaQueryWrapper<WhPmBudgetItemLabor>()
                    .eq(WhPmBudgetItemLabor::getBudgetItemId, item.getId()));
            budgetItemProcurementDao.delete(new LambdaQueryWrapper<WhPmBudgetItemProcurement>()
                    .eq(WhPmBudgetItemProcurement::getBudgetItemId, item.getId()));
            budgetItemOtherDao.delete(new LambdaQueryWrapper<WhPmBudgetItemOther>()
                    .eq(WhPmBudgetItemOther::getBudgetItemId, item.getId()));
        }
        // Soft delete items
        for (WhPmBudgetItem item : items) {
            budgetItemDao.deleteById(item.getId());
        }
    }

    private void recalculateTotals(WhPmBudget budget) {
        List<WhPmBudgetItem> primaryItems = budgetItemDao.selectPrimaryItems(budget.getId());
        BigDecimal costBaseline = BigDecimal.ZERO;
        for (WhPmBudgetItem item : primaryItems) {
            costBaseline = costBaseline.add(new BigDecimal(item.getAmount()));
        }
        budget.setCostBaseline(costBaseline.toPlainString());
        BigDecimal managementReserve = new BigDecimal(budget.getManagementReserve());
        budget.setTotalBudget(costBaseline.add(managementReserve).toPlainString());
        budgetDao.updateById(budget);
    }

    private WhPmBudget getLatestApprovedBudget(String budgetId) {
        // budgetId is actually the projectId for comparison API
        LambdaQueryWrapper<WhPmBudget> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WhPmBudget::getProjectId, budgetId)
               .eq(WhPmBudget::getStatus, "APPROVED")
               .eq(WhPmBudget::getDelFlag, "0")
               .orderByDesc(WhPmBudget::getCreateDate);
        List<WhPmBudget> budgets = budgetDao.selectList(wrapper);
        if (budgets.isEmpty()) {
            throw new ServiceException("没有已审批通过的预算");
        }
        return budgets.get(0);
    }

    private List<BudgetItemVO> buildItemVOs(List<WhPmBudgetItem> items,
                                            Map<String, List<WhPmBudgetItem>> childrenMap,
                                            Map<String, WhPmBudgetItemLabor> laborMap,
                                            Map<String, WhPmBudgetItemProcurement> procurementMap,
                                            Map<String, WhPmBudgetItemOther> otherMap) {
        List<BudgetItemVO> voList = new ArrayList<>();
        for (WhPmBudgetItem item : items) {
            BudgetItemVO vo = buildSingleItemVO(item, laborMap, procurementMap, otherMap);
            List<WhPmBudgetItem> children = childrenMap.get(item.getId());
            if (children != null && !children.isEmpty()) {
                List<BudgetItemVO> childVOs = buildItemVOs(children, childrenMap, laborMap, procurementMap, otherMap);
                vo.setChildren(childVOs);
            }
            voList.add(vo);
        }
        return voList;
    }

    private BudgetItemVO buildSingleItemVO(WhPmBudgetItem item,
                                           Map<String, WhPmBudgetItemLabor> laborMap,
                                           Map<String, WhPmBudgetItemProcurement> procurementMap,
                                           Map<String, WhPmBudgetItemOther> otherMap) {
        BudgetItemVO vo = new BudgetItemVO();
        vo.setId(item.getId());
        vo.setCategory(item.getCategory());
        vo.setLevel(item.getLevel());
        vo.setBudgetAmount(new BigDecimal(item.getAmount()));

        // Get actual amount
        double actual = actualCostDao.sumAmountByBudgetItemId(item.getId());
        vo.setActualAmount(BigDecimal.valueOf(actual));

        // Calculate ratio
        if (vo.getBudgetAmount().compareTo(BigDecimal.ZERO) > 0) {
            vo.setRatio(vo.getActualAmount().divide(vo.getBudgetAmount(), 4, RoundingMode.HALF_UP));
        } else {
            vo.setRatio(BigDecimal.ZERO);
        }

        // Fill detail
        if ("LABOR".equals(item.getCategory())) {
            WhPmBudgetItemLabor labor = laborMap.get(item.getId());
            if (labor != null) {
                vo.setRoleCode(labor.getRoleCode());
                vo.setPositionId(labor.getPositionId());
                vo.setHours(labor.getHours());
                vo.setCostRate(new BigDecimal(labor.getCostRate()));
                if (labor.getPositionId() != null) {
                    var pos = sysPositionDao.selectById(labor.getPositionId());
                    vo.setPositionName(pos != null ? pos.getName() : null);
                }
            }
        } else if ("PROCUREMENT".equals(item.getCategory())) {
            WhPmBudgetItemProcurement procurement = procurementMap.get(item.getId());
            if (procurement != null) {
                vo.setBomItem(procurement.getBomItem());
                vo.setQty(new BigDecimal(procurement.getQty()));
                vo.setUnitPrice(new BigDecimal(procurement.getUnitPrice()));
            }
        }

        return vo;
    }

    private Map<String, WhPmBudgetItemLabor> loadLaborDetails(List<WhPmBudgetItem> items) {
        Map<String, WhPmBudgetItemLabor> map = new HashMap<>();
        for (WhPmBudgetItem item : items) {
            List<WhPmBudgetItemLabor> labors = budgetItemLaborDao.selectByBudgetItemId(item.getId());
            if (!labors.isEmpty()) {
                map.put(item.getId(), labors.get(0));
            }
        }
        return map;
    }

    private Map<String, WhPmBudgetItemProcurement> loadProcurementDetails(List<WhPmBudgetItem> items) {
        Map<String, WhPmBudgetItemProcurement> map = new HashMap<>();
        for (WhPmBudgetItem item : items) {
            List<WhPmBudgetItemProcurement> procurements = budgetItemProcurementDao.selectByBudgetItemId(item.getId());
            if (!procurements.isEmpty()) {
                map.put(item.getId(), procurements.get(0));
            }
        }
        return map;
    }

    private Map<String, WhPmBudgetItemOther> loadOtherDetails(List<WhPmBudgetItem> items) {
        Map<String, WhPmBudgetItemOther> map = new HashMap<>();
        for (WhPmBudgetItem item : items) {
            List<WhPmBudgetItemOther> others = budgetItemOtherDao.selectByBudgetItemId(item.getId());
            if (!others.isEmpty()) {
                map.put(item.getId(), others.get(0));
            }
        }
        return map;
    }

    private BigDecimal sumActualForItems(List<BudgetItemVO> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (BudgetItemVO item : items) {
            if (item.getChildren() != null && !item.getChildren().isEmpty()) {
                total = total.add(sumActualForItems(item.getChildren()));
            } else {
                total = total.add(item.getActualAmount());
            }
        }
        return total;
    }

    private boolean isOtherCategory(String category) {
        return "TRAVEL".equals(category) || "BUSINESS".equals(category)
                || "ENTERTAINMENT".equals(category) || "ACTIVITY".equals(category)
                || "OTHER".equals(category);
    }

    private String nextDraftVersion(String currentVersion) {
        // x.1 -> x.7
        if (currentVersion.endsWith(".1")) {
            String prefix = currentVersion.substring(0, currentVersion.length() - 2);
            return prefix + ".7";
        }
        return "v0.7";
    }

    private String getApproverId(WhPmBudget budget) {
        // Get sponsor from project charter
        var charter = charterDao.selectById(budget.getProjectId());
        if (charter != null && charter.getSponsorId() != null) {
            return charter.getSponsorId();
        }
        // Fallback to admin
        return "admin";
    }

    private void fillUserNames(List<WhPmBudget> budgets) {
        Map<String, String> userNameMap = new HashMap<>();
        Set<String> projectIds = new HashSet<>();
        for (WhPmBudget b : budgets) {
            collectUserId(b.getCreateBy(), userNameMap);
            if (b.getProjectId() != null && !b.getProjectId().isEmpty()) {
                projectIds.add(b.getProjectId());
            }
        }
        // Load charters for project names and PM names
        Map<String, com.wh.entity.pm.WhPmCharter> charterMap = new HashMap<>();
        for (String pid : projectIds) {
            var charter = charterDao.selectById(pid);
            if (charter != null) charterMap.put(pid, charter);
        }
        // Load PM user names
        Set<String> pmIds = new HashSet<>();
        for (var c : charterMap.values()) {
            if (c.getPmId() != null) pmIds.add(c.getPmId());
        }
        Map<String, String> pmNameMap = new HashMap<>();
        for (String pmId : pmIds) {
            var user = sysUserDao.selectById(pmId);
            if (user != null) pmNameMap.put(pmId, user.getRealName());
        }
        for (WhPmBudget b : budgets) {
            b.setCreateByName(userNameMap.get(b.getCreateBy()));
            var charter = charterMap.get(b.getProjectId());
            if (charter != null) {
                b.setProjectName(charter.getProjectName());
                b.setProjectShortName(charter.getProjectShortName());
                if (charter.getPmId() != null) {
                    b.setPmName(pmNameMap.get(charter.getPmId()));
                }
            }
        }
    }

    private void fillActualCosts(List<WhPmBudget> budgets) {
        for (WhPmBudget b : budgets) {
            if (b.getProjectId() == null) continue;
            Double cost = actualCostDao.sumAmountByProjectId(b.getProjectId());
            double actualCost = cost != null ? cost : 0.0;
            b.setActualCost(actualCost);
            double costBaseline = parseDouble(b.getCostBaseline());
            b.setBudgetRemaining(costBaseline - actualCost);
            if (costBaseline > 0) {
                b.setCostRatio(actualCost / costBaseline);
            } else {
                b.setCostRatio(0.0);
            }
        }
    }

    private double parseDouble(String val) {
        try {
            return val != null ? Double.parseDouble(val) : 0.0;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private void collectUserId(String userId, Map<String, String> map) {
        if (userId != null && !userId.isEmpty() && !map.containsKey(userId)) {
            var user = sysUserDao.selectById(userId);
            if (user != null) {
                map.put(userId, user.getRealName());
            }
        }
    }
}
