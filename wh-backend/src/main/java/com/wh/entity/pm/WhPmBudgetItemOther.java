package com.wh.entity.pm;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wh.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_budget_item_other")
public class WhPmBudgetItemOther extends BaseEntity {

    @TableField("BUDGET_ITEM_ID")
    private String budgetItemId;

    @TableField("CATEGORY")
    private String category;

    @TableField("DESCRIPTION")
    private String description;

    @TableField("AMOUNT")
    private String amount;
}
