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
@DisplayName("WhPmActualCostController 集成测试")
class WhPmActualCostControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthHelper authHelper;
    @Autowired private TestFixtures fixtures;

    private String pmToken;
    private String projectId;

    @BeforeEach
    void setUp() {
        pmToken = authHelper.pmToken();
        WhPmCharter project = fixtures.createTestProject("成本测试项目");
        projectId = project.getId();
    }

    @Test
    @DisplayName("GET /api/pm/actual-costs - 分页查询")
    void list_returnsPagedList() throws Exception {
        mockMvc.perform(get("/api/pm/actual-costs")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @DisplayName("GET /api/pm/actual-costs - 按项目筛选")
    void list_filterByProject() throws Exception {
        mockMvc.perform(get("/api/pm/actual-costs")
                        .header("Authorization", "Bearer " + pmToken)
                        .param("projectId", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/pm/actual-costs/aggregation - 汇总查询")
    void aggregation_returnsData() throws Exception {
        mockMvc.perform(get("/api/pm/actual-costs/aggregation")
                        .header("Authorization", "Bearer " + pmToken)
                        .param("projectId", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/pm/actual-costs/sum - 合计查询")
    void sum_returnsNumber() throws Exception {
        mockMvc.perform(get("/api/pm/actual-costs/sum")
                        .header("Authorization", "Bearer " + pmToken)
                        .param("projectId", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("POST /api/pm/actual-costs - 创建成本调用可达")
    void create_validRequest_endpointReachable() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "projectId", projectId,
                "costAmount", "5000.00",
                "costType", "人力成本",
                "yearMonth", "2026-01"
        ));
        mockMvc.perform(post("/api/pm/actual-costs")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    // ═══════════════════════════════════════════════════════
    //  补充：detail / delete
    // ═══════════════════════════════════════════════════════

    private String createActualCost() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "projectId", projectId,
                "amount", "5000.00",
                "costType", "人力成本",
                "costDate", "2026-01-15",
                "description", "补充成本"));
        String resp = mockMvc.perform(post("/api/pm/actual-costs")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resp).get("data").get("id").asText();
    }

    @Test
    @DisplayName("GET /api/pm/actual-costs/{id} - 查询单条成本")
    void detail_existingId_returnsDetail() throws Exception {
        String id = createActualCost();
        mockMvc.perform(get("/api/pm/actual-costs/{id}", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(id));
    }

    @Test
    @DisplayName("DELETE /api/pm/actual-costs/{id} - 删除手动成本")
    void delete_success() throws Exception {
        String id = createActualCost();
        mockMvc.perform(delete("/api/pm/actual-costs/{id}", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/pm/actual-costs/{id} - 不存在的 ID 返回 404")
    void detail_nonexistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/pm/actual-costs/{id}", "nonexistent-id")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }
}
