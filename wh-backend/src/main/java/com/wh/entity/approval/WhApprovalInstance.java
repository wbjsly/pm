package com.wh.entity.approval;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wh_approval_instance")
public class WhApprovalInstance {
    private String id;
    @TableField("BIZ_ID")
    private String bizId;
    @TableField("FLOW_CODE")
    private String flowCode;
    @TableField("PROCESS_INSTANCE_ID")
    private String processInstanceId;
    @TableField("STATUS")
    private String status;
    @TableField("CREATE_DATE")
    private LocalDateTime createDate;
}
