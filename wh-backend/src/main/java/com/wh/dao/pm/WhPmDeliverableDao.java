package com.wh.dao.pm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wh.entity.pm.WhPmDeliverable;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WhPmDeliverableDao extends BaseMapper<WhPmDeliverable> {

    @Delete("DELETE FROM wh_pm_deliverable WHERE ID = #{id}")
    int physicalDeleteById(String id);
}
