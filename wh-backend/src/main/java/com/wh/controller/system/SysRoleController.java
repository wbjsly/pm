package com.wh.controller.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wh.common.R;
import com.wh.dao.system.SysRoleDao;
import com.wh.entity.system.SysRole;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/system/roles")
public class SysRoleController {

    private final SysRoleDao sysRoleDao;

    public SysRoleController(SysRoleDao sysRoleDao) {
        this.sysRoleDao = sysRoleDao;
    }

    @GetMapping
    public R<List<Map<String, Object>>> list() {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getDelFlag, "0");
        wrapper.eq(SysRole::getStatus, "1");
        wrapper.orderByAsc(SysRole::getSortOrder);
        List<SysRole> roles = sysRoleDao.selectList(wrapper);
        List<Map<String, Object>> result = roles.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("roleCode", r.getRoleCode());
            map.put("roleName", r.getRoleName());
            return map;
        }).collect(Collectors.toList());
        return R.ok(result);
    }
}
