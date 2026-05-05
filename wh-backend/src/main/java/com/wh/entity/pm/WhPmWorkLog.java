package com.wh.entity.pm;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wh.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_work_log")
public class WhPmWorkLog extends BaseEntity {

    @TableField("PROJECT_ID")
    private String projectId;

    @TableField("TASK_ID")
    private String taskId;

    @TableField("LOG_DATE")
    private String logDate;

    @TableField("WORK_DESCRIPTION")
    private String workDescription;

    @TableField("HOURS_WORKED")
    private String hoursWorked;

    @TableField("STATUS")
    private String status;

    @TableField("BLOCKER_REASON")
    private String blockerReason;

    // Non-DB fields for display
    @TableField(exist = false)
    private String projectName;

    @TableField(exist = false)
    private String projectShortName;

    @TableField(exist = false)
    private String createByName;
}
