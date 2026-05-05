package com.wh.bo.pm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wh.common.ServiceException;
import com.wh.dao.pm.WhPmActualCostDao;
import com.wh.dao.pm.WhPmBudgetDao;
import com.wh.dao.pm.WhPmCostWarningDao;
import com.wh.entity.pm.WhPmBudget;
import com.wh.entity.pm.WhPmCostWarning;
import com.wh.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class WhPmCostWarningBo {

    private final WhPmCostWarningDao costWarningDao;
    private final WhPmBudgetDao budgetDao;
    private final WhPmActualCostDao actualCostDao;

    @Value("${cost.warning.threshold.info:0.80}")
    private double thresholdInfo;

    @Value("${cost.warning.threshold.warn:0.95}")
    private double thresholdWarn;

    @Value("${cost.warning.threshold.critical:1.00}")
    private double thresholdCritical;

    public WhPmCostWarningBo(WhPmCostWarningDao costWarningDao,
                             WhPmBudgetDao budgetDao,
                             WhPmActualCostDao actualCostDao) {
        this.costWarningDao = costWarningDao;
        this.budgetDao = budgetDao;
        this.actualCostDao = actualCostDao;
    }

    public List<WhPmCostWarning> listByProjectId(String projectId, String status) {
        LambdaQueryWrapper<WhPmCostWarning> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WhPmCostWarning::getDelFlag, "0");
        if (projectId != null && !projectId.isEmpty()) {
            wrapper.eq(WhPmCostWarning::getProjectId, projectId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(WhPmCostWarning::getStatus, status);
        }
        wrapper.orderByDesc(WhPmCostWarning::getTriggeredAt);
        return costWarningDao.selectList(wrapper);
    }

    public List<WhPmCostWarning> listActiveAll(int limit) {
        return costWarningDao.selectActiveAll(limit);
    }

    @Transactional
    public void closeWarning(String id, String userId) {
        WhPmCostWarning warning = costWarningDao.selectById(id);
        if (warning == null || "1".equals(warning.getDelFlag())) {
            throw new ServiceException(404, "预警不存在");
        }
        if ("CLOSED".equals(warning.getStatus())) {
            throw new ServiceException("预警已关闭");
        }
        warning.setStatus("CLOSED");
        warning.setClosedBy(userId);
        warning.setClosedAt(LocalDateTime.now().toString());
        costWarningDao.updateById(warning);
    }

    @Transactional
    public void triggerCalculation() {
        log.info("Starting cost warning calculation...");

        // Get all APPROVED budgets
        LambdaQueryWrapper<WhPmBudget> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WhPmBudget::getStatus, "APPROVED")
               .eq(WhPmBudget::getDelFlag, "0");
        List<WhPmBudget> budgets = budgetDao.selectList(wrapper);

        for (WhPmBudget budget : budgets) {
            processBudgetWarning(budget);
        }

        log.info("Cost warning calculation completed for {} budgets", budgets.size());
    }

    private void processBudgetWarning(WhPmBudget budget) {
        // Calculate total actual cost for the project
        Double actualSum = actualCostDao.sumAmountByBudgetItemId(budget.getId());
        if (actualSum == null) actualSum = 0.0;

        BigDecimal totalBudget = new BigDecimal(budget.getTotalBudget());
        BigDecimal totalActual = BigDecimal.valueOf(actualSum);
        BigDecimal ratio = totalBudget.compareTo(BigDecimal.ZERO) > 0
                ? totalActual.divide(totalBudget, 4, BigDecimal.ROUND_HALF_UP)
                : BigDecimal.ZERO;

        double ratioValue = ratio.doubleValue();
        String level;
        if (ratioValue >= thresholdCritical) {
            level = "CRITICAL";
        } else if (ratioValue >= thresholdWarn) {
            level = "WARN";
        } else if (ratioValue >= thresholdInfo) {
            level = "INFO";
        } else {
            level = null;
        }

        // Check existing active warnings for this budget
        LambdaQueryWrapper<WhPmCostWarning> existingWrapper = new LambdaQueryWrapper<>();
        existingWrapper.eq(WhPmCostWarning::getBudgetId, budget.getId())
                       .eq(WhPmCostWarning::getStatus, "ACTIVE")
                       .eq(WhPmCostWarning::getDelFlag, "0");
        List<WhPmCostWarning> existingWarnings = costWarningDao.selectList(existingWrapper);

        if (level == null) {
            // Ratio below threshold, close all active warnings
            for (WhPmCostWarning w : existingWarnings) {
                w.setStatus("CLOSED");
                w.setClosedAt(LocalDateTime.now().toString());
                costWarningDao.updateById(w);
            }
        } else {
            // Create or update warning
            boolean found = false;
            for (WhPmCostWarning w : existingWarnings) {
                if (w.getLevel().equals(level)) {
                    w.setRatio(ratio.toPlainString());
                    w.setTriggeredAt(LocalDateTime.now().toString());
                    costWarningDao.updateById(w);
                    found = true;
                } else {
                    // Close lower-level warnings if higher-level triggered
                    w.setStatus("CLOSED");
                    w.setClosedAt(LocalDateTime.now().toString());
                    costWarningDao.updateById(w);
                }
            }
            if (!found) {
                WhPmCostWarning warning = new WhPmCostWarning();
                warning.setProjectId(budget.getProjectId());
                warning.setBudgetId(budget.getId());
                warning.setLevel(level);
                warning.setRatio(ratio.toPlainString());
                warning.setStatus("ACTIVE");
                warning.setTriggeredAt(LocalDateTime.now().toString());
                costWarningDao.insert(warning);
                log.info("Created {} cost warning for budget {} (ratio: {})",
                        level, budget.getBudgetCode(), ratioValue);
            }
        }
    }
}
