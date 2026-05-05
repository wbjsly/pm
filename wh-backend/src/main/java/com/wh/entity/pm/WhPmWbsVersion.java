package com.wh.entity.pm;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wh.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_wbs_version")
public class WhPmWbsVersion extends BaseEntity {

    @TableField("WBS_ID")
    private String wbsId;

    @TableField("VERSION_NUMBER")
    private java.math.BigDecimal versionNumber;

    @TableField("PLANNED_START_DATE")
    private String plannedStartDate;

    @TableField("PLANNED_END_DATE")
    private String plannedEndDate;

    @TableField("ACTUAL_START_DATE")
    private String actualStartDate;

    @TableField("ACTUAL_END_DATE")
    private String actualEndDate;
}
