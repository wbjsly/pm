package com.wh.entity.pm;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wh.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_budget")
public class WhPmBudget extends BaseEntity {

    @TableField("PROJECT_ID")
    private String projectId;

    @TableField("BUDGET_CODE")
    private String budgetCode;

    @TableField("VERSION")
    private String version;

    @TableField("COST_BASELINE")
    private String costBaseline;

    @TableField("MANAGEMENT_RESERVE")
    private String managementReserve;

    @TableField("TOTAL_BUDGET")
    private String totalBudget;

    @TableField("TIME_PHASED_DATA")
    private String timePhasedData;

    @TableField("STATUS")
    private String status;

    @TableField("APPROVAL_COMMENT")
    private String approvalComment;

    @TableField("PROCESS_INSTANCE_ID")
    private String processInstanceId;

    @TableField(exist = false)
    private String createByName;

    @TableField(exist = false)
    private String projectName;

    @TableField(exist = false)
    private String pmName;

    @TableField(exist = false)
    private String projectShortName;

    @TableField(exist = false)
    private Double actualCost;

    @TableField(exist = false)
    private Double budgetRemaining;

    @TableField(exist = false)
    private Double costRatio;

    @TableField(exist = false)
    private String laborAmount;

    @TableField(exist = false)
    private String procurementAmount;

    @TableField(exist = false)
    private String otherAmount;
}
