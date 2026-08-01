package com.wh.controller.system;

import com.wh.fixtures.AuthHelper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"sqlite", "dev"})
@DisplayName("SysUserController 集成测试")
class SysUserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AuthHelper authHelper;

    private String adminToken;

    @BeforeEach
    void setUp() {
        adminToken = authHelper.adminToken();
    }

    @Test
    @DisplayName("GET /api/system/users - 分页查询用户列表")
    void list_returnsPagedList() throws Exception {
        mockMvc.perform(get("/api/system/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    private com.wh.dao.system.SysUserDao sysUserDao;

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private static final String SEED_USER_ID = "user00000000000000000000000000001";

    @Test
    @DisplayName("GET /api/system/users/{id} - 查询用户详情")
    void detail_existingId_returnsDetail() throws Exception {
        mockMvc.perform(get("/api/system/users/{id}", SEED_USER_ID)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(SEED_USER_ID));
    }

    @Test
    @DisplayName("GET /api/system/users/{id} - 不存在的用户返回 404")
    void detail_nonexistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/system/users/{id}", "nonexistent-user")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("PUT /api/system/users/{id} - 更新用户")
    void update_success() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "nickName", "测试昵称", "email", "updated@example.com"));
        mockMvc.perform(put("/api/system/users/{id}", SEED_USER_ID)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        // 恢复原值，避免污染
        String restore = objectMapper.writeValueAsString(java.util.Map.of(
                "nickName", "系统管理员", "email", "admin@example.com"));
        mockMvc.perform(put("/api/system/users/{id}", SEED_USER_ID)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(restore))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/system/users/{id} - 不存在的用户返回 404")
    void update_nonexistentId_returns404() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("nickName", "x"));
        mockMvc.perform(put("/api/system/users/{id}", "nonexistent-user")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("DELETE /api/system/users/{id} - 删除用户（逻辑删除）")
    void delete_success() throws Exception {
        // 插入临时用户
        com.wh.entity.system.SysUser u = new com.wh.entity.system.SysUser();
        u.setUsername("temp_delete_" + System.nanoTime());
        u.setPassword("x");
        u.setStatus("1");
        u.setDelFlag("0");
        u.setVerNo(0);
        sysUserDao.insert(u);
        String tempId = u.getId();

        mockMvc.perform(delete("/api/system/users/{id}", tempId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        // 清理
        jdbcTemplate.update("DELETE FROM sys_user WHERE ID = ?", tempId);
    }

    @Test
    @DisplayName("DELETE /api/system/users/{id} - 不存在的用户返回 404")
    void delete_nonexistentId_returns404() throws Exception {
        mockMvc.perform(delete("/api/system/users/{id}", "nonexistent-user")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("PUT /api/system/users/{id}/password - 重置密码")
    void resetPassword_success() throws Exception {
        // 使用临时用户，避免污染 seed 用户密码导致登录测试失败
        com.wh.entity.system.SysUser u = new com.wh.entity.system.SysUser();
        u.setUsername("temp_pwd_" + System.nanoTime());
        u.setPassword("x");
        u.setStatus("1");
        u.setDelFlag("0");
        u.setVerNo(0);
        sysUserDao.insert(u);
        String tempId = u.getId();

        String body = objectMapper.writeValueAsString(java.util.Map.of("password", "NewPass@123"));
        mockMvc.perform(put("/api/system/users/{id}/password", tempId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        jdbcTemplate.update("DELETE FROM sys_user WHERE ID = ?", tempId);
    }

    @Test
    @DisplayName("PUT /api/system/users/{id}/password - 空密码返回 400")
    void resetPassword_empty_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("password", ""));
        mockMvc.perform(put("/api/system/users/{id}/password", SEED_USER_ID)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }
}
