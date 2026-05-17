package com.wh;

import com.wh.bo.system.WhPmMenuBo;
import com.wh.dao.system.SysMenuDao;
import com.wh.entity.system.SysMenu;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles({"sqlite", "dev"})
@Transactional
@DisplayName("菜单 BO 测试")
class WhPmMenuBoTest {

    @Autowired
    private WhPmMenuBo menuBo;

    @Autowired
    private SysMenuDao menuDao;

    // ══════════════════════════════════════════════════
    //  用户菜单
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("用户菜单查询")
    class UserMenuTests {

        @Test
        @DisplayName("admin 角色看到所有启用的菜单")
        void testGetUserMenus_Admin() {
            List<SysMenu> menus = menuBo.getUserMenus(List.of("ROLE_ADMIN"));
            assertFalse(menus.isEmpty());
            assertTrue(menus.stream().allMatch(m -> "1".equals(m.getStatus())));
        }

        @Test
        @DisplayName("PM 角色只看到 perm 匹配的菜单")
        void testGetUserMenus_PM() {
            // 系统种子菜单中应包含 pm 相关的菜单
            List<SysMenu> pmMenus = menuBo.getUserMenus(List.of("ROLE_PM"));
            assertFalse(pmMenus.isEmpty());
            // 每个菜单的 perm 字段应包含 ROLE_PM
            assertTrue(pmMenus.stream().allMatch(m ->
                    m.getPerm() != null && m.getPerm().toUpperCase().contains("ROLE_PM")));
        }

        @Test
        @DisplayName("空角色列表返回空集合")
        void testGetUserMenus_EmptyRoles() {
            List<SysMenu> menus = menuBo.getUserMenus(List.of());
            assertTrue(menus.isEmpty());
        }

        @Test
        @DisplayName("null 角色列表返回空集合")
        void testGetUserMenus_NullRoles() {
            List<SysMenu> menus = menuBo.getUserMenus(null);
            assertTrue(menus.isEmpty());
        }

        @Test
        @DisplayName("不匹配的角色不返回菜单")
        void testGetUserMenus_NoMatch() {
            List<SysMenu> menus = menuBo.getUserMenus(List.of("ROLE_NON_EXISTENT"));
            assertTrue(menus.isEmpty());
        }

        @Test
        @DisplayName("用户菜单按 SORT_ORDER 排序")
        void testGetUserMenus_Sorted() {
            List<SysMenu> menus = menuBo.getUserMenus(List.of("ROLE_ADMIN"));
            for (int i = 0; i < menus.size() - 1; i++) {
                Integer curr = menus.get(i).getSortOrder();
                Integer next = menus.get(i + 1).getSortOrder();
                if (curr != null && next != null) {
                    assertTrue(curr <= next, "菜单应按 SORT_ORDER 升序排列");
                }
            }
        }
    }

    // ══════════════════════════════════════════════════
    //  全量查询
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("全量菜单查询")
    class GetAllMenusTests {

        @Test
        @DisplayName("getAllMenus - 返回所有菜单（含禁用）")
        void testGetAllMenus() {
            // 先创建一个禁用菜单
            SysMenu disabled = new SysMenu();
            disabled.setTitle("禁用菜单");
            disabled.setPath("/disabled");
            disabled.setComponent("Disabled");
            disabled.setStatus("0"); // 禁用
            menuBo.create(disabled);

            List<SysMenu> all = menuBo.getAllMenus();
            assertTrue(all.stream().anyMatch(m -> m.getTitle().equals("禁用菜单")));
        }

        @Test
        @DisplayName("getAllMenus - 按 SORT_ORDER 排序")
        void testGetAllMenus_Sorted() {
            List<SysMenu> all = menuBo.getAllMenus();
            for (int i = 0; i < all.size() - 1; i++) {
                Integer curr = all.get(i).getSortOrder();
                Integer next = all.get(i + 1).getSortOrder();
                if (curr != null && next != null) {
                    assertTrue(curr <= next);
                }
            }
        }
    }

    // ══════════════════════════════════════════════════
    //  单条查询
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("单条菜单查询")
    class GetByIdTests {

        @Test
        @DisplayName("getById - 存在返回")
        void testGetById_Success() {
            SysMenu menu = new SysMenu();
            menu.setTitle("临时菜单");
            menu.setPath("/tmp");
            menu.setComponent("Tmp");
            SysMenu saved = menuBo.create(menu);

            SysMenu found = menuBo.getById(saved.getId());
            assertNotNull(found);
            assertEquals("临时菜单", found.getTitle());
        }

        @Test
        @DisplayName("getById - 不存在返回 null")
        void testGetById_NotFound() {
            SysMenu found = menuBo.getById("non-existent-id");
            assertNull(found);
        }
    }

    // ══════════════════════════════════════════════════
    //  CRUD
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("菜单 CRUD")
    class MenuCrudTests {

        @Test
        @DisplayName("create - 创建菜单")
        void testCreate() {
            SysMenu menu = new SysMenu();
            menu.setTitle("新建菜单");
            menu.setPath("/new");
            menu.setComponent("NewView");
            menu.setIcon("el-icon-setting");
            menu.setSortOrder(99);
            menu.setStatus("1");

            SysMenu saved = menuBo.create(menu);

            assertNotNull(saved.getId());
            assertEquals("新建菜单", saved.getTitle());
            assertEquals("1", saved.getStatus());
        }

        @Test
        @DisplayName("update - 更新菜单")
        void testUpdate() {
            SysMenu menu = new SysMenu();
            menu.setTitle("原名");
            menu.setPath("/orig");
            menu.setComponent("Orig");
            SysMenu saved = menuBo.create(menu);

            saved.setTitle("更新名");
            saved.setIcon("el-icon-edit");
            menuBo.update(saved);

            SysMenu updated = menuDao.selectById(saved.getId());
            assertEquals("更新名", updated.getTitle());
            assertEquals("el-icon-edit", updated.getIcon());
        }

        @Test
        @DisplayName("delete - 删除菜单")
        void testDelete() {
            SysMenu menu = new SysMenu();
            menu.setTitle("待删除菜单");
            menu.setPath("/del");
            menu.setComponent("Del");
            SysMenu saved = menuBo.create(menu);

            // 删除后 admin 应看不到该菜单
            List<SysMenu> before = menuBo.getUserMenus(List.of("ROLE_ADMIN"));
            menuBo.delete(saved.getId());
            List<SysMenu> after = menuBo.getUserMenus(List.of("ROLE_ADMIN"));

            assertEquals(before.size() - 1, after.size());
            assertTrue(after.stream().noneMatch(m -> saved.getId().equals(m.getId())));
        }
    }
}
