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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"sqlite", "dev"})
@DisplayName("WhPmCharterController 集成测试")
class WhPmCharterControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthHelper authHelper;
    @Autowired private TestFixtures fixtures;

    private String pmToken;
    private String projectId;

    @BeforeEach
    void setUp() {
        pmToken = authHelper.pmToken();
        WhPmCharter project = fixtures.createTestProject("集成测试项目");
        projectId = project.getId();
    }

    @Nested
    @DisplayName("GET /api/pm/charters")
    class ListCharters {

        @Test
        @DisplayName("无 token 返回 403")
        void withoutToken_403() throws Exception {
            mockMvc.perform(get("/api/pm/charters"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("正常分页查询返回列表")
        void withToken_returnsPagedList() throws Exception {
            mockMvc.perform(get("/api/pm/charters")
                            .header("Authorization", "Bearer " + pmToken)
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.records").isArray())
                    .andExpect(jsonPath("$.data.total").isNumber());
        }

        @Test
        @DisplayName("按状态筛选")
        void filterByStatus() throws Exception {
            mockMvc.perform(get("/api/pm/charters")
                            .header("Authorization", "Bearer " + pmToken)
                            .param("status", "DRAFT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Nested
    @DisplayName("GET /api/pm/charters/{id}")
    class Detail {

        @Test
        @DisplayName("存在的项目返回详情")
        void existingId_returnsDetail() throws Exception {
            mockMvc.perform(get("/api/pm/charters/{id}", projectId)
                            .header("Authorization", "Bearer " + pmToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(projectId));
        }

        @Test
        @DisplayName("不存在的 ID 返回错误")
        void nonexistentId_returnsError() throws Exception {
            mockMvc.perform(get("/api/pm/charters/{id}", "nonexistent-id")
                            .header("Authorization", "Bearer " + pmToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(404));
        }
    }

    @Nested
    @DisplayName("POST /api/pm/charters")
    class Create {

        @Test
        @DisplayName("正常创建返回项目数据")
        void validRequest_createsProject() throws Exception {
            String body = objectMapper.writeValueAsString(Map.of(
                    "projectName", "新测试项目",
                    "projectCode", "NEW-" + System.currentTimeMillis(),
                    "projectShortName", "NTP",
                    "sponsorId", TestFixtures.PM_USER,
                    "pmId", TestFixtures.PM_USER
            ));
            mockMvc.perform(post("/api/pm/charters")
                            .header("Authorization", "Bearer " + pmToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").isNotEmpty())
                    .andExpect(jsonPath("$.data.status").value("DRAFT"));
        }

        @Test
        @DisplayName("必填字段缺失返回错误")
        void missingRequiredFields_returnsError() throws Exception {
            String body = objectMapper.writeValueAsString(Map.of(
                    "projectName", ""
            ));
            mockMvc.perform(post("/api/pm/charters")
                            .header("Authorization", "Bearer " + pmToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }
    }

    @Nested
    @DisplayName("PUT /api/pm/charters/{id}")
    class Update {

        @Test
        @DisplayName("更新项目信息")
        void updateProject_success() throws Exception {
            String body = objectMapper.writeValueAsString(Map.of(
                    "projectName", "更新后的项目名"
            ));
            mockMvc.perform(put("/api/pm/charters/{id}", projectId)
                            .header("Authorization", "Bearer " + pmToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Nested
    @DisplayName("POST /api/pm/charters/{id}/submit")
    class Submit {

        @Test
        @DisplayName("草稿状态提交成功")
        void draftStatus_submitSuccess() throws Exception {
            mockMvc.perform(post("/api/pm/charters/{id}/submit", projectId)
                            .header("Authorization", "Bearer " + pmToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Nested
    @DisplayName("POST /api/pm/charters/{id}/approve")
    class Approve {

        @Test
        @DisplayName("审批中状态审批通过")
        void pendingStatus_approveSuccess() throws Exception {
            // 先提交
            mockMvc.perform(post("/api/pm/charters/{id}/submit", projectId)
                    .header("Authorization", "Bearer " + pmToken));

            String body = objectMapper.writeValueAsString(Map.of(
                    "rejectReason", "同意立项"
            ));
            mockMvc.perform(post("/api/pm/charters/{id}/approve", projectId)
                            .header("Authorization", "Bearer " + pmToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("非审批中状态审批失败")
        void nonPendingStatus_throwsError() throws Exception {
            String body = objectMapper.writeValueAsString(Map.of("rejectReason", "同意"));
            mockMvc.perform(post("/api/pm/charters/{id}/approve", projectId)
                            .header("Authorization", "Bearer " + pmToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }
    }

    @Nested
    @DisplayName("POST /api/pm/charters/{id}/reject")
    class Reject {

        @Test
        @DisplayName("审批中状态驳回成功")
        void pendingStatus_rejectSuccess() throws Exception {
            mockMvc.perform(post("/api/pm/charters/{id}/submit", projectId)
                    .header("Authorization", "Bearer " + pmToken));

            String body = objectMapper.writeValueAsString(Map.of(
                    "rejectReason", "需修改预算"
            ));
            mockMvc.perform(post("/api/pm/charters/{id}/reject", projectId)
                            .header("Authorization", "Bearer " + pmToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Nested
    @DisplayName("DELETE /api/pm/charters/{id}")
    class Delete {

        @Test
        @DisplayName("删除项目")
        void deleteProject_success() throws Exception {
            mockMvc.perform(delete("/api/pm/charters/{id}", projectId)
                            .header("Authorization", "Bearer " + pmToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Nested
    @DisplayName("GET /api/pm/charters/stats")
    class Stats {

        @Test
        @DisplayName("查询 PM 统计信息")
        void queryStats_returnsData() throws Exception {
            mockMvc.perform(get("/api/pm/charters/stats")
                            .header("Authorization", "Bearer " + pmToken)
                            .param("pmId", TestFixtures.PM_USER))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }
}
