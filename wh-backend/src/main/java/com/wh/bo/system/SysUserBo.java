package com.wh.bo.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wh.common.ServiceException;
import com.wh.dao.system.SysPositionDao;
import com.wh.dao.system.SysUserDao;
import com.wh.dao.system.SysUserRoleDao;
import com.wh.entity.system.SysPosition;
import com.wh.entity.system.SysUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户管理业务逻辑。
 * 同时对外提供用户 ID → 姓名的批量查询，供各 PM BO 复用，避免逐条 selectById 的 N+1 问题。
 */
@Service
public class SysUserBo {

    private final SysUserDao sysUserDao;
    private final SysUserRoleDao sysUserRoleDao;
    private final SysPositionDao sysPositionDao;
    private final PasswordEncoder passwordEncoder;

    public SysUserBo(SysUserDao sysUserDao,
                     SysUserRoleDao sysUserRoleDao,
                     SysPositionDao sysPositionDao,
                     PasswordEncoder passwordEncoder) {
        this.sysUserDao = sysUserDao;
        this.sysUserRoleDao = sysUserRoleDao;
        this.sysPositionDao = sysPositionDao;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 批量获取用户 ID → 真实姓名映射（单次 IN 查询）。不存在的用户不会出现在结果中。
     */
    public Map<String, String> getRealNameMap(Collection<String> userIds) {
        Set<String> ids = userIds == null ? Set.of() : userIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> !id.isEmpty())
                .collect(Collectors.toSet());
        // 注意：使用 HashMap 而非 Map.of()/Collectors.toMap，
        // 以容忍 null key 查询（外键字段可能为 null）与 null value（realName 可能为 null）。
        Map<String, String> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        for (SysUser user : sysUserDao.selectBatchIds(ids)) {
            map.put(user.getId(), user.getRealName());
        }
        return map;
    }

    /**
     * 统计某岗位下未删除的用户数。
     */
    public long countUsersByPositionId(String positionId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getPositionId, positionId)
               .eq(SysUser::getDelFlag, "0");
        return sysUserDao.selectCount(wrapper);
    }

    /**
     * 获取用户的角色编码列表。
     */
    public List<String> getUserRoleCodes(String userId) {
        return sysUserRoleDao.getUserRoles(userId).stream()
                .map(r -> (String) r.get("ROLE_CODE"))
                .collect(Collectors.toList());
    }

    public Map<String, Object> pageUsers(int pageNum, int pageSize, String keyword, String status, String positionId) {
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
            // 必须用 and(...) 嵌套，否则 OR 会绕过 del_flag/status 等前置条件
            wrapper.and(w -> w.like(SysUser::getRealName, keyword)
                    .or()
                    .like(SysUser::getUsername, keyword));
        }
        wrapper.orderByAsc(SysUser::getUsername);
        IPage<SysUser> result = sysUserDao.selectPage(page, wrapper);

        // 批量加载岗位与角色，避免逐用户查询
        Set<String> positionIds = result.getRecords().stream()
                .map(SysUser::getPositionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, String> positionNameMap = getPositionNameMap(positionIds);

        Set<String> userIds = result.getRecords().stream()
                .map(SysUser::getId)
                .collect(Collectors.toSet());
        Map<String, List<String>> roleCodesMap = getRoleCodesMap(userIds);

        List<Map<String, Object>> records = new ArrayList<>();
        for (SysUser user : result.getRecords()) {
            Map<String, Object> map = userToMap(user, positionNameMap);
            map.put("roles", roleCodesMap.getOrDefault(user.getId(), List.of()));
            records.add(map);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("total", result.getTotal());
        response.put("pageNum", result.getCurrent());
        response.put("pageSize", result.getSize());
        response.put("records", records);
        return response;
    }

    public Map<String, Object> getUserDetail(String id) {
        SysUser user = getExistingUser(id);
        Map<String, Object> map = userToMap(user,
                getPositionNameMap(user.getPositionId() != null ? Set.of(user.getPositionId()) : Set.of()));
        map.put("roles", getUserRoleCodes(id));
        return map;
    }

    @Transactional
    public void updateUser(String id, Map<String, Object> req) {
        SysUser user = getExistingUser(id);

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
    }

    public void deleteUser(String id) {
        SysUser user = getExistingUser(id);
        user.setDelFlag("1");
        sysUserDao.updateById(user);
    }

    public void resetPassword(String id, String password) {
        SysUser user = getExistingUser(id);
        if (password == null || password.isEmpty()) {
            throw new ServiceException(400, "密码不能为空");
        }
        user.setPassword(passwordEncoder.encode(password));
        sysUserDao.updateById(user);
    }

    private SysUser getExistingUser(String id) {
        SysUser user = sysUserDao.selectById(id);
        if (user == null || "1".equals(user.getDelFlag())) {
            throw new ServiceException(404, "用户不存在");
        }
        return user;
    }

    private Map<String, List<String>> getRoleCodesMap(Collection<String> userIds) {
        if (userIds.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, List<String>> result = new HashMap<>();
        for (Map<String, Object> row : sysUserRoleDao.getRoleCodesByUserIds(userIds)) {
            String userId = (String) row.get("USER_ID");
            String roleCode = (String) row.get("ROLE_CODE");
            result.computeIfAbsent(userId, k -> new ArrayList<>()).add(roleCode);
        }
        return result;
    }

    private Map<String, String> getPositionNameMap(Collection<String> positionIds) {
        Map<String, String> map = new HashMap<>();
        if (positionIds.isEmpty()) {
            return map;
        }
        for (SysPosition pos : sysPositionDao.selectBatchIds(positionIds)) {
            map.put(pos.getId(), pos.getName());
        }
        return map;
    }

    private Map<String, Object> userToMap(SysUser user, Map<String, String> positionNameMap) {
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
        map.put("positionName", user.getPositionId() != null ? positionNameMap.get(user.getPositionId()) : null);
        return map;
    }
}
