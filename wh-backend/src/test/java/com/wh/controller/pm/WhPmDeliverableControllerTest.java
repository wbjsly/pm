package com.wh.controller.pm;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wh.dao.pm.WhPmCharterDao;
import com.wh.entity.pm.WhPmCharter;
import com.wh.fixtures.AuthHelper;
import com.wh.fixtures.TestFixtures;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import io.minio.MinioClient;
import io.minio.GetObjectArgs;
import io.minio.PutObjectArgs;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"sqlite", "dev"})
@DisplayName("WhPmDeliverableController 集成测试")
class WhPmDeliverableControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthHelper authHelper;
    @Autowired private TestFixtures fixtures;
    @Autowired private WhPmCharterDao charterDao;

    @MockBean private MinioClient minioClient;

    private String pmToken;
    private String projectId;

    @BeforeEach
    void setUp() {
        pmToken = authHelper.pmToken();
        // 创建草稿项目，然后直接设置状态为 APPROVED（避免 Flowable）
        WhPmCharter project = fixtures.createTestProject("成果物测试项目");
        projectId = project.getId();
        LambdaUpdateWrapper<WhPmCharter> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(WhPmCharter::getId, projectId)
               .set(WhPmCharter::getStatus, "APPROVED");
        charterDao.update(null, wrapper);
    }

    // ─── 辅助：创建交付物 ───

    private String createDeliverable(String name) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", name, "projectId", projectId, "plannedDeliveryDate", "2026-12-31"));
        String resp = mockMvc.perform(post("/api/pm/deliverables")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resp).get("data").get("id").asText();
    }

    // ══════════════════════════════════════════════════
    //  GET /api/pm/deliverables
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/pm/deliverables")
    class List {

        @Test @DisplayName("分页查询")
        void returnsPagedList() throws Exception {
            mockMvc.perform(get("/api/pm/deliverables")
                            .header("Authorization", "Bearer " + pmToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.records").isArray());
        }

        @Test @DisplayName("按项目筛选")
        void filterByProject() throws Exception {
            mockMvc.perform(get("/api/pm/deliverables")
                            .header("Authorization", "Bearer " + pmToken)
                            .param("projectId", projectId))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        }

        @Test @DisplayName("按关键词搜索")
        void searchByKeyword() throws Exception {
            mockMvc.perform(get("/api/pm/deliverables")
                            .header("Authorization", "Bearer " + pmToken)
                            .param("keyword", "交付"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        }

        @Test @DisplayName("按状态筛选")
        void filterByStatus() throws Exception {
            mockMvc.perform(get("/api/pm/deliverables")
                            .header("Authorization", "Bearer " + pmToken)
                            .param("status", "DRAFT"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        }
    }

    // ══════════════════════════════════════════════════
    //  GET /api/pm/deliverables/{id}
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/pm/deliverables/{id}")
    class Detail {

        @Test @DisplayName("存在的交付物返回详情")
        void existingId_returnsDetail() throws Exception {
            String id = createDeliverable("详情测试");
            mockMvc.perform(get("/api/pm/deliverables/{id}", id)
                            .header("Authorization", "Bearer " + pmToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(id));
        }

        @Test @DisplayName("不存在的 ID 返回 404")
        void nonexistentId_returns404() throws Exception {
            mockMvc.perform(get("/api/pm/deliverables/{id}", "nonexistent-id")
                            .header("Authorization", "Bearer " + pmToken))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(404));
        }
    }

    // ══════════════════════════════════════════════════
    //  POST /api/pm/deliverables
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/pm/deliverables")
    class Create {

        @Test @DisplayName("正常创建")
        void validRequest_createsDeliverable() throws Exception {
            String body = objectMapper.writeValueAsString(Map.of(
                    "name", "测试交付物", "projectId", projectId,
                    "description", "测试描述", "plannedDeliveryDate", "2026-12-31"));
            mockMvc.perform(post("/api/pm/deliverables")
                            .header("Authorization", "Bearer " + pmToken)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.name").value("测试交付物"));
        }

        @Test @DisplayName("缺少必填字段 name 返回 500")
        void missingRequiredFields_returnsError() throws Exception {
            String body = objectMapper.writeValueAsString(Map.of("projectId", projectId));
            mockMvc.perform(post("/api/pm/deliverables")
                            .header("Authorization", "Bearer " + pmToken)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }
    }

    // ══════════════════════════════════════════════════
    //  PUT /api/pm/deliverables/{id}
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("PUT /api/pm/deliverables/{id}")
    class Update {

        @Test @DisplayName("更新成功")
        void success() throws Exception {
            String id = createDeliverable("更新测试");
            String updateBody = objectMapper.writeValueAsString(Map.of(
                    "name", "更新后的交付物", "plannedDeliveryDate", "2026-06-30"));
            mockMvc.perform(put("/api/pm/deliverables/{id}", id)
                            .header("Authorization", "Bearer " + pmToken)
                            .contentType(MediaType.APPLICATION_JSON).content(updateBody))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        }

        @Test @DisplayName("不存在的 ID 返回 404")
        void nonexistentId_returns404() throws Exception {
            String updateBody = objectMapper.writeValueAsString(Map.of("name", "不存在"));
            mockMvc.perform(put("/api/pm/deliverables/{id}", "nonexistent-id")
                            .header("Authorization", "Bearer " + pmToken)
                            .contentType(MediaType.APPLICATION_JSON).content(updateBody))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(404));
        }
    }

    // ══════════════════════════════════════════════════
    //  DELETE /api/pm/deliverables/{id}
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /api/pm/deliverables/{id}")
    class Delete {

        @Test @DisplayName("删除成功")
        void success() throws Exception {
            String id = createDeliverable("删除测试");
            mockMvc.perform(delete("/api/pm/deliverables/{id}", id)
                            .header("Authorization", "Bearer " + pmToken))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        }

        @Test @DisplayName("不存在的 ID 返回 404")
        void nonexistentId_returns404() throws Exception {
            mockMvc.perform(delete("/api/pm/deliverables/{id}", "nonexistent-id")
                            .header("Authorization", "Bearer " + pmToken))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(404));
        }
    }

    // ══════════════════════════════════════════════════
    //  无 token 访问
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("无 token 访问")
    class NoToken {

        @Test @DisplayName("GET /api/pm/deliverables 返回 403")
        void list_withoutToken_403() throws Exception {
            mockMvc.perform(get("/api/pm/deliverables")).andExpect(status().isForbidden());
        }

        @Test @DisplayName("POST /api/pm/deliverables 返回 403")
        void create_withoutToken_403() throws Exception {
            mockMvc.perform(post("/api/pm/deliverables")
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test @DisplayName("PUT /api/pm/deliverables/{id} 返回 403")
        void update_withoutToken_403() throws Exception {
            mockMvc.perform(put("/api/pm/deliverables/some-id")
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test @DisplayName("DELETE /api/pm/deliverables/{id} 返回 403")
        void delete_withoutToken_403() throws Exception {
            mockMvc.perform(delete("/api/pm/deliverables/some-id"))
                    .andExpect(status().isForbidden());
        }
    }

    // ══════════════════════════════════════════════════
    //  submit / approve / reject / deliver 工作流端点
    // ══════════════════════════════════════════════════

    @Test
    @DisplayName("POST /{id}/submit - 提交审批")
    void submit_success() throws Exception {
        String id = createDeliverable("提交成果物");
        mockMvc.perform(post("/api/pm/deliverables/{id}/submit", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("POST /{id}/approve - 审批通过")
    void approve_success() throws Exception {
        String id = createDeliverable("审批成果物");
        mockMvc.perform(post("/api/pm/deliverables/{id}/submit", id)
                        .header("Authorization", "Bearer " + pmToken));
        mockMvc.perform(post("/api/pm/deliverables/{id}/approve", id)
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"验收通过\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("POST /{id}/reject - 审批驳回")
    void reject_success() throws Exception {
        String id = createDeliverable("驳回成果物");
        mockMvc.perform(post("/api/pm/deliverables/{id}/submit", id)
                        .header("Authorization", "Bearer " + pmToken));
        mockMvc.perform(post("/api/pm/deliverables/{id}/reject", id)
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rejectReason\":\"需修改\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("POST /{id}/deliver - 标记已交付")
    void deliver_success() throws Exception {
        String id = createDeliverable("交付成果物");
        mockMvc.perform(post("/api/pm/deliverables/{id}/submit", id)
                        .header("Authorization", "Bearer " + pmToken));
        mockMvc.perform(post("/api/pm/deliverables/{id}/approve", id)
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"通过\"}"));
        mockMvc.perform(post("/api/pm/deliverables/{id}/deliver", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ══════════════════════════════════════════════════
    //  附件上传 / 删除 / 下载 / 打包
    // ══════════════════════════════════════════════════

    @Test
    @DisplayName("POST /{id}/attachments - 上传附件")
    void uploadAttachment_success() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
        String id = createDeliverable("附件成果物");
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "pdf-content".getBytes());
        mockMvc.perform(multipart("/api/pm/deliverables/{id}/attachments", id)
                        .file(file)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].fileName").value("report.pdf"));
    }

    @Test
    @DisplayName("DELETE /{id}/attachments/{index} - 删除附件")
    void deleteAttachment_success() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
        String id = createDeliverable("删除附件成果物");
        MockMultipartFile file = new MockMultipartFile(
                "file", "a.txt", "text/plain", "hello".getBytes());
        mockMvc.perform(multipart("/api/pm/deliverables/{id}/attachments", id)
                        .file(file)
                        .header("Authorization", "Bearer " + pmToken));
        mockMvc.perform(delete("/api/pm/deliverables/{id}/attachments/{index}", id, 0)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /{id}/attachments/{index} - 下载单个附件")
    void downloadAttachment_success() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
        io.minio.GetObjectResponse response = new io.minio.GetObjectResponse(
                null, null, null, "obj",
                new java.io.ByteArrayInputStream("content".getBytes()));
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(response);
        String id = createDeliverable("下载附件成果物");
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.txt", "text/plain", "content".getBytes());
        mockMvc.perform(multipart("/api/pm/deliverables/{id}/attachments", id)
                        .file(file)
                        .header("Authorization", "Bearer " + pmToken));
        mockMvc.perform(get("/api/pm/deliverables/{id}/attachments/{index}", id, 0)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("doc.txt")));
    }

    @Test
    @DisplayName("GET /{id}/attachments/zip - 打包下载")
    void downloadZip_success() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
        io.minio.GetObjectResponse response = new io.minio.GetObjectResponse(
                null, null, null, "obj",
                new java.io.ByteArrayInputStream("zip-content".getBytes()));
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(response);
        String id = createDeliverable("打包成果物");
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.txt", "text/plain", "zip-content".getBytes());
        mockMvc.perform(multipart("/api/pm/deliverables/{id}/attachments", id)
                        .file(file)
                        .header("Authorization", "Bearer " + pmToken));
        mockMvc.perform(get("/api/pm/deliverables/{id}/attachments/zip", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/zip"));
    }
}
