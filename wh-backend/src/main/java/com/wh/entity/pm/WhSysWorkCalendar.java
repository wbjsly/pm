package com.wh.entity.pm;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wh.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wh_sys_work_calendar")
public class WhSysWorkCalendar extends BaseEntity {

    @TableField("CALENDAR_DATE")
    private String calendarDate;

    @TableField("DAY_TYPE")
    private String dayType;

    @TableField("HOLIDAY_NAME")
    private String holidayName;

    @TableField("STANDARD_HOURS")
    private String standardHours;

    @TableField("YEAR")
    private String year;
}
