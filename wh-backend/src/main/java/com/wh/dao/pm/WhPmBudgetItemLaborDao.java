package com.wh.dao.pm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wh.entity.pm.WhPmBudgetItemLabor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WhPmBudgetItemLaborDao extends BaseMapper<WhPmBudgetItemLabor> {

    @Select("SELECT * FROM pm_budget_item_labor WHERE BUDGET_ITEM_ID = #{budgetItemId} AND DEL_FLAG = '0'")
    List<WhPmBudgetItemLabor> selectByBudgetItemId(@Param("budgetItemId") String budgetItemId);
}
