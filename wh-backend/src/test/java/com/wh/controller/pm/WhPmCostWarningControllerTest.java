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

import org.springframework.http.MediaType;
import java.util.Map;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;


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

    @org.springframework.beans.factory.annotation.Autowired
    private com.wh.dao.pm.WhPmCostWarningDao costWarningDao;

    @org.springframework.beans.factory.annotation.Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private final java.util.List<String> insertedWarningIds = new java.util.ArrayList<>();

    @AfterEach
    void tearDown() {
        for (String id : insertedWarningIds) {
            try { jdbcTemplate.update("DELETE FROM pm_cost_warning WHERE ID = ?", id); } catch (Exception ignored) {}
        }
        insertedWarningIds.clear();
    }

    private String insertWarning(String status) {
        com.wh.entity.pm.WhPmCostWarning w = new com.wh.entity.pm.WhPmCostWarning();
        w.setProjectId("warning-project");
        w.setBudgetId("warning-budget");
        w.setLevel("WARN");
        w.setRatio("0.90");
        w.setStatus(status);
        w.setTriggeredAt(java.time.LocalDateTime.now().toString());
        w.setDelFlag("0");
        w.setVerNo(0);
        costWarningDao.insert(w);
        insertedWarningIds.add(w.getId());
        return w.getId();
    }

    @Test
    @DisplayName("POST /api/pm/cost-warnings/{id}/close - 关闭预警")
    void close_existingId_returnsOk() throws Exception {
        String id = insertWarning("ACTIVE");
        mockMvc.perform(post("/api/pm/cost-warnings/{id}/close", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("POST /api/pm/cost-warnings/{id}/close - 不存在的预警返回 404")
    void close_nonexistentId_returns404() throws Exception {
        mockMvc.perform(post("/api/pm/cost-warnings/{id}/close", "nonexistent-warning")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("POST /api/pm/cost-warnings/{id}/close - 已关闭的预警返回错误")
    void close_alreadyClosed_returnsError() throws Exception {
        String id = insertWarning("CLOSED");
        mockMvc.perform(post("/api/pm/cost-warnings/{id}/close", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }
}
