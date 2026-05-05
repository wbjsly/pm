package com.wh.entity.pm;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wh.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_budget_item")
public class WhPmBudgetItem extends BaseEntity {

    @TableField("BUDGET_ID")
    private String budgetId;

    @TableField("CATEGORY")
    private String category;

    @TableField("PARENT_ID")
    private String parentId;

    @TableField("AMOUNT")
    private String amount;

    @TableField("LEVEL")
    private Integer level;

    @TableField("SORT_ORDER")
    private Integer sortOrder;

    @TableField(exist = false)
    private List<WhPmBudgetItem> children;

    @TableField(exist = false)
    private WhPmBudgetItemLabor laborDetail;

    @TableField(exist = false)
    private WhPmBudgetItemProcurement procurementDetail;

    @TableField(exist = false)
    private WhPmBudgetItemOther otherDetail;

    @TableField(exist = false)
    private BigDecimal actualAmount;

    @TableField(exist = false)
    private BigDecimal ratio;
}
