package com.wh.dao.pm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wh.entity.pm.WhPmActualCost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface WhPmActualCostDao extends BaseMapper<WhPmActualCost> {

    @Select("SELECT COALESCE(SUM(CAST(AMOUNT AS REAL)), 0) FROM pm_actual_cost WHERE BUDGET_ITEM_ID = #{budgetItemId} AND DEL_FLAG = '0'")
    Double sumAmountByBudgetItemId(@Param("budgetItemId") String budgetItemId);

    @Select("SELECT * FROM pm_actual_cost WHERE PROJECT_ID = #{projectId} AND DEL_FLAG = '0' ORDER BY COST_DATE DESC")
    List<WhPmActualCost> selectByProjectId(@Param("projectId") String projectId);

    @Select("SELECT COALESCE(SUM(CAST(AMOUNT AS REAL)), 0) FROM pm_actual_cost WHERE PROJECT_ID = #{projectId} AND DEL_FLAG = '0'")
    Double sumAmountByProjectId(@Param("projectId") String projectId);

    @Select("SELECT SUBSTR(COST_DATE, 1, 7) AS yearMonth, " +
            "COALESCE(SUM(CASE WHEN COST_TYPE = 'LABOR' THEN CAST(AMOUNT AS REAL) ELSE 0 END), 0) AS laborAmount, " +
            "COALESCE(SUM(CASE WHEN COST_TYPE = 'PROCUREMENT' THEN CAST(AMOUNT AS REAL) ELSE 0 END), 0) AS procurementAmount, " +
            "COALESCE(SUM(CASE WHEN COST_TYPE = 'TRAVEL' THEN CAST(AMOUNT AS REAL) ELSE 0 END), 0) AS travelAmount, " +
            "COALESCE(SUM(CASE WHEN COST_TYPE = 'BUSINESS' THEN CAST(AMOUNT AS REAL) ELSE 0 END), 0) AS businessAmount, " +
            "COALESCE(SUM(CASE WHEN COST_TYPE = 'ENTERTAINMENT' THEN CAST(AMOUNT AS REAL) ELSE 0 END), 0) AS entertainmentAmount, " +
            "COALESCE(SUM(CASE WHEN COST_TYPE = 'ACTIVITY' THEN CAST(AMOUNT AS REAL) ELSE 0 END), 0) AS activityAmount, " +
            "COALESCE(SUM(CASE WHEN COST_TYPE = 'OTHER' THEN CAST(AMOUNT AS REAL) ELSE 0 END), 0) AS otherAmount " +
            "FROM pm_actual_cost WHERE PROJECT_ID = #{projectId} AND DEL_FLAG = '0' " +
            "GROUP BY SUBSTR(COST_DATE, 1, 7) ORDER BY yearMonth")
    List<Map<String, Object>> aggregateMonthlyByProject(@Param("projectId") String projectId);

    @Select("<script>" +
            "SELECT COALESCE(SUM(CAST(AMOUNT AS REAL)), 0) FROM pm_actual_cost " +
            "WHERE PROJECT_ID = #{projectId} AND DEL_FLAG = '0' " +
            "<if test='yearMonths != null and yearMonths.length > 0'>AND ( " +
            "<foreach collection='yearMonths' item='ym' separator=' OR '>COST_DATE LIKE #{ym} || '%'</foreach>) " +
            "</if>" +
            "<if test='costTypes != null and costTypes.length > 0'>AND COST_TYPE IN " +
            "<foreach collection='costTypes' item='ct' open='(' separator=',' close=')'>#{ct}</foreach></if>" +
            "</script>")
    Double sumAmountByFilter(@Param("projectId") String projectId,
                             @Param("yearMonths") String[] yearMonths,
                             @Param("costTypes") String[] costTypes);
}
