package com.wh.bo.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wh.common.ServiceException;
import com.wh.dao.system.SysUserDao;
import com.wh.entity.system.SysUser;
import com.wh.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 认证业务逻辑：登录签发 token、根据 token 获取用户信息。
 */
@Service
public class AuthBo {

    private final PasswordEncoder passwordEncoder;
    private final SysUserDao sysUserDao;
    private final SysUserBo sysUserBo;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthBo(PasswordEncoder passwordEncoder,
                  SysUserDao sysUserDao,
                  SysUserBo sysUserBo,
                  JwtTokenProvider jwtTokenProvider) {
        this.passwordEncoder = passwordEncoder;
        this.sysUserDao = sysUserDao;
        this.sysUserBo = sysUserBo;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public Map<String, Object> login(String username, String password) {
        SysUser user = sysUserDao.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));

        if (user == null) {
            throw new ServiceException(401, "用户不存在: " + username);
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ServiceException(401, "用户名或密码错误");
        }
        if (!"1".equals(user.getStatus())) {
            throw new ServiceException(403, "用户已禁用");
        }

        List<String> roleCodes = sysUserBo.getUserRoleCodes(user.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("token", jwtTokenProvider.generateToken(user.getId(), Map.of("username", username, "roles", roleCodes)));
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("nickName", user.getNickName());
        result.put("roles", roleCodes);
        return result;
    }

    public Map<String, Object> getUserInfo(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ServiceException(401, "未登录");
        }

        String token = authHeader.substring(7);
        String userId;
        try {
            userId = jwtTokenProvider.getSubjectFromToken(token);
        } catch (Exception e) {
            throw new ServiceException(401, "无效的token");
        }

        SysUser user = sysUserDao.selectById(userId);
        if (user == null) {
            throw new ServiceException(404, "用户不存在");
        }

        List<String> roleCodes = sysUserBo.getUserRoleCodes(user.getId());

        Map<String, Object> info = new HashMap<>();
        info.put("userId", user.getId());
        info.put("username", user.getUsername());
        info.put("nickName", user.getNickName());
        info.put("roles", roleCodes);
        info.put("permissions", Collections.emptyList());
        return info;
    }
}
