package com.wh.controller.pm;

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
@DisplayName("ErpProductController 集成测试")
class ErpProductControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthHelper authHelper;

    private String pmToken;

    @BeforeEach
    void setUp() {
        pmToken = authHelper.pmToken();
    }

    private String createProduct(String productCode) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "productCode", productCode,
                "productName", "测试产品",
                "status", "ACTIVE"));
        String resp = mockMvc.perform(post("/api/pm/products")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resp).get("data").get("id").asText();
    }

    // ══════════════════════════════════════════════════
    //  GET /api/pm/products
    // ══════════════════════════════════════════════════

    @Test @DisplayName("GET /api/pm/products - 分页查询")
    void list_returnsPagedList() throws Exception {
        mockMvc.perform(get("/api/pm/products")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test @DisplayName("GET /api/pm/products - 按关键词搜索")
    void list_searchByKeyword() throws Exception {
        mockMvc.perform(get("/api/pm/products")
                        .header("Authorization", "Bearer " + pmToken)
                        .param("keyword", "测试"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ══════════════════════════════════════════════════
    //  GET /api/pm/products/all
    // ══════════════════════════════════════════════════

    @Test @DisplayName("GET /api/pm/products/all - 返回所有产品")
    void listAll_returnsAll() throws Exception {
        mockMvc.perform(get("/api/pm/products/all")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ══════════════════════════════════════════════════
    //  GET /api/pm/products/{id}
    // ══════════════════════════════════════════════════

    @Test @DisplayName("GET /api/pm/products/{id} - 存在返回详情")
    void detail_existingId_returnsDetail() throws Exception {
        String id = createProduct("PROD-DETAIL-" + System.currentTimeMillis());
        mockMvc.perform(get("/api/pm/products/{id}", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(id));
    }

    @Test @DisplayName("GET /api/pm/products/{id} - 不存在返回404")
    void detail_nonexistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/pm/products/{id}", "nonexistent-id")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ══════════════════════════════════════════════════
    //  POST /api/pm/products
    // ══════════════════════════════════════════════════

    @Test @DisplayName("POST /api/pm/products - 正常创建")
    void create_validRequest_creates() throws Exception {
        String code = "PROD-CREATE-" + System.currentTimeMillis();
        String body = objectMapper.writeValueAsString(Map.of(
                "productCode", code, "productName", "新创建产品", "status", "ACTIVE"));
        mockMvc.perform(post("/api/pm/products")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.productName").value("新创建产品"));
    }

    @Test @DisplayName("POST /api/pm/products - 重复编码返回409")
    void create_duplicateCode_returns409() throws Exception {
        String code = "PROD-DUP-" + System.currentTimeMillis();
        String body = objectMapper.writeValueAsString(Map.of(
                "productCode", code, "productName", "产品A", "status", "ACTIVE"));
        mockMvc.perform(post("/api/pm/products")
                .header("Authorization", "Bearer " + pmToken)
                .contentType(MediaType.APPLICATION_JSON).content(body));
        mockMvc.perform(post("/api/pm/products")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409));
    }

    // ══════════════════════════════════════════════════
    //  PUT /api/pm/products/{id}
    // ══════════════════════════════════════════════════

    @Test @DisplayName("PUT /api/pm/products/{id} - 更新成功")
    void update_validRequest_updates() throws Exception {
        String id = createProduct("PROD-UPDATE-" + System.currentTimeMillis());
        String updateBody = objectMapper.writeValueAsString(Map.of("productName", "已更新产品"));
        mockMvc.perform(put("/api/pm/products/{id}", id)
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON).content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ══════════════════════════════════════════════════
    //  DELETE /api/pm/products/{id}
    // ══════════════════════════════════════════════════

    @Test @DisplayName("DELETE /api/pm/products/{id} - 删除成功")
    void delete_success() throws Exception {
        String id = createProduct("PROD-DEL-" + System.currentTimeMillis());
        mockMvc.perform(delete("/api/pm/products/{id}", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test @DisplayName("DELETE /api/pm/products/{id} - 不存在返回404")
    void delete_nonexistentId_returns404() throws Exception {
        mockMvc.perform(delete("/api/pm/products/{id}", "nonexistent-id")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ══════════════════════════════════════════════════
    //  无 token 访问
    // ══════════════════════════════════════════════════

    @Nested @DisplayName("无 token 访问")
    class NoToken {
        @Test @DisplayName("GET /api/pm/products 返回 403")
        void list_withoutToken_403() throws Exception {
            mockMvc.perform(get("/api/pm/products")).andExpect(status().isForbidden());
        }
        @Test @DisplayName("POST /api/pm/products 返回 403")
        void create_withoutToken_403() throws Exception {
            mockMvc.perform(post("/api/pm/products")
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }
        @Test @DisplayName("PUT /api/pm/products/{id} 返回 403")
        void update_withoutToken_403() throws Exception {
            mockMvc.perform(put("/api/pm/products/some-id")
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }
        @Test @DisplayName("DELETE /api/pm/products/{id} 返回 403")
        void delete_withoutToken_403() throws Exception {
            mockMvc.perform(delete("/api/pm/products/some-id"))
                    .andExpect(status().isForbidden());
        }
    }
}
