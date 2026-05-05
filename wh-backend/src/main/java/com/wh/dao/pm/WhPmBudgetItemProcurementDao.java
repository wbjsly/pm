package com.wh.dao.pm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wh.entity.pm.WhPmBudgetItemProcurement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WhPmBudgetItemProcurementDao extends BaseMapper<WhPmBudgetItemProcurement> {

    @Select("SELECT * FROM pm_budget_item_procurement WHERE BUDGET_ITEM_ID = #{budgetItemId} AND DEL_FLAG = '0'")
    List<WhPmBudgetItemProcurement> selectByBudgetItemId(@Param("budgetItemId") String budgetItemId);
}
