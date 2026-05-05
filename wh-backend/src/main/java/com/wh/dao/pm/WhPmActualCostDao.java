package com.wh.dao.pm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wh.entity.pm.WhPmActualCost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WhPmActualCostDao extends BaseMapper<WhPmActualCost> {

    @Select("SELECT COALESCE(SUM(CAST(AMOUNT AS REAL)), 0) FROM pm_actual_cost WHERE BUDGET_ITEM_ID = #{budgetItemId} AND DEL_FLAG = '0'")
    Double sumAmountByBudgetItemId(@Param("budgetItemId") String budgetItemId);

    @Select("SELECT * FROM pm_actual_cost WHERE PROJECT_ID = #{projectId} AND DEL_FLAG = '0' ORDER BY COST_DATE DESC")
    List<WhPmActualCost> selectByProjectId(@Param("projectId") String projectId);

    @Select("SELECT COALESCE(SUM(CAST(AMOUNT AS REAL)), 0) FROM pm_actual_cost WHERE PROJECT_ID = #{projectId} AND DEL_FLAG = '0'")
    Double sumAmountByProjectId(@Param("projectId") String projectId);
}
