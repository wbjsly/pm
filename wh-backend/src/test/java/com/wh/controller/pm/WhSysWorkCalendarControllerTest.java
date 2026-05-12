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
@DisplayName("WhSysWorkCalendarController 集成测试")
class WhSysWorkCalendarControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthHelper authHelper;

    private String pmToken;

    @BeforeEach
    void setUp() {
        pmToken = authHelper.pmToken();
    }

    @Test
    @DisplayName("GET /api/system/work-calendar - 按年份查询")
    void getByYear_returnsList() throws Exception {
        mockMvc.perform(get("/api/system/work-calendar")
                        .header("Authorization", "Bearer " + pmToken)
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/system/work-calendar/month - 按月查询")
    void getByMonth_returnsMap() throws Exception {
        String genBody = objectMapper.writeValueAsString(Map.of("year", "2026"));
        mockMvc.perform(post("/api/system/work-calendar/generate")
                .header("Authorization", "Bearer " + pmToken)
                .contentType(MediaType.APPLICATION_JSON).content(genBody));

        mockMvc.perform(get("/api/system/work-calendar/month")
                        .header("Authorization", "Bearer " + pmToken)
                        .param("year", "2026")
                        .param("month", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("POST /api/system/work-calendar/generate - 生成年份日历")
    void generateYear_returnsOk() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("year", "2026"));
        mockMvc.perform(post("/api/system/work-calendar/generate")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/system/work-calendar/is-workday - 判断是否工作日")
    void isWorkDay_returnsBoolean() throws Exception {
        mockMvc.perform(get("/api/system/work-calendar/is-workday")
                        .header("Authorization", "Bearer " + pmToken)
                        .param("date", "2026-01-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("POST /api/system/work-calendar - 更新单日")
    void updateDay_returnsOk() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "date", "2026-01-05",
                "isWorkDay", "1",
                "dayType", "工作日"
        ));
        mockMvc.perform(post("/api/system/work-calendar")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }
}
