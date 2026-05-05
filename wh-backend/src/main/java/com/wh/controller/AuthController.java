package com.wh.controller;

import com.wh.common.R;
import com.wh.dao.system.SysRoleDao;
import com.wh.dao.system.SysUserDao;
import com.wh.dao.system.SysUserRoleDao;
import com.wh.entity.system.SysUser;
import com.wh.security.JwtTokenProvider;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final PasswordEncoder passwordEncoder;
    private final SysUserDao sysUserDao;
    private final SysUserRoleDao sysUserRoleDao;
    private final SysRoleDao sysRoleDao;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(PasswordEncoder passwordEncoder, SysUserDao sysUserDao,
                          SysUserRoleDao sysUserRoleDao, SysRoleDao sysRoleDao,
                          JwtTokenProvider jwtTokenProvider) {
        this.passwordEncoder = passwordEncoder;
        this.sysUserDao = sysUserDao;
        this.sysUserRoleDao = sysUserRoleDao;
        this.sysRoleDao = sysRoleDao;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody Map<String, String> loginReq) {
        String username = loginReq.get("username");
        String password = loginReq.get("password");

        SysUser user = sysUserDao.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));

        if (user == null) {
            return R.fail(401, "用户不存在: " + username);
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return R.fail(401, "用户名或密码错误");
        }

        if (!"1".equals(user.getStatus())) {
            return R.fail(403, "用户已禁用");
        }

        List<Map<String, Object>> userRoles = sysUserRoleDao.getUserRoles(user.getId());
        List<String> roleCodes = userRoles.stream()
                .map(r -> (String) r.get("ROLE_CODE"))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("token", jwtTokenProvider.generateToken(user.getId(), Map.of("username", username, "roles", roleCodes)));
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("nickName", user.getNickName());
        result.put("roles", roleCodes);

        return R.ok(result);
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        return R.ok();
    }

    @GetMapping("/info")
    public R<Map<String, Object>> getUserInfo(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return R.fail(401, "未登录");
        }

        String token = authHeader.substring(7);
        String userId;
        String username;
        try {
            userId = jwtTokenProvider.getSubjectFromToken(token);
            var claims = jwtTokenProvider.getClaims(token);
            username = (String) claims.get("username");
        } catch (Exception e) {
            return R.fail(401, "无效的token");
        }

        SysUser user = sysUserDao.selectById(userId);
        if (user == null) {
            return R.fail(404, "用户不存在");
        }

        List<Map<String, Object>> userRoles = sysUserRoleDao.getUserRoles(user.getId());
        List<String> roleCodes = userRoles.stream()
                .map(r -> (String) r.get("ROLE_CODE"))
                .collect(Collectors.toList());

        Map<String, Object> info = new HashMap<>();
        info.put("userId", user.getId());
        info.put("username", user.getUsername());
        info.put("nickName", user.getNickName());
        info.put("roles", roleCodes);
        info.put("permissions", Collections.emptyList());

        return R.ok(info);
    }
}
