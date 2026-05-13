package com.wh.controller.system;

import com.wh.bo.system.WhPmMenuBo;
import com.wh.common.R;
import com.wh.entity.system.SysMenu;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/system/menus")
public class SysMenuController {

    private final WhPmMenuBo menuBo;

    public SysMenuController(WhPmMenuBo menuBo) {
        this.menuBo = menuBo;
    }

    /** 获取当前用户可见的菜单 */
    @GetMapping("/user")
    public R<List<SysMenu>> getUserMenus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        return R.ok(menuBo.getUserMenus(roles));
    }

    /** 全量查询（管理页面） */
    @GetMapping
    public R<List<SysMenu>> getAll() {
        return R.ok(menuBo.getAllMenus());
    }

    /** 单条查询 */
    @GetMapping("/{id}")
    public R<SysMenu> getOne(@PathVariable String id) {
        return R.ok(menuBo.getById(id));
    }

    /** 新增 */
    @PostMapping
    public R<SysMenu> create(@RequestBody SysMenu menu) {
        return R.ok(menuBo.create(menu));
    }

    /** 更新 */
    @PutMapping
    public R<SysMenu> update(@RequestBody SysMenu menu) {
        return R.ok(menuBo.update(menu));
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        menuBo.delete(id);
        return R.ok();
    }
}
