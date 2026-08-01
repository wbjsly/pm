package com.wh.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wh.dao.system.SysUserDao;
import com.wh.entity.system.SysUser;
import com.wh.fixtures.AuthHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"sqlite", "dev"})
@DisplayName("AuthController 集成测试")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthHelper authHelper;

    @Autowired
    private SysUserDao sysUserDao;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private static final String LOGIN_URL = "/api/auth/login";
    private static final String LOGOUT_URL = "/api/auth/logout";
    private static final String INFO_URL = "/api/auth/info";

    // Admin user from seed data (001-system-foundation.sql)
    // username: admin, password: Admin@123 (BCrypt hashed)
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "Admin@123";

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginTests {

        @Test
        @DisplayName("正确用户名和密码 → 返回token")
        void loginWithCorrectCredentials_ShouldReturnToken() throws Exception {
            String body = String.format("{\"username\":\"%s\",\"password\":\"%s\"}",
                    ADMIN_USERNAME, ADMIN_PASSWORD);

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.token").isNotEmpty())
                    .andExpect(jsonPath("$.data.username").value("admin"));
        }

        @Test
        @DisplayName("不存在的用户 → 返回code=401")
        void loginWithNonexistentUser_ShouldReturn401() throws Exception {
            String body = "{\"username\":\"nonexistent\",\"password\":\"whatever\"}";

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401))
                    .andExpect(jsonPath("$.message").value("用户不存在: nonexistent"));
        }

        @Test
        @DisplayName("错误密码 → 返回code=401")
        void loginWithWrongPassword_ShouldReturn401() throws Exception {
            String body = String.format("{\"username\":\"%s\",\"password\":\"%s\"}",
                    ADMIN_USERNAME, "wrongPassword");

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401))
                    .andExpect(jsonPath("$.message").value("用户名或密码错误"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("退出登录 → 返回成功")
        void logout_ShouldReturnSuccess() throws Exception {
            mockMvc.perform(post(LOGOUT_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Nested
    @DisplayName("GET /api/auth/info")
    class InfoTests {

        @Test
        @DisplayName("无token → 返回code=401")
        void getInfoWithoutToken_ShouldReturn401() throws Exception {
            mockMvc.perform(get(INFO_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401))
                    .andExpect(jsonPath("$.message").value("未登录"));
        }

        @Test
        @DisplayName("无效token → 返回code=401")
        void getInfoWithInvalidToken_ShouldReturn401() throws Exception {
            mockMvc.perform(get(INFO_URL)
                            .header("Authorization", "Bearer invalid_token_here"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401))
                    .andExpect(jsonPath("$.message").value("无效的token"));
        }

        @Test
        @DisplayName("有效token → 返回用户信息")
        void getInfoWithValidToken_ShouldReturnUserInfo() throws Exception {
            // First login to get a real token
            String loginBody = String.format("{\"username\":\"%s\",\"password\":\"%s\"}",
                    ADMIN_USERNAME, ADMIN_PASSWORD);

            String response = mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // Parse token from login response
            String token = objectMapper.readTree(response).get("data").get("token").asText();

            // Use token to call /api/auth/info
            mockMvc.perform(get(INFO_URL)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.username").value("admin"))
                    .andExpect(jsonPath("$.data.userId").exists())
                    .andExpect(jsonPath("$.data.roles").isArray());
        }
    }

    @Nested
    @DisplayName("补充 - 登录与信息边界")
    class ExtraAuthTests {

        @Test
        @DisplayName("禁用用户登录 - 返回 403")
        void loginDisabledUser_returns403() throws Exception {
            SysUser u = new SysUser();
            u.setUsername("disabled_" + System.nanoTime());
            u.setPassword("$2a$10$KvoqroXik9qiqRA8MLTZTe4XrU1QurZ3dLZVEg9CJNfImhr1AGD3W"); // Admin@123
            u.setStatus("0");
            u.setDelFlag("0");
            u.setVerNo(0);
            sysUserDao.insert(u);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    java.util.Map.of("username", u.getUsername(), "password", "Admin@123"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(403));
            jdbcCleanup(u.getId());
        }

        @Test
        @DisplayName("非 Bearer 前缀的 Authorization - 返回 401")
        void getInfoWithNonBearer_returns401() throws Exception {
            mockMvc.perform(get("/api/auth/info")
                            .header("Authorization", "Basic abc123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401));
        }

        @Test
        @DisplayName("有效 token 但用户不存在 - 返回 404")
        void getInfoWithUnknownUser_returns404() throws Exception {
            String token = authHelper.generateToken("nonexistent_user_xyz", java.util.List.of("ROLE_USER"));
            mockMvc.perform(get("/api/auth/info")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(404));
        }

        private void jdbcCleanup(String id) {
            jdbcTemplate.update("DELETE FROM sys_user WHERE ID = ?", id);
        }
    }
}
