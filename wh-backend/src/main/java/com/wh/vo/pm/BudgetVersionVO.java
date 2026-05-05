package com.wh.vo.pm;

import com.wh.entity.pm.WhPmBudget;
import lombok.Data;

@Data
public class BudgetVersionVO {

    private String id;

    private String budgetCode;

    private String version;

    private String status;

    private String totalBudget;

    private String createDate;

    private String createBy;

    public static BudgetVersionVO from(WhPmBudget budget) {
        BudgetVersionVO vo = new BudgetVersionVO();
        vo.setId(budget.getId());
        vo.setBudgetCode(budget.getBudgetCode());
        vo.setVersion(budget.getVersion());
        vo.setStatus(budget.getStatus());
        vo.setTotalBudget(budget.getTotalBudget());
        vo.setCreateDate(budget.getCreateDate() != null ? budget.getCreateDate().toString() : null);
        vo.setCreateBy(budget.getCreateBy());
        return vo;
    }
}
