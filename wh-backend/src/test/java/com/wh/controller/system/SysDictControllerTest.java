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
@DisplayName("SysDictController 集成测试")
class SysDictControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthHelper authHelper;

    private String adminToken;

    @BeforeEach
    void setUp() {
        adminToken = authHelper.adminToken();
    }

    // ══════════════════════════════════════════════════
    //  GET /api/system/dict/all
    // ══════════════════════════════════════════════════

    @Test
    @DisplayName("GET /api/system/dict/all - 有 token 返回字典数据")
    void getAll_returnsDict() throws Exception {
        mockMvc.perform(get("/api/system/dict/all")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ══════════════════════════════════════════════════
    //  GET /api/system/dict/types
    // ══════════════════════════════════════════════════

    @Test
    @DisplayName("GET /api/system/dict/types - 返回字典类型列表")
    void getTypes_returnsList() throws Exception {
        mockMvc.perform(get("/api/system/dict/types")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ══════════════════════════════════════════════════
    //  GET /api/system/dict/items/{typeCode}
    // ══════════════════════════════════════════════════

    @Test
    @DisplayName("GET /api/system/dict/items/{typeCode} - 存在的类型返回条目列表")
    void getItems_existingTypeCode_returnsItems() throws Exception {
        mockMvc.perform(get("/api/system/dict/items/{typeCode}", "CHARTER_STATUS")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/system/dict/items/{typeCode} - 不存在的类型代码返回空列表")
    void getItems_nonexistentTypeCode_returnsEmpty() throws Exception {
        mockMvc.perform(get("/api/system/dict/items/{typeCode}", "NONEXISTENT_TYPE")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ══════════════════════════════════════════════════
    //  POST /api/system/dict/type
    // ══════════════════════════════════════════════════

    @Test
    @DisplayName("POST /api/system/dict/type - 正常创建字典类型")
    void createType_validRequest_createsType() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "typeCode", "TEST_TYPE_" + System.currentTimeMillis(),
                "typeName", "测试类型",
                "status", "1"
        ));
        mockMvc.perform(post("/api/system/dict/type")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.typeCode").isString());
    }

    // ══════════════════════════════════════════════════
    //  PUT /api/system/dict/type
    // ══════════════════════════════════════════════════

    @Test
    @DisplayName("PUT /api/system/dict/type - 更新字典类型成功")
    void updateType_validRequest_updatesType() throws Exception {
        String typeCode = "UPDATE_TYPE_" + System.currentTimeMillis();
        String createBody = objectMapper.writeValueAsString(Map.of(
                "typeCode", typeCode,
                "typeName", "待更新",
                "status", "1"
        ));
        String resp = mockMvc.perform(post("/api/system/dict/type")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(resp).get("data").get("id").asText();

        String updateBody = objectMapper.writeValueAsString(Map.of(
                "id", id,
                "typeName", "已更新"
        ));
        mockMvc.perform(put("/api/system/dict/type")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.typeName").value("已更新"));
    }

    // ══════════════════════════════════════════════════
    //  DELETE /api/system/dict/type/{id}
    // ══════════════════════════════════════════════════

    @Test
    @DisplayName("DELETE /api/system/dict/type/{id} - 删除字典类型成功")
    void deleteType_deleteType_success() throws Exception {
        String typeCode = "DELETE_TYPE_" + System.currentTimeMillis();
        String createBody = objectMapper.writeValueAsString(Map.of(
                "typeCode", typeCode,
                "typeName", "待删除",
                "status", "1"
        ));
        String resp = mockMvc.perform(post("/api/system/dict/type")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(resp).get("data").get("id").asText();

        mockMvc.perform(delete("/api/system/dict/type/{id}", id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ══════════════════════════════════════════════════
    //  POST /api/system/dict/item
    // ══════════════════════════════════════════════════

    @Test
    @DisplayName("POST /api/system/dict/item - 正常创建字典条目")
    void createItem_validRequest_createsItem() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "typeCode", "CHARTER_STATUS",
                "itemCode", "TEST_ITEM_" + System.currentTimeMillis(),
                "itemLabel", "测试条目",
                "status", "1"
        ));
        mockMvc.perform(post("/api/system/dict/item")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    // ══════════════════════════════════════════════════
    //  PUT /api/system/dict/item
    // ══════════════════════════════════════════════════

    @Test
    @DisplayName("PUT /api/system/dict/item - 更新字典条目成功")
    void updateItem_validRequest_updatesItem() throws Exception {
        String itemCode = "UPDATE_ITEM_" + System.currentTimeMillis();
        String createBody = objectMapper.writeValueAsString(Map.of(
                "typeCode", "CHARTER_STATUS",
                "itemCode", itemCode,
                "itemLabel", "待更新条目",
                "status", "1"
        ));
        String resp = mockMvc.perform(post("/api/system/dict/item")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(resp).get("data").get("id").asText();

        String updateBody = objectMapper.writeValueAsString(Map.of(
                "id", id,
                "itemLabel", "已更新条目"
        ));
        mockMvc.perform(put("/api/system/dict/item")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.itemLabel").value("已更新条目"));
    }

    // ══════════════════════════════════════════════════
    //  DELETE /api/system/dict/item/{id}
    // ══════════════════════════════════════════════════

    @Test
    @DisplayName("DELETE /api/system/dict/item/{id} - 删除字典条目成功")
    void deleteItem_deleteItem_success() throws Exception {
        String itemCode = "DEL_ITEM_" + System.currentTimeMillis();
        String createBody = objectMapper.writeValueAsString(Map.of(
                "typeCode", "CHARTER_STATUS",
                "itemCode", itemCode,
                "itemLabel", "待删除条目",
                "status", "1"
        ));
        String resp = mockMvc.perform(post("/api/system/dict/item")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(resp).get("data").get("id").asText();

        mockMvc.perform(delete("/api/system/dict/item/{id}", id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ══════════════════════════════════════════════════
    //  无 token 访问（写操作）
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("无 token 访问")
    class NoToken {

        @Test
        @DisplayName("POST /api/system/dict/type 返回 403")
        void createType_withoutToken_403() throws Exception {
            mockMvc.perform(post("/api/system/dict/type")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PUT /api/system/dict/type 返回 403")
        void updateType_withoutToken_403() throws Exception {
            mockMvc.perform(put("/api/system/dict/type")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DELETE /api/system/dict/type/{id} 返回 403")
        void deleteType_withoutToken_403() throws Exception {
            mockMvc.perform(delete("/api/system/dict/type/some-id"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /api/system/dict/item 返回 403")
        void createItem_withoutToken_403() throws Exception {
            mockMvc.perform(post("/api/system/dict/item")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PUT /api/system/dict/item 返回 403")
        void updateItem_withoutToken_403() throws Exception {
            mockMvc.perform(put("/api/system/dict/item")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DELETE /api/system/dict/item/{id} 返回 403")
        void deleteItem_withoutToken_403() throws Exception {
            mockMvc.perform(delete("/api/system/dict/item/some-id"))
                    .andExpect(status().isForbidden());
        }
    }

    // ══════════════════════════════════════════════════
    //  错误路径
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("错误路径")
    class ErrorPath {

        @Test
        @DisplayName("DELETE /api/system/dict/type/{id} - 不存在的 ID 返回 200（幂等删除）")
        void deleteType_nonexistentId_returnsOk() throws Exception {
            mockMvc.perform(delete("/api/system/dict/type/{id}", "nonexistent-id")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("DELETE /api/system/dict/item/{id} - 不存在的 ID 返回 200（幂等删除）")
        void deleteItem_nonexistentId_returnsOk() throws Exception {
            mockMvc.perform(delete("/api/system/dict/item/{id}", "nonexistent-id")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }
}
