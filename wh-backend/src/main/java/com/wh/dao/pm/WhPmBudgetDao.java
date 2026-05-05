package com.wh.dao.pm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wh.entity.pm.WhPmBudget;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WhPmBudgetDao extends BaseMapper<WhPmBudget> {

    @Delete("DELETE FROM pm_budget WHERE ID = #{id}")
    int physicalDeleteById(String id);
}
