package com.wh.entity.pm;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wh.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_actual_cost")
public class WhPmActualCost extends BaseEntity {

    @TableField("PROJECT_ID")
    private String projectId;

    @TableField("ACTIVITY_ID")
    private String activityId;

    @TableField("BUDGET_ITEM_ID")
    private String budgetItemId;

    @TableField("COST_DATE")
    private String costDate;

    @TableField("COST_TYPE")
    private String costType;

    @TableField("AMOUNT")
    private String amount;

    @TableField("DESCRIPTION")
    private String description;

    @TableField("SOURCE_SYSTEM")
    private String sourceSystem;

    @TableField("SOURCE_REF")
    private String sourceRef;

    @TableField("SOURCE_ID")
    private String sourceId;
}
