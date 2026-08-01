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

    // ─── 辅助方法 ───

    private String createBudget() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("projectId", projectId));
        String resp = mockMvc.perform(post("/api/pm/budgets")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resp).get("data").get("id").asText();
    }

    // ══════════════════════════════════════════════════
    //  无 token 访问
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("无 token 访问")
    class NoToken {

        @Test
        @DisplayName("GET /api/pm/budgets 返回 403")
        void list_withoutToken_403() throws Exception {
            mockMvc.perform(get("/api/pm/budgets"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /api/pm/budgets 返回 403")
        void create_withoutToken_403() throws Exception {
            mockMvc.perform(post("/api/pm/budgets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PUT /api/pm/budgets/{id} 返回 403")
        void update_withoutToken_403() throws Exception {
            mockMvc.perform(put("/api/pm/budgets/some-id")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DELETE /api/pm/budgets/{id} 返回 403")
        void delete_withoutToken_403() throws Exception {
            mockMvc.perform(delete("/api/pm/budgets/some-id"))
                    .andExpect(status().isForbidden());
        }
    }

    // ══════════════════════════════════════════════════
    //  GET /api/pm/budgets
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/pm/budgets")
    class List {

        @Test
        @DisplayName("分页查询返回列表")
        void returnsPagedList() throws Exception {
            mockMvc.perform(get("/api/pm/budgets")
                            .header("Authorization", "Bearer " + pmToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.records").isArray());
        }

        @Test
        @DisplayName("按项目筛选")
        void filterByProject() throws Exception {
            mockMvc.perform(get("/api/pm/budgets")
                            .header("Authorization", "Bearer " + pmToken)
                            .param("projectId", projectId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    // ══════════════════════════════════════════════════
    //  POST /api/pm/budgets
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/pm/budgets")
    class Create {

        @Test
        @DisplayName("正常创建预算")
        void validRequest_createsBudget() throws Exception {
            String body = objectMapper.writeValueAsString(Map.of("projectId", projectId));
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
        @DisplayName("不存在的项目ID返回500")
        void nonexistentProjectId_returns500() throws Exception {
            String body = objectMapper.writeValueAsString(Map.of("projectId", "nonexistent-project-id"));
            mockMvc.perform(post("/api/pm/budgets")
                            .header("Authorization", "Bearer " + pmToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }
    }

    // ══════════════════════════════════════════════════
    //  GET /api/pm/budgets/{id}
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/pm/budgets/{id}")
    class Detail {

        @Test
        @DisplayName("存在的预算返回详情")
        void existingId_returnsDetail() throws Exception {
            String id = createBudget();
            mockMvc.perform(get("/api/pm/budgets/{id}", id)
                            .header("Authorization", "Bearer " + pmToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(id));
        }

        @Test
        @DisplayName("不存在的ID返回404")
        void nonexistentId_returns404() throws Exception {
            mockMvc.perform(get("/api/pm/budgets/{id}", "nonexistent-id")
                            .header("Authorization", "Bearer " + pmToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(404));
        }
    }

    // ══════════════════════════════════════════════════
    //  GET /api/pm/budgets/{id}/detail
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/pm/budgets/{id}/detail")
    class DetailWithItems {

        @Test
        @DisplayName("查看预算明细")
        void detail_returnsDetail() throws Exception {
            String id = createBudget();
            mockMvc.perform(get("/api/pm/budgets/{id}/detail", id)
                            .header("Authorization", "Bearer " + pmToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    // ══════════════════════════════════════════════════
    //  GET /api/pm/budgets/versions/{projectId}
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/pm/budgets/versions/{projectId}")
    class Versions {

        @Test
        @DisplayName("版本历史")
        void versions_returnsHistory() throws Exception {
            mockMvc.perform(get("/api/pm/budgets/versions/{projectId}", projectId)
                            .header("Authorization", "Bearer " + pmToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    // ══════════════════════════════════════════════════
    //  PUT /api/pm/budgets/{id}
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("PUT /api/pm/budgets/{id}")
    class Update {

        @Test
        @DisplayName("更新预算成功")
        void validRequest_updatesBudget() throws Exception {
            String id = createBudget();
            String updateBody = objectMapper.writeValueAsString(Map.of("items", java.util.List.of()));
            mockMvc.perform(put("/api/pm/budgets/{id}", id)
                            .header("Authorization", "Bearer " + pmToken)
                            .contentType(MediaType.APPLICATION_JSON).content(updateBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    // ══════════════════════════════════════════════════
    //  DELETE /api/pm/budgets/{id}
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /api/pm/budgets/{id}")
    class Delete {

        @Test
        @DisplayName("删除预算成功")
        void delete_success() throws Exception {
            String id = createBudget();
            mockMvc.perform(delete("/api/pm/budgets/{id}", id)
                            .header("Authorization", "Bearer " + pmToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("不存在的ID返回404")
        void delete_nonexistentId_returns404() throws Exception {
            mockMvc.perform(delete("/api/pm/budgets/{id}", "nonexistent-id")
                            .header("Authorization", "Bearer " + pmToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(404));
        }
    }


    // ══════════════════════════════════════════════════
    //  GET /api/pm/budgets/projects
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/pm/budgets/projects")
    class ProjectBudgets {

        @Test
        @DisplayName("项目预算列表")
        void projectBudgets_returnsList() throws Exception {
            mockMvc.perform(get("/api/pm/budgets/projects")
                            .header("Authorization", "Bearer " + pmToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.records").isArray());
        }
    }

    // ═══════════════════════════════════════════════════════
    //  补充：detailWithItems / submit / approve / reject / upgrade / comparison
    // ═══════════════════════════════════════════════════════

    private String createBudget(String projectIdParam) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "projectId", projectIdParam,
                "managementReserve", "1000"));
        String resp = mockMvc.perform(post("/api/pm/budgets")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resp).get("data").get("id").asText();
    }

    @Test
    @DisplayName("GET /api/pm/budgets/{id}/detail - 预算明细")
    void detailWithItems_success() throws Exception {
        String id = createBudget(projectId);
        mockMvc.perform(get("/api/pm/budgets/{id}/detail", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("POST /api/pm/budgets/{id}/submit - 提交审批")
    void submit_success() throws Exception {
        String id = createBudget(projectId);
        mockMvc.perform(post("/api/pm/budgets/{id}/submit", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/pm/budgets/{id}", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/pm/budgets/{id}/approve - 审批通过")
    void approve_success() throws Exception {
        String id = createBudget(projectId);
        mockMvc.perform(post("/api/pm/budgets/{id}/submit", id)
                        .header("Authorization", "Bearer " + pmToken));
        String body = objectMapper.writeValueAsString(Map.of("comment", "同意"));
        mockMvc.perform(post("/api/pm/budgets/{id}/approve", id)
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/pm/budgets/{id}", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    @DisplayName("POST /api/pm/budgets/{id}/reject - 审批驳回")
    void reject_success() throws Exception {
        String id = createBudget(projectId);
        mockMvc.perform(post("/api/pm/budgets/{id}/submit", id)
                        .header("Authorization", "Bearer " + pmToken));
        String body = objectMapper.writeValueAsString(Map.of("comment", "不同意"));
        mockMvc.perform(post("/api/pm/budgets/{id}/reject", id)
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        // 业务设计：驳回后回到草稿状态 DRAFT，可修改后重新提交
        mockMvc.perform(get("/api/pm/budgets/{id}", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.approvalComment").value("不同意"));
    }

    @Test
    @DisplayName("POST /api/pm/budgets/{id}/upgrade - 升级已审批预算")
    void upgrade_success() throws Exception {
        String id = createBudget(projectId);
        mockMvc.perform(post("/api/pm/budgets/{id}/submit", id)
                        .header("Authorization", "Bearer " + pmToken));
        mockMvc.perform(post("/api/pm/budgets/{id}/approve", id)
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"通过\"}"));
        String body = objectMapper.writeValueAsString(Map.of("managementReserve", "2000"));
        mockMvc.perform(post("/api/pm/budgets/{id}/upgrade", id)
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/pm/budgets/{id}/comparison - 预实对比")
    void comparison_success() throws Exception {
        String id = createBudget(projectId);
        mockMvc.perform(post("/api/pm/budgets/{id}/submit", id)
                        .header("Authorization", "Bearer " + pmToken));
        mockMvc.perform(post("/api/pm/budgets/{id}/approve", id)
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"通过\"}"));
        mockMvc.perform(get("/api/pm/budgets/{id}/comparison", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
