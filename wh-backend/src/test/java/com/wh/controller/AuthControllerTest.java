package com.wh.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
}
