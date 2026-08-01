package com.wh.controller.pm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wh.dao.pm.ErpProductDao;
import com.wh.entity.pm.ErpProduct;
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
@DisplayName("ErpModuleController 集成测试")
class ErpModuleControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthHelper authHelper;
    @Autowired private ErpProductDao productDao;

    private String pmToken;
    private String productId;

    @BeforeEach
    void setUp() {
        pmToken = authHelper.pmToken();
        ErpProduct product = new ErpProduct();
        String uniqueCode = "PROD-CTRL-" + System.currentTimeMillis();
        product.setProductCode(uniqueCode);
        product.setProductName("控制器测试产品");
        product.setStatus("ACTIVE");
        productDao.insert(product);
        productId = product.getId();
    }

    private String createModule(String name) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "productId", productId,
                "moduleName", name,
                "moduleCode", "MOD-" + System.currentTimeMillis(),
                "description", name + "描述",
                "status", "ACTIVE"));
        String resp = mockMvc.perform(post("/api/pm/modules")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resp).get("data").get("id").asText();
    }

    // ══════════════════════════════════════════════════
    //  GET /api/pm/modules
    // ══════════════════════════════════════════════════

    @Test
    @DisplayName("GET /api/pm/modules - 分页查询")
    void list_returnsPagedList() throws Exception {
        mockMvc.perform(get("/api/pm/modules")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @DisplayName("GET /api/pm/modules - 按产品ID筛选")
    void list_filterByProductId() throws Exception {
        mockMvc.perform(get("/api/pm/modules")
                        .header("Authorization", "Bearer " + pmToken)
                        .param("productId", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/pm/modules - 按关键词搜索")
    void list_searchByKeyword() throws Exception {
        mockMvc.perform(get("/api/pm/modules")
                        .header("Authorization", "Bearer " + pmToken)
                        .param("keyword", "模块"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ══════════════════════════════════════════════════
    //  GET /api/pm/modules/by-product/{productId}
    // ══════════════════════════════════════════════════

    @Test
    @DisplayName("GET /api/pm/modules/by-product/{productId} - 按产品查询模块列表")
    void listByProduct_returnsList() throws Exception {
        createModule("ByProduct模块");
        mockMvc.perform(get("/api/pm/modules/by-product/{productId}", productId)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/pm/modules/by-product/{productId} - 不存在的产品返回空数组")
    void listByProduct_nonexistentProduct_returnsEmpty() throws Exception {
        mockMvc.perform(get("/api/pm/modules/by-product/{productId}", "nonexistent-id")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ══════════════════════════════════════════════════
    //  GET /api/pm/modules/{id}
    // ══════════════════════════════════════════════════

    @Test
    @DisplayName("GET /api/pm/modules/{id} - 存在的模块返回详情")
    void detail_existingId_returnsDetail() throws Exception {
        String id = createModule("详情测试");
        mockMvc.perform(get("/api/pm/modules/{id}", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(id));
    }

    @Test
    @DisplayName("GET /api/pm/modules/{id} - 不存在的ID返回404")
    void detail_nonexistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/pm/modules/{id}", "nonexistent-id")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ══════════════════════════════════════════════════
    //  POST /api/pm/modules
    // ══════════════════════════════════════════════════

    @Test
    @DisplayName("POST /api/pm/modules - 正常创建")
    void create_validRequest_createsModule() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "productId", productId,
                "moduleName", "新模块",
                "moduleCode", "MOD-NEW-" + System.currentTimeMillis(),
                "description", "新模块描述",
                "status", "ACTIVE"));
        mockMvc.perform(post("/api/pm/modules")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.moduleName").value("新模块"));
    }

    // ══════════════════════════════════════════════════
    //  PUT /api/pm/modules/{id}
    // ══════════════════════════════════════════════════

    @Test
    @DisplayName("PUT /api/pm/modules/{id} - 更新模块成功")
    void update_validRequest_updatesModule() throws Exception {
        String id = createModule("更新测试");
        String updateBody = objectMapper.writeValueAsString(Map.of(
                "moduleName", "已更新模块",
                "moduleCode", "MOD-UPDATED"));
        mockMvc.perform(put("/api/pm/modules/{id}", id)
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON).content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ══════════════════════════════════════════════════
    //  DELETE /api/pm/modules/{id}
    // ══════════════════════════════════════════════════

    @Test
    @DisplayName("DELETE /api/pm/modules/{id} - 删除模块成功")
    void delete_success() throws Exception {
        String id = createModule("删除测试");
        mockMvc.perform(delete("/api/pm/modules/{id}", id)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("DELETE /api/pm/modules/{id} - 不存在的ID返回404")
    void delete_nonexistentId_returns404() throws Exception {
        mockMvc.perform(delete("/api/pm/modules/{id}", "nonexistent-id")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ══════════════════════════════════════════════════
    //  无 token 访问
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("无 token 访问")
    class NoToken {

        @Test
        @DisplayName("GET /api/pm/modules 返回 403")
        void list_withoutToken_403() throws Exception {
            mockMvc.perform(get("/api/pm/modules"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /api/pm/modules 返回 403")
        void create_withoutToken_403() throws Exception {
            mockMvc.perform(post("/api/pm/modules")
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PUT /api/pm/modules/{id} 返回 403")
        void update_withoutToken_403() throws Exception {
            mockMvc.perform(put("/api/pm/modules/some-id")
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DELETE /api/pm/modules/{id} 返回 403")
        void delete_withoutToken_403() throws Exception {
            mockMvc.perform(delete("/api/pm/modules/some-id"))
                    .andExpect(status().isForbidden());
        }
    }
}
