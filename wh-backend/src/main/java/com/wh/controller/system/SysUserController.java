package com.wh.controller.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wh.common.R;
import com.wh.dao.system.SysPositionDao;
import com.wh.dao.system.SysRoleDao;
import com.wh.dao.system.SysUserDao;
import com.wh.dao.system.SysUserRoleDao;
import com.wh.entity.system.SysPosition;
import com.wh.entity.system.SysUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/system/users")
public class SysUserController {

    private final SysUserDao sysUserDao;
    private final SysUserRoleDao sysUserRoleDao;
    private final SysRoleDao sysRoleDao;
    private final PasswordEncoder passwordEncoder;
    private final SysPositionDao sysPositionDao;

    public SysUserController(SysUserDao sysUserDao,
                             SysUserRoleDao sysUserRoleDao,
                             SysRoleDao sysRoleDao,
                             PasswordEncoder passwordEncoder,
                             SysPositionDao sysPositionDao) {
        this.sysUserDao = sysUserDao;
        this.sysUserRoleDao = sysUserRoleDao;
        this.sysRoleDao = sysRoleDao;
        this.passwordEncoder = passwordEncoder;
        this.sysPositionDao = sysPositionDao;
    }

    @GetMapping
    public R<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String positionId) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getDelFlag, "0");
        if (status != null && !status.isEmpty()) {
            wrapper.eq(SysUser::getStatus, status);
        }
        if (positionId != null && !positionId.isEmpty()) {
            wrapper.eq(SysUser::getPositionId, positionId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SysUser::getRealName, keyword)
                   .or()
                   .like(SysUser::getUsername, keyword);
        }
        wrapper.orderByAsc(SysUser::getUsername);
        IPage<SysUser> result = sysUserDao.selectPage(page, wrapper);

        // Enrich with role codes
        List<Map<String, Object>> records = new ArrayList<>();
        for (SysUser user : result.getRecords()) {
            Map<String, Object> map = userToMap(user);
            map.put("roles", getUserRoleCodes(user.getId()));
            records.add(map);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("total", result.getTotal());
        response.put("pageNum", result.getCurrent());
        response.put("pageSize", result.getSize());
        response.put("records", records);
        return R.ok(response);
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable String id) {
        SysUser user = sysUserDao.selectById(id);
        if (user == null || "1".equals(user.getDelFlag())) {
            return R.fail(404, "用户不存在");
        }
        Map<String, Object> map = userToMap(user);
        map.put("roles", getUserRoleCodes(id));
        return R.ok(map);
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable String id, @RequestBody Map<String, Object> req) {
        SysUser user = sysUserDao.selectById(id);
        if (user == null || "1".equals(user.getDelFlag())) {
            return R.fail(404, "用户不存在");
        }

        if (req.containsKey("nickName")) user.setNickName((String) req.get("nickName"));
        if (req.containsKey("realName")) user.setRealName((String) req.get("realName"));
        if (req.containsKey("email")) user.setEmail((String) req.get("email"));
        if (req.containsKey("phone")) user.setPhone((String) req.get("phone"));
        if (req.containsKey("status")) user.setStatus((String) req.get("status"));
        if (req.containsKey("positionId")) user.setPositionId((String) req.get("positionId"));

        sysUserDao.updateById(user);

        // Update roles if provided
        if (req.containsKey("roleIds")) {
            @SuppressWarnings("unchecked")
            List<String> roleIds = (List<String>) req.get("roleIds");
            sysUserRoleDao.deleteByUserId(id);
            for (String roleId : roleIds) {
                String urId = UUID.randomUUID().toString().replace("-", "");
                sysUserRoleDao.insertUserRole(urId, id, roleId);
            }
        }

        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        SysUser user = sysUserDao.selectById(id);
        if (user == null || "1".equals(user.getDelFlag())) {
            return R.fail(404, "用户不存在");
        }
        user.setDelFlag("1");
        sysUserDao.updateById(user);
        return R.ok();
    }

    @PutMapping("/{id}/password")
    public R<Void> resetPassword(@PathVariable String id, @RequestBody Map<String, String> req) {
        SysUser user = sysUserDao.selectById(id);
        if (user == null || "1".equals(user.getDelFlag())) {
            return R.fail(404, "用户不存在");
        }
        String password = req.get("password");
        if (password == null || password.isEmpty()) {
            return R.fail(400, "密码不能为空");
        }
        user.setPassword(passwordEncoder.encode(password));
        sysUserDao.updateById(user);
        return R.ok();
    }

    private Map<String, Object> userToMap(SysUser user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("nickName", user.getNickName());
        map.put("realName", user.getRealName());
        map.put("email", user.getEmail());
        map.put("phone", user.getPhone());
        map.put("avatar", user.getAvatar());
        map.put("status", user.getStatus());
        map.put("positionId", user.getPositionId());
        if (user.getPositionId() != null) {
            SysPosition pos = sysPositionDao.selectById(user.getPositionId());
            map.put("positionName", pos != null ? pos.getName() : null);
        } else {
            map.put("positionName", null);
        }
        return map;
    }

    private List<String> getUserRoleCodes(String userId) {
        List<Map<String, Object>> roles = sysUserRoleDao.getUserRoles(userId);
        return roles.stream()
                .map(r -> (String) r.get("ROLE_CODE"))
                .collect(Collectors.toList());
    }
}
