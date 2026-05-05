package com.wh.dao.pm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wh.entity.pm.WhPmCostWarning;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WhPmCostWarningDao extends BaseMapper<WhPmCostWarning> {

    @Select("SELECT * FROM pm_cost_warning WHERE PROJECT_ID = #{projectId} AND STATUS = 'ACTIVE' AND DEL_FLAG = '0' ORDER BY CASE LEVEL WHEN 'CRITICAL' THEN 1 WHEN 'WARN' THEN 2 WHEN 'INFO' THEN 3 ELSE 4 END")
    List<WhPmCostWarning> selectActiveByProjectId(@Param("projectId") String projectId);

    @Select("SELECT * FROM pm_cost_warning WHERE STATUS = 'ACTIVE' AND DEL_FLAG = '0' ORDER BY CASE LEVEL WHEN 'CRITICAL' THEN 1 WHEN 'WARN' THEN 2 WHEN 'INFO' THEN 3 ELSE 4 END LIMIT #{limit}")
    List<WhPmCostWarning> selectActiveAll(@Param("limit") int limit);
}
