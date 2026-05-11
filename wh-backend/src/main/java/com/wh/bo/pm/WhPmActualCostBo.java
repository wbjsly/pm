package com.wh.bo.pm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wh.common.ServiceException;
import com.wh.dao.pm.WhPmActualCostDao;
import com.wh.dao.pm.WhPmBudgetItemDao;
import com.wh.entity.pm.WhPmActualCost;
import com.wh.entity.pm.WhPmBudgetItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class WhPmActualCostBo {

    private final WhPmActualCostDao actualCostDao;
    private final WhPmBudgetItemDao budgetItemDao;

    public WhPmActualCostBo(WhPmActualCostDao actualCostDao, WhPmBudgetItemDao budgetItemDao) {
        this.actualCostDao = actualCostDao;
        this.budgetItemDao = budgetItemDao;
    }

    public IPage<WhPmActualCost> pageList(int pageNum, int pageSize, String projectId,
                                          String budgetItemId, String sourceSystem,
                                          String costTypes, String yearMonth) {
        Page<WhPmActualCost> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WhPmActualCost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WhPmActualCost::getDelFlag, "0");
        if (projectId != null && !projectId.isEmpty()) {
            wrapper.eq(WhPmActualCost::getProjectId, projectId);
        }
        if (budgetItemId != null && !budgetItemId.isEmpty()) {
            wrapper.eq(WhPmActualCost::getBudgetItemId, budgetItemId);
        }
        if (sourceSystem != null && !sourceSystem.isEmpty()) {
            wrapper.eq(WhPmActualCost::getSourceSystem, sourceSystem);
        }
        if (costTypes != null && !costTypes.isEmpty()) {
            String[] types = costTypes.split(",");
            wrapper.in(WhPmActualCost::getCostType, Arrays.asList(types));
        }
        if (yearMonth != null && !yearMonth.isEmpty()) {
            String[] months = yearMonth.split(",");
            if (months.length == 1) {
                wrapper.likeRight(WhPmActualCost::getCostDate, months[0]);
            } else {
                wrapper.and(w -> {
                    w.likeRight(WhPmActualCost::getCostDate, months[0]);
                    for (int i = 1; i < months.length; i++) {
                        w.or().likeRight(WhPmActualCost::getCostDate, months[i]);
                    }
                });
            }
        }
        wrapper.orderByDesc(WhPmActualCost::getCostDate);
        return actualCostDao.selectPage(page, wrapper);
    }

    public List<Map<String, Object>> getMonthlyAggregation(String projectId) {
        return actualCostDao.aggregateMonthlyByProject(projectId);
    }

    public Double getSumByFilter(String projectId, String yearMonth, String costTypes) {
        String[] types = (costTypes != null && !costTypes.isEmpty()) ? costTypes.split(",") : null;
        String[] months = (yearMonth != null && !yearMonth.isEmpty()) ? yearMonth.split(",") : null;
        return actualCostDao.sumAmountByFilter(projectId, months, types);
    }

    public WhPmActualCost getById(String id) {
        WhPmActualCost cost = actualCostDao.selectById(id);
        if (cost == null || "1".equals(cost.getDelFlag())) {
            throw new ServiceException(404, "实际成本记录不存在");
        }
        return cost;
    }

    @Transactional
    public WhPmActualCost create(ActualCostCreateRequest req) {
        // Validate budget item exists
        if (req.getBudgetItemId() != null && !req.getBudgetItemId().isEmpty()) {
            WhPmBudgetItem item = budgetItemDao.selectById(req.getBudgetItemId());
            if (item == null || "1".equals(item.getDelFlag())) {
                throw new ServiceException("预算科目不存在");
            }
            // Validate cost type matches category
            if (req.getCostType() != null && !req.getCostType().equals(item.getCategory())) {
                throw new ServiceException("成本类型与预算科目不匹配");
            }
        }

        WhPmActualCost cost = new WhPmActualCost();
        cost.setProjectId(req.getProjectId());
        cost.setBudgetItemId(req.getBudgetItemId());
        cost.setActivityId(req.getActivityId());
        cost.setCostDate(req.getCostDate());
        cost.setCostType(req.getCostType());
        cost.setAmount(req.getAmount());
        cost.setDescription(req.getDescription());
        cost.setSourceSystem("manual");
        cost.setSourceRef(req.getSourceRef());
        cost.setSourceId(null);
        actualCostDao.insert(cost);
        return cost;
    }

    @Transactional
    public void delete(String id) {
        WhPmActualCost cost = getById(id);
        if (!"manual".equals(cost.getSourceSystem())) {
            throw new ServiceException("只能删除手动录入的实际成本");
        }
        actualCostDao.deleteById(id);
    }
}
