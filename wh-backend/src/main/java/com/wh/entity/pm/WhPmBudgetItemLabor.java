package com.wh.entity.pm;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wh.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_budget_item_labor")
public class WhPmBudgetItemLabor extends BaseEntity {

    @TableField("BUDGET_ITEM_ID")
    private String budgetItemId;

    @TableField("ROLE_CODE")
    private String roleCode;

    @TableField("HOURS")
    private String hours;

    @TableField("COST_RATE")
    private String costRate;

    @TableField("AMOUNT")
    private String amount;
}
