package com.wh.entity.pm;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wh.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wh_pm_deliverable")
public class WhPmDeliverable extends BaseEntity {

    @TableField("DELIVERABLE_CODE")
    private String deliverableCode;

    @TableField("NAME")
    private String name;

    @TableField("DESCRIPTION")
    private String description;

    @TableField("PLANNED_DELIVERY_DATE")
    private String plannedDeliveryDate;

    @TableField("ACTUAL_DELIVERY_DATE")
    private String actualDeliveryDate;

    @TableField("STATUS")
    private String status;

    @TableField("ATTACHMENTS")
    private String attachments;

    @TableField("APPROVAL_COMMENT")
    private String approvalComment;

    @TableField("PROCESS_INSTANCE_ID")
    private String processInstanceId;

    @TableField("PROJECT_ID")
    private String projectId;

    // Non-DB transient fields for display
    @TableField(exist = false)
    private String createByName;

    @TableField(exist = false)
    private String sponsorName;
}
