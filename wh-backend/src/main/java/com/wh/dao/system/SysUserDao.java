package com.wh.dao.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wh.entity.system.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserDao extends BaseMapper<SysUser> {
}
