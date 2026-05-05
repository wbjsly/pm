package com.wh.entity.approval;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wh_approval_record")
public class WhApprovalRecord {
    private String id;
    @TableField("INSTANCE_ID")
    private String instanceId;
    @TableField("APPROVER")
    private String approver;
    @TableField("ACTION")
    private String action;
    @TableField("COMMENT")
    private String comment;
    @TableField("APPROVE_DATE")
    private LocalDateTime approveDate;
}
