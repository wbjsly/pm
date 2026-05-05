package com.wh.dao.pm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wh.entity.pm.WhSysWorkCalendar;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WhSysWorkCalendarDao extends BaseMapper<WhSysWorkCalendar> {

    @Select("SELECT * FROM wh_sys_work_calendar WHERE CALENDAR_DATE = #{date} AND DEL_FLAG = '0'")
    WhSysWorkCalendar selectByDate(@Param("date") String date);

    @Select("SELECT * FROM wh_sys_work_calendar WHERE YEAR = #{year} AND DEL_FLAG = '0' ORDER BY CALENDAR_DATE")
    List<WhSysWorkCalendar> selectByYear(@Param("year") String year);

    @Select("SELECT * FROM wh_sys_work_calendar WHERE YEAR = #{year} AND CALENDAR_DATE BETWEEN #{startDate} AND #{endDate} AND DEL_FLAG = '0' ORDER BY CALENDAR_DATE")
    List<WhSysWorkCalendar> selectByYearAndRange(@Param("year") String year, @Param("startDate") String startDate, @Param("endDate") String endDate);
}
