package com.wh.entity.pm;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wh.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wh_pm_project_charter")
public class WhPmCharter extends BaseEntity {

    @TableField("CHARTER_CODE")
    private String charterCode;

    @TableField("PROJECT_NAME")
    private String projectName;

    @TableField("PROJECT_CODE")
    private String projectCode;

    @TableField("PROJECT_SHORT_NAME")
    private String projectShortName;

    @TableField("DESCRIPTION")
    private String description;

    @TableField("OBJECTIVES")
    private String objectives;

    @TableField("SCOPE_SUMMARY")
    private String scopeSummary;

    @TableField("SPONSOR_ID")
    private String sponsorId;

    @TableField("PM_ID")
    private String pmId;

    @TableField("BUDGET_CAP")
    private String budgetCap;

    @TableField("START_DATE")
    private String startDate;

    @TableField("END_DATE")
    private String endDate;

    @TableField("KEY_STAKEHOLDERS")
    private String keyStakeholders;

    @TableField("STATUS")
    private String status;

    @TableField("APPROVAL_COMMENT")
    private String approvalComment;

    @TableField("PROCESS_INSTANCE_ID")
    private String processInstanceId;

    @TableField("CONTRACT_NO")
    private String contractNo;

    @TableField("PROJECT_CATEGORY")
    private String projectCategory;

    @TableField("OUTPUT_VALUE_TAXABLE")
    private String outputValueTaxable;

    @TableField("OUTPUT_VALUE_EXCLUDING_TAX")
    private String outputValueExcludingTax;

    @TableField("TAX_RATE")
    private String taxRate;

    @TableField("TAX_AMOUNT")
    private String taxAmount;

    // 非DB字段：用于展示
    @TableField(exist = false)
    private String sponsorName;

    @TableField(exist = false)
    private String pmName;

    // 非DB字段：WBS汇总统计
    @TableField(exist = false)
    private Double wbsTotalEffort;

    @TableField(exist = false)
    private String wbsLatestEndDate;
}
