package com.wh.controller.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wh.dao.system.SysMenuDao;
import com.wh.entity.system.SysMenu;
import com.wh.fixtures.AuthHelper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"sqlite", "dev"})
@DisplayName("SysMenuController 集成测试")
class SysMenuControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthHelper authHelper;
    @Autowired private SysMenuDao menuDao;

    private String adminToken;
    private SysMenu testMenu;

    @BeforeEach
    void setUp() {
        adminToken = authHelper.adminToken();
        testMenu = new SysMenu();
        testMenu.setTitle("测试菜单");
        testMenu.setPath("/test-menu");
        testMenu.setComponent("TestMenu");
        testMenu.setIcon("el-icon-setting");
        testMenu.setSortOrder(99);
        testMenu.setStatus("1");
        menuDao.insert(testMenu);
    }

    // ══════════════════════════════════════════════════
    //  GET /api/system/menus/user
    // ══════════════════════════════════════════════════

    @Test @DisplayName("GET /api/system/menus/user - admin 返回菜单列表")
    void getUserMenus_adminUser_returnsMenus() throws Exception {
        mockMvc.perform(get("/api/system/menus/user")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test @DisplayName("GET /api/system/menus/user - 无 token 返回 403")
    void getUserMenus_withoutToken_403() throws Exception {
        mockMvc.perform(get("/api/system/menus/user"))
                .andExpect(status().isForbidden());
    }

    // ══════════════════════════════════════════════════
    //  GET /api/system/menus
    // ══════════════════════════════════════════════════

    @Test @DisplayName("GET /api/system/menus - 返回所有菜单")
    void getAll_returnsAllMenus() throws Exception {
        mockMvc.perform(get("/api/system/menus")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test @DisplayName("GET /api/system/menus - 无 token 返回 403")
    void getAll_withoutToken_403() throws Exception {
        mockMvc.perform(get("/api/system/menus"))
                .andExpect(status().isForbidden());
    }

    // ══════════════════════════════════════════════════
    //  GET /api/system/menus/{id}
    // ══════════════════════════════════════════════════

    @Test @DisplayName("GET /api/system/menus/{id} - 存在的菜单返回详情")
    void getOne_existingId_returnsDetail() throws Exception {
        mockMvc.perform(get("/api/system/menus/{id}", testMenu.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(testMenu.getId()))
                .andExpect(jsonPath("$.data.title").value("测试菜单"));
    }

    @Test @DisplayName("GET /api/system/menus/{id} - 不存在的 ID 返回空")
    void getOne_nonexistentId_returnsNull() throws Exception {
        mockMvc.perform(get("/api/system/menus/{id}", "nonexistent-id")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test @DisplayName("GET /api/system/menus/{id} - 无 token 返回 403")
    void getOne_withoutToken_403() throws Exception {
        mockMvc.perform(get("/api/system/menus/some-id"))
                .andExpect(status().isForbidden());
    }

    // ══════════════════════════════════════════════════
    //  POST /api/system/menus
    // ══════════════════════════════════════════════════

    @Test @DisplayName("POST /api/system/menus - 创建菜单成功")
    void create_validRequest_createsMenu() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "新菜单", "path", "/new-menu",
                "component", "NewMenu", "icon", "el-icon-setting",
                "sortOrder", 50, "status", "1"));
        mockMvc.perform(post("/api/system/menus")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("新菜单"));
    }

    @Test @DisplayName("POST /api/system/menus - 无 token 返回 403")
    void create_withoutToken_403() throws Exception {
        mockMvc.perform(post("/api/system/menus")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    // ══════════════════════════════════════════════════
    //  PUT /api/system/menus
    // ══════════════════════════════════════════════════

    @Test @DisplayName("PUT /api/system/menus - 更新菜单成功")
    void update_validRequest_updatesMenu() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "id", testMenu.getId(), "title", "更新后的菜单"));
        mockMvc.perform(put("/api/system/menus")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("更新后的菜单"));
    }

    @Test @DisplayName("PUT /api/system/menus - 无 token 返回 403")
    void update_withoutToken_403() throws Exception {
        mockMvc.perform(put("/api/system/menus")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    // ══════════════════════════════════════════════════
    //  DELETE /api/system/menus/{id}
    // ══════════════════════════════════════════════════

    @Test @DisplayName("DELETE /api/system/menus/{id} - 删除菜单成功")
    void deleteMenu_success() throws Exception {
        mockMvc.perform(delete("/api/system/menus/{id}", testMenu.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test @DisplayName("DELETE /api/system/menus/{id} - 无 token 返回 403")
    void delete_withoutToken_403() throws Exception {
        mockMvc.perform(delete("/api/system/menus/some-id"))
                .andExpect(status().isForbidden());
    }
}
