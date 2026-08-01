package com.wh.controller.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wh.fixtures.AuthHelper;
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
@DisplayName("SysCostQuotaController 集成测试")
class SysCostQuotaControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthHelper authHelper;

    private String adminToken;

    @BeforeEach
    void setUp() {
        adminToken = authHelper.adminToken();
    }

    // ══════════════════════════════════════════════════
    //  岗位管理
    // ══════════════════════════════════════════════════

    @Test @DisplayName("GET /api/system/cost-quota/positions - 岗位列表")
    void listPositions_returnsList() throws Exception {
        mockMvc.perform(get("/api/system/cost-quota/positions")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test @DisplayName("POST /api/system/cost-quota/positions - 创建岗位")
    void createPosition_success() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "测试岗位_" + System.currentTimeMillis(), "sortOrder", 99));
        mockMvc.perform(post("/api/system/cost-quota/positions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    @Test @DisplayName("GET /api/system/cost-quota/years - 年份列表")
    void listYears_returnsList() throws Exception {
        mockMvc.perform(get("/api/system/cost-quota/years")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test @DisplayName("POST /api/system/cost-quota/years - 创建年份")
    void createYear_success() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "测试年份_" + System.currentTimeMillis(),
                "startDate", "2026-01-01", "endDate", "2026-12-31"));
        mockMvc.perform(post("/api/system/cost-quota/years")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    @Test @DisplayName("GET /api/system/cost-quota/years/latest/default-start-date - 默认开始日期")
    void getDefaultStartDate_returnsDate() throws Exception {
        mockMvc.perform(get("/api/system/cost-quota/years/latest/default-start-date")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test @DisplayName("GET /api/system/cost-quota/quotas - 查询定额列表")
    void listQuotas_returnsList() throws Exception {
        mockMvc.perform(get("/api/system/cost-quota/quotas")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("yearId", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test @DisplayName("GET /api/system/cost-quota/quotas/current-rate - 当前费率")
    void getCurrentRate_returnsData() throws Exception {
        mockMvc.perform(get("/api/system/cost-quota/quotas/current-rate")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("positionId", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test @DisplayName("GET /api/system/cost-quota/quotas/history - 历史版本")
    void getHistory_returnsData() throws Exception {
        mockMvc.perform(get("/api/system/cost-quota/quotas/history")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("positionId", "nonexistent")
                        .param("yearId", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test @DisplayName("GET /api/system/cost-quota/quotas/compare - 版本对比（不存在ID返回500）")
    void compareVersions_nonexistentId_returnsError() throws Exception {
        mockMvc.perform(get("/api/system/cost-quota/quotas/compare")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("quotaId1", "nonexistent-id-1")
                        .param("quotaId2", "nonexistent-id-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ══════════════════════════════════════════════════
    //  岗位更新/删除
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("岗位更新/删除")
    class PositionUpdateDelete {

        @Test @DisplayName("PUT /api/system/cost-quota/positions/{id} - 更新岗位")
        void updatePosition_success() throws Exception {
            String createBody = objectMapper.writeValueAsString(Map.of("name", "待更新岗位", "sortOrder", 50));
            String resp = mockMvc.perform(post("/api/system/cost-quota/positions")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON).content(createBody))
                    .andReturn().getResponse().getContentAsString();
            String id = objectMapper.readTree(resp).get("data").get("id").asText();

            String updateBody = objectMapper.writeValueAsString(Map.of("name", "已更新岗位", "sortOrder", 60));
            mockMvc.perform(put("/api/system/cost-quota/positions/{id}", id)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON).content(updateBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.name").value("已更新岗位"));
        }

        @Test @DisplayName("DELETE /api/system/cost-quota/positions/{id} - 删除岗位（新建无用户无定额可删除）")
        void deletePosition_success() throws Exception {
            String createBody = objectMapper.writeValueAsString(Map.of("name", "待删除岗位", "sortOrder", 99));
            String resp = mockMvc.perform(post("/api/system/cost-quota/positions")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON).content(createBody))
                    .andReturn().getResponse().getContentAsString();
            String id = objectMapper.readTree(resp).get("data").get("id").asText();

            mockMvc.perform(delete("/api/system/cost-quota/positions/{id}", id)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test @DisplayName("DELETE /api/system/cost-quota/positions/{id} - 不存在返回 404")
        void deletePosition_nonexistent_returns404() throws Exception {
            mockMvc.perform(delete("/api/system/cost-quota/positions/{id}", "nonexistent-id")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(404));
        }
    }

    // ══════════════════════════════════════════════════
    //  年份更新/删除
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("年份更新/删除")
    class YearUpdateDelete {

        @Test @DisplayName("PUT /api/system/cost-quota/years/{id} - 更新年份")
        void updateYear_success() throws Exception {
            String uniqueName = "更新年份_" + System.currentTimeMillis();
            String createBody = objectMapper.writeValueAsString(Map.of(
                    "name", uniqueName, "startDate", "2026-01-01", "endDate", "2026-12-31"));
            String resp = mockMvc.perform(post("/api/system/cost-quota/years")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON).content(createBody))
                    .andReturn().getResponse().getContentAsString();
            String id = objectMapper.readTree(resp).get("data").get("id").asText();

            String updateBody = objectMapper.writeValueAsString(Map.of("name", "已更新年份"));
            mockMvc.perform(put("/api/system/cost-quota/years/{id}", id)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON).content(updateBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test @DisplayName("DELETE /api/system/cost-quota/years/{id} - 删除年份（使用2000年，无前一年复制，可删除）")
        void deleteYear_success() throws Exception {
            String uniqueName = "删除年份_" + System.currentTimeMillis();
            String createBody = objectMapper.writeValueAsString(Map.of(
                    "name", uniqueName, "startDate", "2000-01-01", "endDate", "2000-12-31"));
            String resp = mockMvc.perform(post("/api/system/cost-quota/years")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON).content(createBody))
                    .andReturn().getResponse().getContentAsString();
            String id = objectMapper.readTree(resp).get("data").get("id").asText();

            mockMvc.perform(delete("/api/system/cost-quota/years/{id}", id)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test @DisplayName("DELETE /api/system/cost-quota/years/{id} - 不存在返回 404")
        void deleteYear_nonexistent_returns404() throws Exception {
            mockMvc.perform(delete("/api/system/cost-quota/years/{id}", "nonexistent-id")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(404));
        }
    }

    // ══════════════════════════════════════════════════
    //  定额调价
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("定额调价")
    class QuotaAdjust {

        @Test @DisplayName("POST /api/system/cost-quota/quotas/adjust - 调价成功")
        void adjustQuota_success() throws Exception {
            // 先创建岗位
            String posBody = objectMapper.writeValueAsString(Map.of("name", "调价岗位", "sortOrder", 80));
            String posResp = mockMvc.perform(post("/api/system/cost-quota/positions")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON).content(posBody))
                    .andReturn().getResponse().getContentAsString();
            String positionId = objectMapper.readTree(posResp).get("data").get("id").asText();

            // 先创建年份
            String yearName = "调价年份_" + System.currentTimeMillis();
            String yearBody = objectMapper.writeValueAsString(Map.of(
                    "name", yearName, "startDate", "2026-01-01", "endDate", "2026-12-31"));
            String yearResp = mockMvc.perform(post("/api/system/cost-quota/years")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON).content(yearBody))
                    .andReturn().getResponse().getContentAsString();
            String yearId = objectMapper.readTree(yearResp).get("data").get("id").asText();

            String adjustBody = objectMapper.writeValueAsString(Map.of(
                    "positionId", positionId,
                    "yearId", yearId,
                    "dailyRate", 500,
                    "effectiveDate", "2026-07-01",
                    "changeReason", "测试调价"));
            mockMvc.perform(post("/api/system/cost-quota/quotas/adjust")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON).content(adjustBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").isNotEmpty());
        }

        @Test @DisplayName("POST /api/system/cost-quota/quotas/adjust - 同一日期重复调价返回 400")
        void adjustQuota_duplicateDate_returns400() throws Exception {
            String posBody = objectMapper.writeValueAsString(Map.of("name", "重复调价岗位", "sortOrder", 70));
            String posResp = mockMvc.perform(post("/api/system/cost-quota/positions")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON).content(posBody))
                    .andReturn().getResponse().getContentAsString();
            String positionId = objectMapper.readTree(posResp).get("data").get("id").asText();

            String yearName = "重复调价年份_" + System.currentTimeMillis();
            String yearBody = objectMapper.writeValueAsString(Map.of(
                    "name", yearName, "startDate", "2026-01-01", "endDate", "2026-12-31"));
            String yearResp = mockMvc.perform(post("/api/system/cost-quota/years")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON).content(yearBody))
                    .andReturn().getResponse().getContentAsString();
            String yearId = objectMapper.readTree(yearResp).get("data").get("id").asText();

            String adjustBody = objectMapper.writeValueAsString(Map.of(
                    "positionId", positionId,
                    "yearId", yearId,
                    "dailyRate", 500,
                    "effectiveDate", "2026-08-01",
                    "changeReason", "第一次调价"));
            mockMvc.perform(post("/api/system/cost-quota/quotas/adjust")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON).content(adjustBody));

            mockMvc.perform(post("/api/system/cost-quota/quotas/adjust")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON).content(adjustBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400));
        }
    }

    // ══════════════════════════════════════════════════
    //  无 token 访问
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("无 token 访问 403")
    class NoToken {

        @Test @DisplayName("GET /api/system/cost-quota/positions 返回 403")
        void listPositions_withoutToken_403() throws Exception {
            mockMvc.perform(get("/api/system/cost-quota/positions"))
                    .andExpect(status().isForbidden());
        }

        @Test @DisplayName("POST /api/system/cost-quota/positions 返回 403")
        void createPosition_withoutToken_403() throws Exception {
            mockMvc.perform(post("/api/system/cost-quota/positions")
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test @DisplayName("PUT /api/system/cost-quota/positions/{id} 返回 403")
        void updatePosition_withoutToken_403() throws Exception {
            mockMvc.perform(put("/api/system/cost-quota/positions/some-id")
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test @DisplayName("DELETE /api/system/cost-quota/positions/{id} 返回 403")
        void deletePosition_withoutToken_403() throws Exception {
            mockMvc.perform(delete("/api/system/cost-quota/positions/some-id"))
                    .andExpect(status().isForbidden());
        }

        @Test @DisplayName("POST /api/system/cost-quota/years 返回 403")
        void createYear_withoutToken_403() throws Exception {
            mockMvc.perform(post("/api/system/cost-quota/years")
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test @DisplayName("PUT /api/system/cost-quota/years/{id} 返回 403")
        void updateYear_withoutToken_403() throws Exception {
            mockMvc.perform(put("/api/system/cost-quota/years/some-id")
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test @DisplayName("DELETE /api/system/cost-quota/years/{id} 返回 403")
        void deleteYear_withoutToken_403() throws Exception {
            mockMvc.perform(delete("/api/system/cost-quota/years/some-id"))
                    .andExpect(status().isForbidden());
        }

        @Test @DisplayName("POST /api/system/cost-quota/quotas/adjust 返回 403")
        void adjustQuota_withoutToken_403() throws Exception {
            mockMvc.perform(post("/api/system/cost-quota/quotas/adjust")
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }
    }
}
