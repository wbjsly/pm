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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"sqlite", "dev"})
@DisplayName("WhPmWbsElementController 集成测试")
class WhPmWbsElementControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthHelper authHelper;
    @Autowired private TestFixtures fixtures;

    private String pmToken;
    private String projectId;

    @BeforeEach
    void setUp() {
        pmToken = authHelper.pmToken();
        WhPmCharter project = fixtures.createTestProject("WBS测试项目");
        projectId = project.getId();
    }

    @Test
    @DisplayName("GET /api/pm/wbs - 无 token 返回 403")
    void list_withoutToken_403() throws Exception {
        mockMvc.perform(get("/api/pm/wbs").param("projectId", projectId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/pm/wbs - 按 projectId 查询返回列表")
    void list_withProjectId_returnsList() throws Exception {
        mockMvc.perform(get("/api/pm/wbs")
                        .header("Authorization", "Bearer " + pmToken)
                        .param("projectId", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/pm/wbs - 按 status 和 keyword 筛选")
    void list_withFilters_returnsFiltered() throws Exception {
        mockMvc.perform(get("/api/pm/wbs")
                        .header("Authorization", "Bearer " + pmToken)
                        .param("projectId", projectId)
                        .param("status", "PLANNED")
                        .param("keyword", "测试"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("POST /api/pm/wbs - 正常创建根节点")
    void create_rootNode_success() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "projectId", projectId,
                "name", "需求分析",
                "elementType", "TASK"
        ));
        mockMvc.perform(post("/api/pm/wbs")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/pm/wbs/{id} - 不存在的 ID 返回错误")
    void detail_nonexistentId_returnsError() throws Exception {
        mockMvc.perform(get("/api/pm/wbs/{id}", "nonexistent-id")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("POST /api/pm/wbs/{id}/suspend - 挂起不存在的节点返回错误")
    void suspend_nonexistentId_returnsError() throws Exception {
        mockMvc.perform(post("/api/pm/wbs/{id}/suspend", "nonexistent-id")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("POST /api/pm/wbs/{id}/resume - 恢复不存在的节点返回错误")
    void resume_nonexistentId_returnsError() throws Exception {
        mockMvc.perform(post("/api/pm/wbs/{id}/resume", "nonexistent-id")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("POST /api/pm/wbs/{id}/reopen - 重新打开不存在的节点返回错误")
    void reopen_nonexistentId_returnsError() throws Exception {
        mockMvc.perform(post("/api/pm/wbs/{id}/reopen", "nonexistent-id")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("POST /api/pm/wbs/{id}/start - 开始不存在的节点返回错误")
    void start_nonexistentId_returnsError() throws Exception {
        mockMvc.perform(post("/api/pm/wbs/{id}/start", "nonexistent-id")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("POST /api/pm/wbs/{id}/test - 测试不存在的节点返回错误")
    void test_nonexistentId_returnsError() throws Exception {
        mockMvc.perform(post("/api/pm/wbs/{id}/test", "nonexistent-id")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("POST /api/pm/wbs/{id}/complete - 完成不存在的节点返回错误")
    void complete_nonexistentId_returnsError() throws Exception {
        mockMvc.perform(post("/api/pm/wbs/{id}/complete", "nonexistent-id")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("POST /api/pm/wbs/{id}/cancel - 取消不存在的节点返回错误")
    void cancel_nonexistentId_returnsError() throws Exception {
        mockMvc.perform(post("/api/pm/wbs/{id}/cancel", "nonexistent-id")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("GET /api/pm/wbs/{id}/versions - 不存在的 ID 版本历史返回错误")
    void versions_nonexistentId_returnsError() throws Exception {
        mockMvc.perform(get("/api/pm/wbs/{id}/versions", "nonexistent-id")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("POST /api/pm/wbs/import - 导入文件")
    void importWbs_withFile_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "wbs.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[0]);

        mockMvc.perform(multipart("/api/pm/wbs/import")
                        .file(file)
                        .param("projectId", projectId)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/pm/wbs/export - 导出文件")
    void exportWbs_success() throws Exception {
        mockMvc.perform(get("/api/pm/wbs/export")
                        .header("Authorization", "Bearer " + pmToken)
                        .param("projectId", projectId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/pm/wbs/template - 下载模板")
    void downloadTemplate_success() throws Exception {
        mockMvc.perform(get("/api/pm/wbs/template")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk());
    }

    // ═══════════════════════════════════════════════════════
    //  补充：update / delete / 状态流转成功路径
    // ═══════════════════════════════════════════════════════

    private String createWbsNode(String name) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "projectId", projectId, "name", name, "elementType", "TASK"));
        String resp = mockMvc.perform(post("/api/pm/wbs")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resp).get("data").get("id").asText();
    }

    @Test
    @DisplayName("GET /api/pm/wbs/{id} - 存在的节点返回详情")
    void detail_existingId_returnsDetail() throws Exception {
        String id = createWbsNode("详情节点");
        mockMvc.perform(get("/api/pm/wbs/{id}", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(id));
    }

    @Test
    @DisplayName("PUT /api/pm/wbs/{id} - 更新节点成功")
    void update_success() throws Exception {
        String id = createWbsNode("待更新节点");
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "更新后名称", "description", "更新描述"));
        mockMvc.perform(put("/api/pm/wbs/{id}", id)
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/pm/wbs/{id}", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(jsonPath("$.data.name").value("更新后名称"));
    }

    @Test
    @DisplayName("DELETE /api/pm/wbs/{id} - 删除未开始且无子节点的节点成功")
    void delete_success() throws Exception {
        String id = createWbsNode("待删除节点");
        mockMvc.perform(delete("/api/pm/wbs/{id}", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/pm/wbs/{id}", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("POST /api/pm/wbs/{id}/start - 开始节点成功")
    void start_success() throws Exception {
        String id = createWbsNode("开始节点");
        mockMvc.perform(post("/api/pm/wbs/{id}/start", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/pm/wbs/{id}", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(jsonPath("$.data.status").value("IN_DEVELOPMENT"));
    }

    @Test
    @DisplayName("POST suspend/resume - 暂停后恢复成功")
    void suspend_resume_success() throws Exception {
        String id = createWbsNode("暂停恢复节点");
        mockMvc.perform(post("/api/pm/wbs/{id}/start", id)
                        .header("Authorization", "Bearer " + pmToken)).andExpect(status().isOk());
        mockMvc.perform(post("/api/pm/wbs/{id}/suspend", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/pm/wbs/{id}", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));
        mockMvc.perform(post("/api/pm/wbs/{id}/resume", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/pm/wbs/{id}", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(jsonPath("$.data.status").value("IN_DEVELOPMENT"));
    }

    @Test
    @DisplayName("POST test/complete/reopen - 提测完成并重新打开")
    void test_complete_reopen_success() throws Exception {
        String id = createWbsNode("全流程节点");
        mockMvc.perform(post("/api/pm/wbs/{id}/start", id)
                        .header("Authorization", "Bearer " + pmToken)).andExpect(status().isOk());
        mockMvc.perform(post("/api/pm/wbs/{id}/test", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/pm/wbs/{id}", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(jsonPath("$.data.status").value("TESTING"));
        mockMvc.perform(post("/api/pm/wbs/{id}/complete", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/pm/wbs/{id}", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        mockMvc.perform(post("/api/pm/wbs/{id}/reopen", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/pm/wbs/{id}", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(jsonPath("$.data.status").value("NOT_STARTED"));
    }

    @Test
    @DisplayName("POST /api/pm/wbs/{id}/cancel - 取消未开始节点成功")
    void cancel_success() throws Exception {
        String id = createWbsNode("取消节点");
        mockMvc.perform(post("/api/pm/wbs/{id}/cancel", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/pm/wbs/{id}", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("GET /api/pm/wbs/{id}/versions - 存在的节点返回版本历史")
    void versions_existingId_returnsHistory() throws Exception {
        String id = createWbsNode("版本节点");
        mockMvc.perform(get("/api/pm/wbs/{id}/versions", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }
}
