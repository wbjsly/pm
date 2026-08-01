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
@DisplayName("WhPmWorkLogController 集成测试")
class WhPmWorkLogControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthHelper authHelper;
    @Autowired private TestFixtures fixtures;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private String pmToken;
    private String projectId;

    @BeforeEach
    void setUp() {
        pmToken = authHelper.pmToken();
        WhPmCharter project = fixtures.createTestProject("工时测试项目");
        projectId = project.getId();
    }

    @Test
    @DisplayName("GET /api/pm/work-hours - 无 token 返回 403")
    void list_withoutToken_403() throws Exception {
        mockMvc.perform(get("/api/pm/work-hours").param("year", "2026").param("month", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/pm/work-hours - 正常查询返回列表")
    void list_withToken_returnsList() throws Exception {
        mockMvc.perform(get("/api/pm/work-hours")
                        .header("Authorization", "Bearer " + pmToken)
                        .param("year", "2026")
                        .param("month", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("POST /api/pm/work-hours - 创建工时调用成功")
    void create_validRequest_endpointReachable() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "projectId", projectId,
                "logDate", "2026-01-05",
                "hoursWorked", "8",
                "workDescription", "集成测试"
        ));
        mockMvc.perform(post("/api/pm/work-hours")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/pm/work-hours/stats - 查询统计")
    void stats_returnsStats() throws Exception {
        mockMvc.perform(get("/api/pm/work-hours/stats")
                        .header("Authorization", "Bearer " + pmToken)
                        .param("year", "2026")
                        .param("month", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/pm/work-hours/pending - 待审批列表")
    void pending_returnsList() throws Exception {
        mockMvc.perform(get("/api/pm/work-hours/pending")
                        .header("Authorization", "Bearer " + pmToken)
                        .param("year", "2026")
                        .param("month", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/pm/work-hours/by-project - 按项目查询")
    void byProject_returnsList() throws Exception {
        mockMvc.perform(get("/api/pm/work-hours/by-project")
                        .header("Authorization", "Bearer " + pmToken)
                        .param("projectId", projectId)
                        .param("year", "2026")
                        .param("month", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("POST /api/pm/work-hours/batch-approve - 空列表返回结果")
    void batchApprove_emptyList_returnsResult() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("ids", java.util.List.of()));
        mockMvc.perform(post("/api/pm/work-hours/batch-approve")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("POST /api/pm/work-hours/batch-reject - 空列表返回结果")
    void batchReject_emptyList_returnsResult() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "ids", java.util.List.of(),
                "reason", "批量驳回"
        ));
        mockMvc.perform(post("/api/pm/work-hours/batch-reject")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("PUT /api/pm/work-hours/{id} - 不存在的工时返回错误")
    void update_nonexistentId_returnsError() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "projectId", projectId,
                "logDate", "2026-01-05",
                "hoursWorked", "8",
                "workDescription", "更新"
        ));
        mockMvc.perform(put("/api/pm/work-hours/{id}", "nonexistent-id")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ═══════════════════════════════════════════════════════
    //  补充：get / approve / reject / resubmit / delete
    // ═══════════════════════════════════════════════════════

    private String createWorkLog(String date, String hours) throws Exception {
        // 清理该日期残留数据，避免重复运行累积导致 24 小时上限
        jdbcTemplate.update("DELETE FROM pm_work_log WHERE LOG_DATE = ?", date);
        String body = objectMapper.writeValueAsString(Map.of(
                "projectId", projectId, "logDate", date,
                "hoursWorked", hours, "workDescription", "补充测试"));
        String resp = mockMvc.perform(post("/api/pm/work-hours")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resp).get("data").get("id").asText();
    }

    @Test
    @DisplayName("GET /api/pm/work-hours/{id} - 查询单条工时")
    void get_existingId_returnsDetail() throws Exception {
        String id = createWorkLog("2026-01-05", "8");
        mockMvc.perform(get("/api/pm/work-hours/{id}", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(id));
    }

    @Test
    @DisplayName("POST /api/pm/work-hours/{id}/approve - PM 审批通过")
    void approve_success() throws Exception {
        String id = createWorkLog("2026-01-06", "8");
        String body = objectMapper.writeValueAsString(Map.of("reason", "同意"));
        mockMvc.perform(post("/api/pm/work-hours/{id}/approve", id)
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/pm/work-hours/{id}", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    @DisplayName("POST /api/pm/work-hours/{id}/reject - PM 驳回")
    void reject_success() throws Exception {
        String id = createWorkLog("2026-01-07", "8");
        String body = objectMapper.writeValueAsString(Map.of("reason", "工时不符"));
        mockMvc.perform(post("/api/pm/work-hours/{id}/reject", id)
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/pm/work-hours/{id}", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    @DisplayName("POST /api/pm/work-hours/{id}/resubmit - 驳回后重新提交")
    void resubmit_success() throws Exception {
        String id = createWorkLog("2026-01-08", "8");
        mockMvc.perform(post("/api/pm/work-hours/{id}/reject", id)
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"修改\"}"));
        mockMvc.perform(post("/api/pm/work-hours/{id}/resubmit", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/pm/work-hours/{id}", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    @DisplayName("DELETE /api/pm/work-hours/{id} - 删除草稿工时")
    void delete_success() throws Exception {
        String id = createWorkLog("2026-01-09", "8");
        mockMvc.perform(delete("/api/pm/work-hours/{id}", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("POST /api/pm/work-hours/{id}/approve - 无请求体调用")
    void approve_withoutBody() throws Exception {
        String id = createWorkLog("2026-02-05", "8");
        mockMvc.perform(post("/api/pm/work-hours/{id}/approve", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
