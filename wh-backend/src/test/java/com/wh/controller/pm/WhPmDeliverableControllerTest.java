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
@DisplayName("WhPmDeliverableController 集成测试")
class WhPmDeliverableControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthHelper authHelper;
    @Autowired private TestFixtures fixtures;

    private String pmToken;
    private String projectId;

    @BeforeEach
    void setUp() {
        pmToken = authHelper.pmToken();
        WhPmCharter project = fixtures.createApprovedProject("成果物测试项目");
        projectId = project.getId();
    }

    @Test
    @DisplayName("GET /api/pm/deliverables - 无 token 返回 403")
    void list_withoutToken_403() throws Exception {
        mockMvc.perform(get("/api/pm/deliverables"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/pm/deliverables - 分页查询")
    void list_returnsPagedList() throws Exception {
        mockMvc.perform(get("/api/pm/deliverables")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @DisplayName("POST /api/pm/deliverables - 创建交付物")
    void create_validRequest_createsDeliverable() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "测试交付物",
                "projectId", projectId,
                "description", "测试描述",
                "plannedDeliveryDate", "2026-12-31"
        ));
        mockMvc.perform(post("/api/pm/deliverables")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.name").value("测试交付物"));
    }

    @Test
    @DisplayName("GET /api/pm/deliverables/{id} - 查看详情")
    void detail_returnsDetail() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "详情测试", "projectId", projectId, "plannedDeliveryDate", "2026-12-31"));
        String resp = mockMvc.perform(post("/api/pm/deliverables")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(resp).get("data").get("id").asText();

        mockMvc.perform(get("/api/pm/deliverables/{id}", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(id));
    }

    @Test
    @DisplayName("GET /api/pm/deliverables - 按项目筛选")
    void list_filterByProject() throws Exception {
        mockMvc.perform(get("/api/pm/deliverables")
                        .header("Authorization", "Bearer " + pmToken)
                        .param("projectId", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/pm/deliverables - 按关键词搜索")
    void list_searchByKeyword() throws Exception {
        mockMvc.perform(get("/api/pm/deliverables")
                        .header("Authorization", "Bearer " + pmToken)
                        .param("keyword", "交付"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("PUT /api/pm/deliverables/{id} - 更新")
    void update_success() throws Exception {
        String createBody = objectMapper.writeValueAsString(Map.of(
                "name", "更新测试", "projectId", projectId, "plannedDeliveryDate", "2026-12-31"));
        String resp = mockMvc.perform(post("/api/pm/deliverables")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(resp).get("data").get("id").asText();

        String updateBody = objectMapper.writeValueAsString(Map.of(
                "name", "更新后的交付物",
                "projectId", projectId,
                "plannedDeliveryDate", "2026-06-30"
        ));
        mockMvc.perform(put("/api/pm/deliverables/{id}", id)
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("DELETE /api/pm/deliverables/{id} - 删除")
    void delete_success() throws Exception {
        String createBody = objectMapper.writeValueAsString(Map.of(
                "name", "删除测试", "projectId", projectId, "plannedDeliveryDate", "2026-12-31"));
        String resp = mockMvc.perform(post("/api/pm/deliverables")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(resp).get("data").get("id").asText();

        mockMvc.perform(delete("/api/pm/deliverables/{id}", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
