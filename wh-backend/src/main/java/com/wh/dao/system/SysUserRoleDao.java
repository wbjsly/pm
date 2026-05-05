package com.wh.dao.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wh.entity.system.SysUserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SysUserRoleDao extends BaseMapper<SysUserRole> {

    @Select("SELECT sr.ROLE_CODE FROM sys_user_role sur " +
            "INNER JOIN sys_role sr ON sur.ROLE_ID = sr.ID " +
            "WHERE sur.USER_ID = #{userId}")
    List<Map<String, Object>> getUserRoles(String userId);

    @org.apache.ibatis.annotations.Delete("DELETE FROM sys_user_role WHERE USER_ID = #{userId}")
    int deleteByUserId(String userId);

    @org.apache.ibatis.annotations.Insert("INSERT INTO sys_user_role (ID, USER_ID, ROLE_ID) VALUES (#{id}, #{userId}, #{roleId})")
    int insertUserRole(@org.apache.ibatis.annotations.Param("id") String id,
                       @org.apache.ibatis.annotations.Param("userId") String userId,
                       @org.apache.ibatis.annotations.Param("roleId") String roleId);
}
