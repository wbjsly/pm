package com.wh.bo.pm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wh.dao.pm.WhPmActualCostDao;
import com.wh.dao.pm.WhPmBudgetDao;
import com.wh.dao.pm.WhPmBudgetItemDao;
import com.wh.dao.pm.WhPmBudgetItemLaborDao;
import com.wh.dao.pm.WhPmBudgetItemOtherDao;
import com.wh.dao.pm.WhPmBudgetItemProcurementDao;
import com.wh.dao.system.SysPositionDao;
import com.wh.entity.pm.WhPmBudget;
import com.wh.entity.pm.WhPmBudgetItem;
import com.wh.entity.pm.WhPmBudgetItemLabor;
import com.wh.entity.pm.WhPmBudgetItemOther;
import com.wh.entity.pm.WhPmBudgetItemProcurement;
import com.wh.entity.system.SysPosition;
import com.wh.vo.pm.BudgetItemVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 预算项业务逻辑：预算项的创建、删除、金额汇总与 VO 构建。
 * 从 WhPmBudgetBo 抽取，使预算主流程与预算项细节分离。
 */
@Service
public class WhPmBudgetItemBo {

    private final WhPmBudgetDao budgetDao;
    private final WhPmBudgetItemDao budgetItemDao;
    private final WhPmBudgetItemLaborDao budgetItemLaborDao;
    private final WhPmBudgetItemProcurementDao budgetItemProcurementDao;
    private final WhPmBudgetItemOtherDao budgetItemOtherDao;
    private final WhPmActualCostDao actualCostDao;
    private final SysPositionDao sysPositionDao;

    public WhPmBudgetItemBo(WhPmBudgetDao budgetDao, WhPmBudgetItemDao budgetItemDao,
                            WhPmBudgetItemLaborDao budgetItemLaborDao,
                            WhPmBudgetItemProcurementDao budgetItemProcurementDao,
                            WhPmBudgetItemOtherDao budgetItemOtherDao,
                            WhPmActualCostDao actualCostDao,
                            SysPositionDao sysPositionDao) {
        this.budgetDao = budgetDao;
        this.budgetItemDao = budgetItemDao;
        this.budgetItemLaborDao = budgetItemLaborDao;
        this.budgetItemProcurementDao = budgetItemProcurementDao;
        this.budgetItemOtherDao = budgetItemOtherDao;
        this.actualCostDao = actualCostDao;
        this.sysPositionDao = sysPositionDao;
    }

    // ==================== 创建与删除 ====================

