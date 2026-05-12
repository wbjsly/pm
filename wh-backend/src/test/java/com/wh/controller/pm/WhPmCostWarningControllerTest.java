package com.wh.controller.pm;

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
@DisplayName("WhPmCostWarningController 集成测试")
class WhPmCostWarningControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AuthHelper authHelper;

    private String pmToken;

    @BeforeEach
    void setUp() {
        pmToken = authHelper.pmToken();
    }

    @Test
    @DisplayName("GET /api/pm/cost-warnings - 查询列表")
    void list_returnsList() throws Exception {
        mockMvc.perform(get("/api/pm/cost-warnings")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/pm/cost-warnings - 按项目和状态筛选")
    void list_filterByProjectAndStatus() throws Exception {
        mockMvc.perform(get("/api/pm/cost-warnings")
                        .header("Authorization", "Bearer " + pmToken)
                        .param("projectId", "test-project")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/pm/cost-warnings/active - 活跃预警")
    void listActive_returnsLimited() throws Exception {
        mockMvc.perform(get("/api/pm/cost-warnings/active")
                        .header("Authorization", "Bearer " + pmToken)
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("POST /api/pm/cost-warnings/trigger - 触发计算")
    void trigger_returnsOk() throws Exception {
        mockMvc.perform(post("/api/pm/cost-warnings/trigger")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
