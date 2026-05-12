package com.wh.controller.pm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wh.fixtures.AuthHelper;
import com.wh.fixtures.TestFixtures;
import com.wh.entity.pm.WhPmCharter;
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
@DisplayName("WhPmBudgetController 集成测试")
class WhPmBudgetControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthHelper authHelper;
    @Autowired private TestFixtures fixtures;

    private String pmToken;
    private String projectId;

    @BeforeEach
    void setUp() {
        pmToken = authHelper.pmToken();
        WhPmCharter project = fixtures.createTestProject("预算测试项目");
        projectId = project.getId();
    }

    @Test
    @DisplayName("GET /api/pm/budgets - 分页查询返回列表")
    void list_returnsPagedList() throws Exception {
        mockMvc.perform(get("/api/pm/budgets")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @DisplayName("POST /api/pm/budgets - 创建预算")
    void create_validRequest_createsBudget() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "projectId", projectId
        ));
        mockMvc.perform(post("/api/pm/budgets")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.version").value("v0.5"));
    }

    @Test
    @DisplayName("GET /api/pm/budgets/{id}/detail - 查看预算明细")
    void detail_returnsDetail() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("projectId", projectId));
        String resp = mockMvc.perform(post("/api/pm/budgets")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        String budgetId = objectMapper.readTree(resp).get("data").get("id").asText();

        mockMvc.perform(get("/api/pm/budgets/{id}/detail", budgetId)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/pm/budgets/versions/{projectId} - 版本历史")
    void versions_returnsHistory() throws Exception {
        mockMvc.perform(get("/api/pm/budgets/versions/{projectId}", projectId)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/pm/budgets/{id}/comparison - 新预算预实对比")
    void comparison_returnsData() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("projectId", projectId));
        String resp = mockMvc.perform(post("/api/pm/budgets")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        String budgetId = objectMapper.readTree(resp).get("data").get("id").asText();

        mockMvc.perform(get("/api/pm/budgets/{id}/comparison", budgetId)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk());
    }
}
