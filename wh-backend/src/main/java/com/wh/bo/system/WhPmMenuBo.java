package com.wh.bo.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wh.dao.system.SysMenuDao;
import com.wh.entity.system.SysMenu;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class WhPmMenuBo {

    private final SysMenuDao menuDao;

    public WhPmMenuBo(SysMenuDao menuDao) {
        this.menuDao = menuDao;
    }

    /**
     * 获取当前用户可见的菜单列表，按 SORT_ORDER 排序。
     * admin 角色看全部启用的菜单，其他角色按 perm 字段精确匹配。
     */
    public List<SysMenu> getUserMenus(List<String> roles) {
        List<SysMenu> all = menuDao.selectList(
                new LambdaQueryWrapper<SysMenu>()
                        .eq(SysMenu::getStatus, "1")
                        .orderByAsc(SysMenu::getSortOrder)
        );
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        boolean isAdmin = roles.stream().anyMatch(r -> "ROLE_ADMIN".equalsIgnoreCase(r));
        if (isAdmin) {
            return all;
        }
        Set<String> roleSet = roles.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
        return all.stream()
                .filter(m -> {
                    if (m.getPerm() == null || m.getPerm().isEmpty()) return false;
                    return Arrays.stream(m.getPerm().split(","))
                            .map(String::trim)
                            .anyMatch(roleSet::contains);
                })
                .collect(Collectors.toList());
    }

    /** 全量查询（管理页面用） */
    public List<SysMenu> getAllMenus() {
        return menuDao.selectList(
                new LambdaQueryWrapper<SysMenu>()
                        .orderByAsc(SysMenu::getSortOrder)
        );
    }

    /** 单条查询 */
    public SysMenu getById(String id) {
        return menuDao.selectById(id);
    }

    /** 新增 */
    public SysMenu create(SysMenu menu) {
        menuDao.insert(menu);
        return menu;
    }

    /** 更新 */
    public SysMenu update(SysMenu menu) {
        menuDao.updateById(menu);
        return menu;
    }

    /** 逻辑删除 */
    public void delete(String id) {
        menuDao.deleteById(id);
    }
}
