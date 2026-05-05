package com.wh.entity.pm;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wh.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_cost_warning")
public class WhPmCostWarning extends BaseEntity {

    @TableField("PROJECT_ID")
    private String projectId;

    @TableField("BUDGET_ID")
    private String budgetId;

    @TableField("LEVEL")
    private String level;

    @TableField("RATIO")
    private String ratio;

    @TableField("STATUS")
    private String status;

    @TableField("TRIGGERED_AT")
    private String triggeredAt;

    @TableField("CLOSED_BY")
    private String closedBy;

    @TableField("CLOSED_AT")
    private String closedAt;
}
