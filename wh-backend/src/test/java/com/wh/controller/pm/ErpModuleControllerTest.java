package com.wh.controller.pm;

import com.wh.fixtures.AuthHelper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"sqlite", "dev"})
@DisplayName("ErpModuleController 集成测试")
class ErpModuleControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AuthHelper authHelper;

    private String pmToken;

    @BeforeEach
    void setUp() {
        pmToken = authHelper.pmToken();
    }

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
                        .param("productId", "test-product"))
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
}
