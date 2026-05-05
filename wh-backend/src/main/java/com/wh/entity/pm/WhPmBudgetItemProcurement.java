package com.wh.entity.pm;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wh.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_budget_item_procurement")
public class WhPmBudgetItemProcurement extends BaseEntity {

    @TableField("BUDGET_ITEM_ID")
    private String budgetItemId;

    @TableField("BOM_ITEM")
    private String bomItem;

    @TableField("QTY")
    private String qty;

    @TableField("UNIT_PRICE")
    private String unitPrice;

    @TableField("AMOUNT")
    private String amount;
}
