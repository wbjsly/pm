package com.wh.approval.pm;

import com.wh.approval.ApprovalCompletedCallback;
import com.wh.dao.pm.WhPmBudgetDao;
import com.wh.entity.pm.WhPmBudget;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class BudgetApprovalCallback implements ApprovalCompletedCallback {

    private final WhPmBudgetDao budgetDao;

    public BudgetApprovalCallback(WhPmBudgetDao budgetDao) {
        this.budgetDao = budgetDao;
    }

    @Override
    public String getFlowCode() {
        return "PM_BUDGET_APPROVAL";
    }

    @Override
    public void onApproved(String bizId, Map<String, Object> params) {
        WhPmBudget budget = budgetDao.selectById(bizId);
        if (budget != null) {
            // x.7 -> (x+1).0
            String currentVersion = budget.getVersion();
            String approvedVersion = toApprovedVersion(currentVersion);
            budget.setVersion(approvedVersion);
            budget.setStatus("APPROVED");
            budget.setApprovalComment((String) params.get("comment"));
            budgetDao.updateById(budget);
            log.info("Budget {} approved, version: {}", bizId, approvedVersion);
        }
    }

    @Override
    public void onRejected(String bizId, String rejectReason) {
        WhPmBudget budget = budgetDao.selectById(bizId);
        if (budget != null) {
            // x.7 -> x.1
            String draftVersion = toDraftVersion(budget.getVersion());
            budget.setVersion(draftVersion);
            budget.setStatus("DRAFT");
            budget.setApprovalComment(rejectReason);
            budget.setProcessInstanceId(null);
            budgetDao.updateById(budget);
            log.info("Budget {} rejected: {}", bizId, rejectReason);
        }
    }

    private String toApprovedVersion(String currentVersion) {
        // x.7 -> (x+1).0
        String[] parts = currentVersion.substring(1).split("\\.");
        int major = Integer.parseInt(parts[0]);
        return "v" + (major + 1) + ".0";
    }

    private String toDraftVersion(String currentVersion) {
        // x.7 -> x.5
        String[] parts = currentVersion.substring(1).split("\\.");
        return "v" + parts[0] + ".5";
    }
}
