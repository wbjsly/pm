package com.wh.dao.pm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wh.entity.pm.WhPmCharter;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface WhPmCharterDao extends BaseMapper<WhPmCharter> {

    @Delete("DELETE FROM wh_pm_project_charter WHERE ID = #{id}")
    int physicalDeleteById(String id);

    @Select("SELECT STATUS as status, COUNT(*) as cnt FROM wh_pm_project_charter " +
            "WHERE PM_ID = #{pmId} AND DEL_FLAG = '0' GROUP BY STATUS")
    List<Map<String, Object>> selectStatsByPmId(String pmId);
}
