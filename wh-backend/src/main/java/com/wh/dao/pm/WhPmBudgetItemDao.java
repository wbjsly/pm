package com.wh.dao.pm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wh.entity.pm.WhPmBudgetItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WhPmBudgetItemDao extends BaseMapper<WhPmBudgetItem> {

    @Select("SELECT * FROM pm_budget_item WHERE BUDGET_ID = #{budgetId} AND DEL_FLAG = '0' AND PARENT_ID IS NULL ORDER BY SORT_ORDER")
    List<WhPmBudgetItem> selectPrimaryItems(@Param("budgetId") String budgetId);

    @Select("SELECT * FROM pm_budget_item WHERE PARENT_ID = #{parentId} AND DEL_FLAG = '0' ORDER BY SORT_ORDER")
    List<WhPmBudgetItem> selectChildren(@Param("parentId") String parentId);

    @Select("SELECT * FROM pm_budget_item WHERE BUDGET_ID = #{budgetId} AND DEL_FLAG = '0' ORDER BY SORT_ORDER")
    List<WhPmBudgetItem> selectAllByBudgetId(@Param("budgetId") String budgetId);
}
