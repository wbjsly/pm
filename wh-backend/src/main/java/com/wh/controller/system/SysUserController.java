package com.wh.controller.system;

import com.wh.bo.system.SysUserBo;
import com.wh.common.R;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/system/users")
public class SysUserController {

    private final SysUserBo sysUserBo;

    public SysUserController(SysUserBo sysUserBo) {
        this.sysUserBo = sysUserBo;
    }

    @GetMapping
    public R<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String positionId) {
        return R.ok(sysUserBo.pageUsers(pageNum, pageSize, keyword, status, positionId));
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable String id) {
        return R.ok(sysUserBo.getUserDetail(id));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable String id, @RequestBody Map<String, Object> req) {
        sysUserBo.updateUser(id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        sysUserBo.deleteUser(id);
        return R.ok();
    }

    @PutMapping("/{id}/password")
    public R<Void> resetPassword(@PathVariable String id, @RequestBody Map<String, String> req) {
        sysUserBo.resetPassword(id, req.get("password"));
        return R.ok();
    }
}
