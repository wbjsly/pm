package com.wh.dao.pm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wh.entity.pm.WhPmWorkLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface WhPmWorkLogDao extends BaseMapper<WhPmWorkLog> {

    @Select("SELECT SUM(CAST(HOURS_WORKED AS REAL)) FROM pm_work_log " +
            "WHERE CREATE_BY = #{userId} AND LOG_DATE = #{logDate} AND STATUS != 'DELETED' AND DEL_FLAG = '0'")
    BigDecimal sumHoursByUserAndDate(@Param("userId") String userId, @Param("logDate") String logDate);

    @Select("SELECT * FROM pm_work_log WHERE DEL_FLAG = '0' " +
            "AND CREATE_BY = #{userId} AND LOG_DATE >= #{startDate} AND LOG_DATE <= #{endDate} " +
            "ORDER BY LOG_DATE, PROJECT_ID")
    List<WhPmWorkLog> selectByUserAndMonth(@Param("userId") String userId,
                                            @Param("startDate") String startDate,
                                            @Param("endDate") String endDate);

    @Select("SELECT wl.* FROM pm_work_log wl " +
            "INNER JOIN wh_pm_project_charter pc ON wl.PROJECT_ID = pc.ID " +
            "WHERE wl.DEL_FLAG = '0' AND wl.STATUS = 'DRAFT' AND pc.PM_ID = #{pmId} " +
            "AND wl.LOG_DATE >= #{startDate} AND wl.LOG_DATE <= #{endDate} " +
            "ORDER BY wl.LOG_DATE, wl.PROJECT_ID")
    List<WhPmWorkLog> selectPendingByPm(@Param("pmId") String pmId,
                                         @Param("startDate") String startDate,
                                         @Param("endDate") String endDate);

    @Select("SELECT wl.* FROM pm_work_log wl " +
            "WHERE wl.DEL_FLAG = '0' AND wl.ID IN " +
            "(SELECT wl2.ID FROM pm_work_log wl2 " +
            " INNER JOIN wh_pm_project_charter pc2 ON wl2.PROJECT_ID = pc2.ID " +
            " WHERE pc2.PM_ID = #{pmId} AND wl2.STATUS = 'DRAFT') " +
            "AND wl.ID IN (${ids})")
    List<WhPmWorkLog> selectPmApprovableIds(@Param("pmId") String pmId, @Param("ids") String ids);

    @Select("SELECT SUM(CAST(HOURS_WORKED AS REAL)) FROM pm_work_log " +
            "WHERE CREATE_BY = #{userId} AND LOG_DATE >= #{startDate} AND LOG_DATE <= #{endDate} " +
            "AND DEL_FLAG = '0'")
    BigDecimal sumHoursByUserAndMonth(@Param("userId") String userId,
                                       @Param("startDate") String startDate,
                                       @Param("endDate") String endDate);
}
