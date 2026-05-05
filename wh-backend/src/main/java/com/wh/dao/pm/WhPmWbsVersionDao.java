package com.wh.dao.pm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wh.entity.pm.WhPmWbsVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WhPmWbsVersionDao extends BaseMapper<WhPmWbsVersion> {

    @Select("SELECT * FROM pm_wbs_version WHERE WBS_ID = #{wbsId} ORDER BY VERSION_NUMBER DESC")
    List<WhPmWbsVersion> selectByWbsId(String wbsId);

    @Select("SELECT MAX(VERSION_NUMBER) FROM pm_wbs_version WHERE WBS_ID = #{wbsId}")
    java.math.BigDecimal selectMaxVersionNumber(String wbsId);
}