    public void createItems(String budgetId, List<BudgetItemRequest> items) {
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

    public void deleteAllItems(String budgetId) {
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

    /**
     * 根据一级预算项重算预算的成本基线与总预算并落库。
     */
    public void recalculateTotals(WhPmBudget budget) {
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

    // ==================== VO 构建 ====================

    /**
     * 构建扁平的预算项 VO 列表（预算详情页）。
     */
    public List<BudgetItemVO> buildFlatItemVOs(String budgetId) {
        List<WhPmBudgetItem> items = budgetItemDao.selectAllByBudgetId(budgetId);
        ItemContext ctx = buildItemContext(items);
        List<BudgetItemVO> itemVOs = new ArrayList<>();
        for (WhPmBudgetItem item : items) {
            itemVOs.add(buildSingleItemVO(item, ctx));
        }
        return itemVOs;
    }

    /**
     * 构建树形的预算项 VO 列表（版本对比页）。
     */
    public List<BudgetItemVO> buildTreeItemVOs(String budgetId) {
        List<WhPmBudgetItem> items = budgetItemDao.selectAllByBudgetId(budgetId);
        ItemContext ctx = buildItemContext(items);

        Map<String, List<WhPmBudgetItem>> childrenMap = items.stream()
                .filter(i -> i.getParentId() != null)
                .collect(Collectors.groupingBy(WhPmBudgetItem::getParentId));

        List<WhPmBudgetItem> rootItems = items.stream()
                .filter(i -> i.getParentId() == null)
                .sorted(Comparator.comparing(WhPmBudgetItem::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        return buildItemVOs(rootItems, childrenMap, ctx);
    }

    private List<BudgetItemVO> buildItemVOs(List<WhPmBudgetItem> items,
                                            Map<String, List<WhPmBudgetItem>> childrenMap,
                                            ItemContext ctx) {
        List<BudgetItemVO> voList = new ArrayList<>();
        for (WhPmBudgetItem item : items) {
            BudgetItemVO vo = buildSingleItemVO(item, ctx);
            List<WhPmBudgetItem> children = childrenMap.get(item.getId());
            if (children != null && !children.isEmpty()) {
                vo.setChildren(buildItemVOs(children, childrenMap, ctx));
            }
            voList.add(vo);
        }
        return voList;
    }

    private BudgetItemVO buildSingleItemVO(WhPmBudgetItem item, ItemContext ctx) {
        BudgetItemVO vo = new BudgetItemVO();
        vo.setId(item.getId());
        vo.setCategory(item.getCategory());
        vo.setLevel(item.getLevel());
        vo.setBudgetAmount(new BigDecimal(item.getAmount()));

        // Get actual amount
        double actual = ctx.actualAmountMap.getOrDefault(item.getId(), 0.0);
        vo.setActualAmount(BigDecimal.valueOf(actual));

        // Calculate ratio
        if (vo.getBudgetAmount().compareTo(BigDecimal.ZERO) > 0) {
            vo.setRatio(vo.getActualAmount().divide(vo.getBudgetAmount(), 4, RoundingMode.HALF_UP));
        } else {
            vo.setRatio(BigDecimal.ZERO);
        }

        // Fill detail
        if ("LABOR".equals(item.getCategory())) {
            WhPmBudgetItemLabor labor = ctx.laborMap.get(item.getId());
            if (labor != null) {
                vo.setRoleCode(labor.getRoleCode());
                vo.setPositionId(labor.getPositionId());
                vo.setHours(labor.getHours());
                vo.setCostRate(new BigDecimal(labor.getCostRate()));
                if (labor.getPositionId() != null) {
                    vo.setPositionName(ctx.positionNameMap.get(labor.getPositionId()));
                }
            }
        } else if ("PROCUREMENT".equals(item.getCategory())) {
            WhPmBudgetItemProcurement procurement = ctx.procurementMap.get(item.getId());
            if (procurement != null) {
                vo.setBomItem(procurement.getBomItem());
                vo.setQty(new BigDecimal(procurement.getQty()));
                vo.setUnitPrice(new BigDecimal(procurement.getUnitPrice()));
            }
        }

        return vo;
    }

    /**
     * 批量加载预算项的关联数据（人力/采购/其他明细、实际成本、岗位名称），避免逐项 N+1 查询。
     */
    private ItemContext buildItemContext(List<WhPmBudgetItem> items) {
        ItemContext ctx = new ItemContext();
        if (items.isEmpty()) {
            return ctx;
        }
        Set<String> itemIds = items.stream().map(WhPmBudgetItem::getId).collect(Collectors.toSet());

        ctx.laborMap = firstPerItem(
                budgetItemLaborDao.selectList(new LambdaQueryWrapper<WhPmBudgetItemLabor>()
                        .in(WhPmBudgetItemLabor::getBudgetItemId, itemIds)
                        .eq(WhPmBudgetItemLabor::getDelFlag, "0")),
                WhPmBudgetItemLabor::getBudgetItemId);
        ctx.procurementMap = firstPerItem(
                budgetItemProcurementDao.selectList(new LambdaQueryWrapper<WhPmBudgetItemProcurement>()
                        .in(WhPmBudgetItemProcurement::getBudgetItemId, itemIds)
                        .eq(WhPmBudgetItemProcurement::getDelFlag, "0")),
                WhPmBudgetItemProcurement::getBudgetItemId);
        ctx.otherMap = firstPerItem(
                budgetItemOtherDao.selectList(new LambdaQueryWrapper<WhPmBudgetItemOther>()
                        .in(WhPmBudgetItemOther::getBudgetItemId, itemIds)
                        .eq(WhPmBudgetItemOther::getDelFlag, "0")),
                WhPmBudgetItemOther::getBudgetItemId);

        // 批量汇总各预算项实际成本
        for (Map<String, Object> row : actualCostDao.sumAmountByBudgetItemIds(itemIds)) {
            Object bid = row.get("budgetItemId");
            Object total = row.get("total");
            if (bid != null && total != null) {
                ctx.actualAmountMap.put(bid.toString(), ((Number) total).doubleValue());
            }
        }

        // 批量加载人力明细中的岗位名称
        Set<String> positionIds = ctx.laborMap.values().stream()
                .map(WhPmBudgetItemLabor::getPositionId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (!positionIds.isEmpty()) {
            for (SysPosition pos : sysPositionDao.selectBatchIds(positionIds)) {
                ctx.positionNameMap.put(pos.getId(), pos.getName());
            }
        }
        return ctx;
    }

    /**
     * 按预算项 ID 分组，每项保留首条（与原 selectByBudgetItemId(...).get(0) 语义一致）。
     */
    private <T> Map<String, T> firstPerItem(List<T> rows, java.util.function.Function<T, String> itemIdGetter) {
        Map<String, T> map = new HashMap<>();
        for (T row : rows) {
            map.putIfAbsent(itemIdGetter.apply(row), row);
        }
        return map;
    }

    private boolean isOtherCategory(String category) {
        return "TRAVEL".equals(category) || "BUSINESS".equals(category)
                || "ENTERTAINMENT".equals(category) || "ACTIVITY".equals(category)
                || "OTHER".equals(category);
    }

    /**
     * 预算项 VO 构建所需的批量预加载数据。
     */
    private static class ItemContext {
        Map<String, WhPmBudgetItemLabor> laborMap = new HashMap<>();
        Map<String, WhPmBudgetItemProcurement> procurementMap = new HashMap<>();
        Map<String, WhPmBudgetItemOther> otherMap = new HashMap<>();
        Map<String, Double> actualAmountMap = new HashMap<>();
        Map<String, String> positionNameMap = new HashMap<>();
    }
}
