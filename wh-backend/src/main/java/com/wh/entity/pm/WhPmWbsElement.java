package com.wh.entity.pm;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wh.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_wbs_element")
public class WhPmWbsElement extends BaseEntity {

    @TableField("PROJECT_ID")
    private String projectId;

    @TableField("WBS_CODE")
    private String wbsCode;

    @TableField("PARENT_ID")
    private String parentId;

    @TableField("LEVEL")
    private Integer level;

    @TableField("NAME")
    private String name;

    @TableField("DESCRIPTION")
    private String description;

    @TableField("ELEMENT_TYPE")
    private String elementType;

    @TableField("EFFORT_ESTIMATE")
    private String effortEstimate;

    @TableField("BUDGET_ESTIMATE")
    private String budgetEstimate;

    @TableField("OWNER_ID")
    private String ownerId;

    @TableField("STATUS")
    private String status;

    @TableField("SORT_ORDER")
    private Integer sortOrder;

    // Extended fields from 023 migration
    @TableField("PRODUCT_ID")
    private String productId;

    @TableField("MODULE_ID")
    private String moduleId;

    @TableField("PRIORITY")
    private String priority;

    @TableField("TECH_DIFFICULTY")
    private String techDifficulty;

    @TableField("PLANNED_OWNER_ID")
    private String plannedOwnerId;

    @TableField("LATEST_PLANNED_END_DATE")
    private String latestPlannedEndDate;

    @TableField("ACTUAL_START_DATE")
    private String actualStartDate;

    @TableField("ACTUAL_END_DATE")
    private String actualEndDate;

    @TableField("ACTUAL_COMPLETED_BY")
    private String actualCompletedBy;

    // Non-DB fields for display
    @TableField(exist = false)
    private String productName;

    @TableField(exist = false)
    private String moduleName;

    @TableField(exist = false)
    private String plannedOwnerName;

    @TableField(exist = false)
    private String ownerName;

    @TableField(exist = false)
    private WhPmWbsVersion latestVersion;

    @TableField(exist = false)
    private java.util.List<WhPmWbsElement> children;
}
