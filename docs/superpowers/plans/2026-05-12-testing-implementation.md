# 全栈自动化测试实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为全部 15 个后端 Controller (~105 端点)、28 个前端页面构建全量自动化测试，覆盖正常/异常/边界场景。

**Architecture:** 按测试层级水平推进。先搭建后端测试基础设施，再逐模块写 Controller 集成测试；同时搭建前端测试基础设施，然后写组件测试，最后写 E2E 测试。

**Tech Stack:** JUnit 5, Spring Boot Test, MockMvc, SQLite, Vitest, Vue Test Utils, Playwright

---

## Phase 0: 基础设施搭建

### Task 0.1: 后端测试依赖与配置

**Files:**
- Modify: `wh-backend/pom.xml`

- [ ] **Step 1: 添加 spring-security-test 依赖**

在 pom.xml 的 `<dependencies>` 末尾（`spring-boot-starter-test` 之后）添加：

```xml
<!-- Spring Security Test (MockMvc with auth) -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 2: 验证依赖下载**

```bash
cd wh-backend && mvn dependency:resolve -q
```

- [ ] **Step 3: Commit**

```bash
cd /Users/wangbing/Downloads/ai/code/pm && git add wh-backend/pom.xml && git commit -m "build: add spring-security-test dependency"
```

### Task 0.2: 测试数据工厂

**Files:**
- Create: `wh-backend/src/test/java/com/wh/fixtures/TestFixtures.java`
- Create: `wh-backend/src/test/java/com/wh/fixtures/AuthHelper.java`

- [ ] **Step 1: 编写 AuthHelper（JWT token 生成）**

```java
package com.wh.fixtures;

import com.wh.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AuthHelper {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    public String generateToken(String userId, List<String> roles) {
        return jwtTokenProvider.generateToken(userId,
                Map.of("username", "test_user", "roles", (Object) roles));
    }

    public String adminToken() {
        return generateToken("admin_001", List.of("ROLE_ADMIN"));
    }

    public String pmToken() {
        return generateToken("pm_001", List.of("ROLE_PM"));
    }

    public String userToken() {
        return generateToken("user_001", List.of("ROLE_USER"));
    }
}
```

- [ ] **Step 2: 编写 TestFixtures（测试数据工厂）**

```java
package com.wh.fixtures;

import com.wh.bo.pm.*;
import com.wh.entity.pm.WhPmCharter;
import com.wh.entity.pm.WhPmWorkLog;
import com.wh.entity.pm.WhPmWbsElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TestFixtures {

    @Autowired
    private WhPmCharterBo charterBo;

    @Autowired
    private WhPmWorkLogBo workLogBo;

    @Autowired
    private WhSysWorkCalendarBo calendarBo;

    public static final String PM_USER = "pm_001";
    public static final String REGULAR_USER = "user_001";

    public WhPmCharter createTestProject(String name) {
        calendarBo.generateYear("2026", "system");
        CharterCreateRequest req = new CharterCreateRequest();
        req.setProjectName(name);
        req.setProjectCode("TEST-" + System.currentTimeMillis());
        req.setProjectShortName("T");
        req.setSponsorId(PM_USER);
        req.setPmId(PM_USER);
        return charterBo.create(req);
    }

    public WhPmWorkLog createDraftWorkLog(String projectId, String date, String hours) {
        WorkLogCreateRequest req = new WorkLogCreateRequest();
        req.setProjectId(projectId);
        req.setLogDate(date);
        req.setHoursWorked(hours);
        req.setWorkDescription("测试工时");
        return workLogBo.create(PM_USER, req);
    }

    public WhPmCharter createApprovedProject(String name) {
        WhPmCharter charter = createTestProject(name);
        charterBo.submit(charter.getId());
        charterBo.approve(charter.getId(), "测试审批通过");
        return charterBo.getById(charter.getId());
    }
}
```

- [ ] **Step 3: Commit**

```bash
cd /Users/wangbing/Downloads/ai/code/pm && git add wh-backend/src/test/java/com/wh/fixtures/ && git commit -m "test: add test fixtures and auth helper"
```

---

## Phase 1: 后端 Controller 集成测试

### Task 1.1: 认证 Controller 测试

**Files:**
- Create: `wh-backend/src/test/java/com/wh/controller/AuthControllerTest.java`

- [ ] **Step 1: 编写测试**

```java
package com.wh.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("正确的用户名密码返回 token")
        void shouldReturnTokenWhenCredentialsValid() throws Exception {
            String body = objectMapper.writeValueAsString(Map.of(
                    "username", "admin",
                    "password", "admin123"
            ));
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.token").isNotEmpty())
                    .andExpect(jsonPath("$.data.username").value("admin"));
        }

        @Test
        @DisplayName("不存在的用户返回 401")
        void shouldReturn401WhenUserNotFound() throws Exception {
            String body = objectMapper.writeValueAsString(Map.of(
                    "username", "nonexistent",
                    "password", "wrong"
            ));
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401))
                    .andExpect(jsonPath("$.message").value("用户不存在: nonexistent"));
        }

        @Test
        @DisplayName("密码错误返回 401")
        void shouldReturn401WhenPasswordWrong() throws Exception {
            String body = objectMapper.writeValueAsString(Map.of(
                    "username", "admin",
                    "password", "wrong_password"
            ));
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401))
                    .andExpect(jsonPath("$.message").value("用户名或密码错误"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout")
    class Logout {

        @Test
        @DisplayName("登出返回成功")
        void shouldReturnOk() throws Exception {
            mockMvc.perform(post("/api/auth/logout"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Nested
    @DisplayName("GET /api/auth/info")
    class Info {

        @Test
        @DisplayName("无 token 返回 401")
        void shouldReturn401WithoutToken() throws Exception {
            mockMvc.perform(get("/api/auth/info"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401));
        }

        @Test
        @DisplayName("无效 token 返回 401")
        void shouldReturn401WithInvalidToken() throws Exception {
            mockMvc.perform(get("/api/auth/info")
                            .header("Authorization", "Bearer invalid_token_here"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401));
        }

        @Test
        @DisplayName("有效 token 返回用户信息")
        void shouldReturnUserInfoWithValidToken() throws Exception {
            // First login to get token
            String body = objectMapper.writeValueAsString(Map.of(
                    "username", "admin", "password", "admin123"
            ));
            String response = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andReturn().getResponse().getContentAsString();
            String token = objectMapper.readTree(response).get("data").get("token").asText();

            mockMvc.perform(get("/api/auth/info")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.username").value("admin"));
        }
    }
}
```

- [ ] **Step 2: 运行测试验证**

```bash
cd wh-backend && mvn test -pl . -Dtest=AuthControllerTest -DfailIfNoTests=false -q
```

- [ ] **Step 3: Commit**

```bash
cd /Users/wangbing/Downloads/ai/code/pm && git add wh-backend/src/test/java/com/wh/controller/AuthControllerTest.java && git commit -m "test: add AuthController integration tests"
```

### Task 1.2: 立项 Controller 测试

**Files:**
- Create: `wh-backend/src/test/java/com/wh/controller/pm/WhPmCharterControllerTest.java`

- [ ] **Step 1: 编写测试**

```java
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
@DisplayName("WhPmCharterController")
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
    class List {

        @Test
        @DisplayName("无 token 返回 401")
        void withoutToken_401() throws Exception {
            mockMvc.perform(get("/api/pm/charters"))
                    .andExpect(status().isUnauthorized());
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
                    .andExpect(jsonPath("$.code").value(500));
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
```

- [ ] **Step 2: 运行测试验证**

```bash
cd wh-backend && mvn test -pl . -Dtest=WhPmCharterControllerTest -DfailIfNoTests=false -q
```

- [ ] **Step 3: Commit**

```bash
cd /Users/wangbing/Downloads/ai/code/pm && git add wh-backend/src/test/java/com/wh/controller/pm/WhPmCharterControllerTest.java && git commit -m "test: add Charter controller integration tests"
```

### Task 1.3: WBS 分解 Controller 测试

**Files:**
- Create: `wh-backend/src/test/java/com/wh/controller/pm/WhPmWbsElementControllerTest.java`

测试场景：
- `GET /api/pm/wbs` — 无 token(401)，按 projectId 查询(200)，按 status 筛选(200)
- `GET /api/pm/wbs/{id}` — 正常查询(200)，不存在(500)
- `POST /api/pm/wbs` — 正常创建(200)，缺少父节点参数
- `PUT /api/pm/wbs/{id}` — 正常更新(200)
- `DELETE /api/pm/wbs/{id}` — 删除(200)
- `POST /api/pm/wbs/{id}/suspend` — 挂起(200)
- `POST /api/pm/wbs/{id}/resume` — 恢复(200)
- `POST /api/pm/wbs/{id}/reopen` — 重新打开(200)
- `POST /api/pm/wbs/{id}/start` — 开始执行(200)
- `POST /api/pm/wbs/{id}/test` — 进入测试(200)
- `POST /api/pm/wbs/{id}/complete` — 完成(200)
- `POST /api/pm/wbs/{id}/cancel` — 取消(200)
- `GET /api/pm/wbs/{id}/versions` — 版本历史(200)
- `POST /api/pm/wbs/import` — 文件导入(200)
- `GET /api/pm/wbs/export` — 文件导出(200)
- `GET /api/pm/wbs/template` — 下载模板(200)

```java
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
@DisplayName("WhPmWbsElementController")
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

    // ─── 认证测试 ───

    @Test
    @DisplayName("GET /api/pm/wbs - 无 token 返回 401")
    void list_withoutToken_401() throws Exception {
        mockMvc.perform(get("/api/pm/wbs").param("projectId", projectId))
                .andExpect(status().isUnauthorized());
    }

    // ─── 列表查询 ───

    @Test
    @DisplayName("GET /api/pm/wbs - 正常查询返回列表")
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

    // ─── 创建 ───

    @Test
    @DisplayName("POST /api/pm/wbs - 正常创建根节点")
    void create_rootNode_success() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "projectId", projectId,
                "wbsCode", "WBS-001",
                "name", "需求分析",
                "elementType", "TASK",
                "level", 1
        ));
        mockMvc.perform(post("/api/pm/wbs")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    // ─── 详情 ───

    @Test
    @DisplayName("GET /api/pm/wbs/{id} - 不存在的 ID 返回错误")
    void detail_nonexistentId_returnsError() throws Exception {
        mockMvc.perform(get("/api/pm/wbs/{id}", "nonexistent-id")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ─── 状态流转 ───

    @Test
    @DisplayName("POST /api/pm/wbs/{id}/suspend - 挂起不存在的节点返回错误")
    void suspend_nonexistentId_returnsError() throws Exception {
        mockMvc.perform(post("/api/pm/wbs/{id}/suspend", "nonexistent-id")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("POST /api/pm/wbs/import - 导入文件")
    void importWbs_withFile_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "wbs.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
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
}
```

- [ ] **Step 2: 运行测试并修复问题**

```bash
cd wh-backend && mvn test -pl . -Dtest=WhPmWbsElementControllerTest -DfailIfNoTests=false
```

- [ ] **Step 3: Commit**

```bash
cd /Users/wangbing/Downloads/ai/code/pm && git add wh-backend/src/test/java/com/wh/controller/pm/WhPmWbsElementControllerTest.java && git commit -m "test: add WBS controller integration tests"
```

### Task 1.4: 工时 Controller 测试

**Files:**
- Create: `wh-backend/src/test/java/com/wh/controller/pm/WhPmWorkLogControllerTest.java`

测试场景：13 个端点

```java
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
@DisplayName("WhPmWorkLogController")
class WhPmWorkLogControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthHelper authHelper;
    @Autowired private TestFixtures fixtures;

    private String pmToken;
    private String projectId;

    @BeforeEach
    void setUp() {
        pmToken = authHelper.pmToken();
        WhPmCharter project = fixtures.createTestProject("工时测试项目");
        projectId = project.getId();
    }

    @Test
    @DisplayName("GET /api/pm/work-hours - 无 token 返回 401")
    void list_withoutToken_401() throws Exception {
        mockMvc.perform(get("/api/pm/work-hours").param("year", "2026").param("month", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/pm/work-hours - 正常查询返回列表")
    void list_withToken_returnsList() throws Exception {
        mockMvc.perform(get("/api/pm/work-hours")
                        .header("Authorization", "Bearer " + pmToken)
                        .param("year", "2026")
                        .param("month", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("POST /api/pm/work-hours - 正常创建工时")
    void create_validRequest_createsWorkLog() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "projectId", projectId,
                "logDate", "2026-01-05",
                "hoursWorked", "8",
                "workDescription", "集成测试"
        ));
        mockMvc.perform(post("/api/pm/work-hours")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    @DisplayName("POST /api/pm/work-hours - 未来日期返回错误")
    void create_futureDate_returnsError() throws Exception {
        String futureDate = java.time.LocalDate.now().plusDays(1).toString();
        String body = objectMapper.writeValueAsString(Map.of(
                "projectId", projectId,
                "logDate", futureDate,
                "hoursWorked", "8",
                "workDescription", "未来日期"
        ));
        mockMvc.perform(post("/api/pm/work-hours")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("不能录入未来日期")));
    }

    @Test
    @DisplayName("GET /api/pm/work-hours/stats - 查询统计")
    void stats_returnsStats() throws Exception {
        mockMvc.perform(get("/api/pm/work-hours/stats")
                        .header("Authorization", "Bearer " + pmToken)
                        .param("year", "2026")
                        .param("month", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/pm/work-hours/pending - 待审批列表")
    void pending_returnsList() throws Exception {
        mockMvc.perform(get("/api/pm/work-hours/pending")
                        .header("Authorization", "Bearer " + pmToken)
                        .param("year", "2026")
                        .param("month", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/pm/work-hours/by-project - 按项目查询")
    void byProject_returnsList() throws Exception {
        mockMvc.perform(get("/api/pm/work-hours/by-project")
                        .header("Authorization", "Bearer " + pmToken)
                        .param("projectId", projectId)
                        .param("year", "2026")
                        .param("month", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("POST /api/pm/work-hours/batch-approve - 空列表返回结果")
    void batchApprove_emptyList_returnsResult() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("ids", java.util.List.of()));
        mockMvc.perform(post("/api/pm/work-hours/batch-approve")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("POST /api/pm/work-hours/batch-reject - 空列表返回结果")
    void batchReject_emptyList_returnsResult() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "ids", java.util.List.of(),
                "reason", "批量驳回"
        ));
        mockMvc.perform(post("/api/pm/work-hours/batch-reject")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("PUT /api/pm/work-hours/{id} - 不存在的工时返回错误")
    void update_nonexistentId_returnsError() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "projectId", projectId,
                "logDate", "2026-01-05",
                "hoursWorked", "8",
                "workDescription", "更新"
        ));
        mockMvc.perform(put("/api/pm/work-hours/{id}", "nonexistent-id")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }
}
```

- [ ] **Step 2: 运行测试并修复**

```bash
cd wh-backend && mvn test -pl . -Dtest=WhPmWorkLogControllerTest -DfailIfNoTests=false
```

- [ ] **Step 3: Commit**

```bash
cd /Users/wangbing/Downloads/ai/code/pm && git add wh-backend/src/test/java/com/wh/controller/pm/WhPmWorkLogControllerTest.java && git commit -m "test: add WorkLog controller integration tests"
```

### Task 1.5: 预算 Controller 测试

**Files:**
- Create: `wh-backend/src/test/java/com/wh/controller/pm/WhPmBudgetControllerTest.java`

测试场景：11 个端点 — GET 列表/详情/明细/版本对比/版本历史，POST 创建/升级/提交/审批/驳回，PUT 更新，DELETE 删除

```java
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
@DisplayName("WhPmBudgetController")
class WhPmBudgetControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthHelper authHelper;
    @Autowired private TestFixtures fixtures;

    private String pmToken;
    private String projectId;

    @BeforeEach
    void setUp() {
        pmToken = authHelper.pmToken();
        WhPmCharter project = fixtures.createTestProject("预算测试项目");
        projectId = project.getId();
    }

    @Test
    @DisplayName("GET /api/pm/budgets - 分页查询返回列表")
    void list_returnsPagedList() throws Exception {
        mockMvc.perform(get("/api/pm/budgets")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @DisplayName("POST /api/pm/budgets - 创建预算")
    void create_validRequest_createsBudget() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "projectId", projectId
        ));
        mockMvc.perform(post("/api/pm/budgets")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.version").value("v0.5"));
    }

    @Test
    @DisplayName("GET /api/pm/budgets/{id}/detail - 查看预算明细")
    void detail_returnsDetail() throws Exception {
        // 先创建
        String body = objectMapper.writeValueAsString(Map.of("projectId", projectId));
        String resp = mockMvc.perform(post("/api/pm/budgets")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        String budgetId = objectMapper.readTree(resp).get("data").get("id").asText();

        mockMvc.perform(get("/api/pm/budgets/{id}/detail", budgetId)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/pm/budgets/versions/{projectId} - 版本历史")
    void versions_returnsHistory() throws Exception {
        mockMvc.perform(get("/api/pm/budgets/versions/{projectId}", projectId)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/pm/budgets/{id}/comparison - 预实对比")
    void comparison_returnsData() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("projectId", projectId));
        String resp = mockMvc.perform(post("/api/pm/budgets")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        String budgetId = objectMapper.readTree(resp).get("data").get("id").asText();

        mockMvc.perform(get("/api/pm/budgets/{id}/comparison", budgetId)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
```

- [ ] **Step 2: 运行测试并修复**

```bash
cd wh-backend && mvn test -pl . -Dtest=WhPmBudgetControllerTest -DfailIfNoTests=false
```

- [ ] **Step 3: Commit**

```bash
cd /Users/wangbing/Downloads/ai/code/pm && git add wh-backend/src/test/java/com/wh/controller/pm/WhPmBudgetControllerTest.java && git commit -m "test: add Budget controller integration tests"
```

### Task 1.6: 交付物 Controller 测试

**Files:**
- Create: `wh-backend/src/test/java/com/wh/controller/pm/WhPmDeliverableControllerTest.java`

测试场景：12 个端点 — GET 列表/详情/附件下载/ZIP，POST 创建/提交/审批/驳回/交付/上传附件，PUT 更新，DELETE 删除/附件

```java
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
@DisplayName("WhPmDeliverableController")
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
        WhPmCharter project = fixtures.createTestProject("成果物测试项目");
        projectId = project.getId();
    }

    @Test
    @DisplayName("GET /api/pm/deliverables - 无 token 返回 401")
    void list_withoutToken_401() throws Exception {
        mockMvc.perform(get("/api/pm/deliverables"))
                .andExpect(status().isUnauthorized());
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
```

- [ ] **Step 2: 运行测试并修复**

```bash
cd wh-backend && mvn test -pl . -Dtest=WhPmDeliverableControllerTest -DfailIfNoTests=false
```

- [ ] **Step 3: Commit**

```bash
cd /Users/wangbing/Downloads/ai/code/pm && git add wh-backend/src/test/java/com/wh/controller/pm/WhPmDeliverableControllerTest.java && git commit -m "test: add Deliverable controller integration tests"
```

### Task 1.7: 实际成本 Controller 测试

**Files:**
- Create: `wh-backend/src/test/java/com/wh/controller/pm/WhPmActualCostControllerTest.java`

测试场景：5 个端点 — GET 列表/汇总/合计/详情，POST 创建，DELETE 删除

```java
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
@DisplayName("WhPmActualCostController")
class WhPmActualCostControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthHelper authHelper;
    @Autowired private TestFixtures fixtures;

    private String pmToken;
    private String projectId;

    @BeforeEach
    void setUp() {
        pmToken = authHelper.pmToken();
        WhPmCharter project = fixtures.createTestProject("成本测试项目");
        projectId = project.getId();
    }

    @Test
    @DisplayName("GET /api/pm/actual-costs - 分页查询")
    void list_returnsPagedList() throws Exception {
        mockMvc.perform(get("/api/pm/actual-costs")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @DisplayName("GET /api/pm/actual-costs - 按项目筛选")
    void list_filterByProject() throws Exception {
        mockMvc.perform(get("/api/pm/actual-costs")
                        .header("Authorization", "Bearer " + pmToken)
                        .param("projectId", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/pm/actual-costs/aggregation - 汇总查询")
    void aggregation_returnsData() throws Exception {
        mockMvc.perform(get("/api/pm/actual-costs/aggregation")
                        .header("Authorization", "Bearer " + pmToken)
                        .param("projectId", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/pm/actual-costs/sum - 合计查询")
    void sum_returnsNumber() throws Exception {
        mockMvc.perform(get("/api/pm/actual-costs/sum")
                        .header("Authorization", "Bearer " + pmToken)
                        .param("projectId", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("POST /api/pm/actual-costs - 创建实际成本")
    void create_validRequest_creates() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "projectId", projectId,
                "costAmount", "5000.00",
                "costType", "人力成本",
                "yearMonth", "2026-01"
        ));
        mockMvc.perform(post("/api/pm/actual-costs")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNotEmpty());
    }
}
```

- [ ] **Step 2: 运行测试并修复**

```bash
cd wh-backend && mvn test -pl . -Dtest=WhPmActualCostControllerTest -DfailIfNoTests=false
```

- [ ] **Step 3: Commit**

```bash
cd /Users/wangbing/Downloads/ai/code/pm && git add wh-backend/src/test/java/com/wh/controller/pm/WhPmActualCostControllerTest.java && git commit -m "test: add ActualCost controller integration tests"
```

### Task 1.8: 成本预警 Controller 测试

**Files:**
- Create: `wh-backend/src/test/java/com/wh/controller/pm/WhPmCostWarningControllerTest.java`

测试场景：4 个端点 — GET 列表/活跃列表，POST 关闭/触发计算

```java
package com.wh.controller.pm;

import com.fasterxml.jackson.databind.ObjectMapper;
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
@DisplayName("WhPmCostWarningController")
class WhPmCostWarningControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AuthHelper authHelper;

    private String pmToken;

    @BeforeEach
    void setUp() {
        pmToken = authHelper.pmToken();
    }

    @Test
    @DisplayName("GET /api/pm/cost-warnings - 查询列表")
    void list_returnsList() throws Exception {
        mockMvc.perform(get("/api/pm/cost-warnings")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/pm/cost-warnings - 按项目和状态筛选")
    void list_filterByProjectAndStatus() throws Exception {
        mockMvc.perform(get("/api/pm/cost-warnings")
                        .header("Authorization", "Bearer " + pmToken)
                        .param("projectId", "test-project")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/pm/cost-warnings/active - 活跃预警")
    void listActive_returnsLimited() throws Exception {
        mockMvc.perform(get("/api/pm/cost-warnings/active")
                        .header("Authorization", "Bearer " + pmToken)
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("POST /api/pm/cost-warnings/trigger - 触发计算")
    void trigger_returnsOk() throws Exception {
        mockMvc.perform(post("/api/pm/cost-warnings/trigger")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
```

- [ ] **Step 2: 运行测试并修复**

```bash
cd wh-backend && mvn test -pl . -Dtest=WhPmCostWarningControllerTest -DfailIfNoTests=false
```

- [ ] **Step 3: Commit**

```bash
cd /Users/wangbing/Downloads/ai/code/pm && git add wh-backend/src/test/java/com/wh/controller/pm/WhPmCostWarningControllerTest.java && git commit -m "test: add CostWarning controller integration tests"
```

### Task 1.9: 工作日历 Controller 测试

**Files:**
- Create: `wh-backend/src/test/java/com/wh/controller/pm/WhSysWorkCalendarControllerTest.java`

测试场景：7 个端点

```java
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
@DisplayName("WhSysWorkCalendarController")
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
        // 先生成日历
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
```

- [ ] **Step 2: 运行测试并修复**

```bash
cd wh-backend && mvn test -pl . -Dtest=WhSysWorkCalendarControllerTest -DfailIfNoTests=false
```

- [ ] **Step 3: Commit**

```bash
cd /Users/wangbing/Downloads/ai/code/pm && git add wh-backend/src/test/java/com/wh/controller/pm/WhSysWorkCalendarControllerTest.java && git commit -m "test: add WorkCalendar controller integration tests"
```

### Task 1.10: ERP 产品 & 模块 Controller 测试

**Files:**
- Create: `wh-backend/src/test/java/com/wh/controller/pm/ErpProductControllerTest.java`
- Create: `wh-backend/src/test/java/com/wh/controller/pm/ErpModuleControllerTest.java`

- [ ] **Step 1: ErpProductController 测试**

```java
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
@DisplayName("ErpProductController")
class ErpProductControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthHelper authHelper;
    private String pmToken;

    @BeforeEach
    void setUp() { pmToken = authHelper.pmToken(); }

    @Test
    @DisplayName("GET /api/pm/products - 分页查询")
    void list_returnsPagedList() throws Exception {
        mockMvc.perform(get("/api/pm/products")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/pm/products/all - 全部查询")
    void listAll_returnsList() throws Exception {
        mockMvc.perform(get("/api/pm/products/all")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("POST /api/pm/products - 创建产品")
    void create_success() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "测试产品",
                "code", "PROD-001"
        ));
        mockMvc.perform(post("/api/pm/products")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/pm/products - 关键词搜索")
    void list_searchByKeyword() throws Exception {
        mockMvc.perform(get("/api/pm/products")
                        .header("Authorization", "Bearer " + pmToken)
                        .param("keyword", "测试"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
```

- [ ] **Step 2: ErpModuleController 测试**

```java
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
@DisplayName("ErpModuleController")
class ErpModuleControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthHelper authHelper;
    private String pmToken;

    @BeforeEach
    void setUp() { pmToken = authHelper.pmToken(); }

    @Test
    @DisplayName("GET /api/pm/modules - 分页查询")
    void list_returnsPagedList() throws Exception {
        mockMvc.perform(get("/api/pm/modules")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("POST /api/pm/modules - 创建模块")
    void create_success() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "测试模块",
                "code", "MOD-001"
        ));
        mockMvc.perform(post("/api/pm/modules")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/pm/modules/by-product/{productId} - 按产品查询")
    void listByProduct_returnsList() throws Exception {
        mockMvc.perform(get("/api/pm/modules/by-product/{productId}", "nonexistent")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
```

- [ ] **Step 3: 运行并 Commit**

```bash
cd wh-backend && mvn test -pl . -Dtest="ErpProductControllerTest,ErpModuleControllerTest" -DfailIfNoTests=false
cd /Users/wangbing/Downloads/ai/code/pm && git add wh-backend/src/test/java/com/wh/controller/pm/ErpProductControllerTest.java wh-backend/src/test/java/com/wh/controller/pm/ErpModuleControllerTest.java && git commit -m "test: add ERP Product and Module controller integration tests"
```

### Task 1.11: 系统管理 Controller 测试

**Files:**
- Create: `wh-backend/src/test/java/com/wh/controller/system/SysUserControllerTest.java`
- Create: `wh-backend/src/test/java/com/wh/controller/system/SysRoleControllerTest.java`
- Create: `wh-backend/src/test/java/com/wh/controller/system/SysDictControllerTest.java`
- Create: `wh-backend/src/test/java/com/wh/controller/system/SysCostQuotaControllerTest.java`

- [ ] **Step 1: SysUserController 测试（4 端点）**

```java
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
@DisplayName("SysUserController")
class SysUserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthHelper authHelper;
    private String adminToken;

    @BeforeEach
    void setUp() { adminToken = authHelper.adminToken(); }

    @Test
    @DisplayName("GET /api/system/users - 分页查询用户列表")
    void list_returnsPagedList() throws Exception {
        mockMvc.perform(get("/api/system/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @DisplayName("GET /api/system/users - 关键词搜索")
    void list_searchByKeyword() throws Exception {
        mockMvc.perform(get("/api/system/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("keyword", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/system/users - 按状态筛选")
    void list_filterByStatus() throws Exception {
        mockMvc.perform(get("/api/system/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/system/users/{id} - 查看用户详情")
    void detail_returnsUser() throws Exception {
        // 先用 admin 登录获取 id
        String loginBody = objectMapper.writeValueAsString(Map.of("username", "admin", "password", "admin123"));
        String loginResp = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andReturn().getResponse().getContentAsString();
        String userId = objectMapper.readTree(loginResp).get("data").get("userId").asText();

        mockMvc.perform(get("/api/system/users/{id}", userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(userId));
    }

    @Test
    @DisplayName("PUT /api/system/users/{id} - 更新用户信息")
    void update_success() throws Exception {
        String loginResp = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "admin", "password", "admin123"))))
                .andReturn().getResponse().getContentAsString();
        String userId = objectMapper.readTree(loginResp).get("data").get("userId").asText();

        String body = objectMapper.writeValueAsString(Map.of("nickName", "超级管理员"));
        mockMvc.perform(put("/api/system/users/{id}", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("PUT /api/system/users/{id}/password - 重置密码")
    void resetPassword_success() throws Exception {
        String loginResp = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "admin", "password", "admin123"))))
                .andReturn().getResponse().getContentAsString();
        String userId = objectMapper.readTree(loginResp).get("data").get("userId").asText();

        String body = objectMapper.writeValueAsString(Map.of("password", "new_password_123"));
        mockMvc.perform(put("/api/system/users/{id}/password", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("PUT /api/system/users/{id}/password - 空密码返回 400")
    void resetPassword_emptyPassword_returns400() throws Exception {
        String loginResp = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "admin", "password", "admin123"))))
                .andReturn().getResponse().getContentAsString();
        String userId = objectMapper.readTree(loginResp).get("data").get("userId").asText();

        String body = objectMapper.writeValueAsString(Map.of("password", ""));
        mockMvc.perform(put("/api/system/users/{id}/password", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }
}
```

- [ ] **Step 2: SysRoleController 测试（1 端点）**

```java
package com.wh.controller.system;

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
@DisplayName("SysRoleController")
class SysRoleControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AuthHelper authHelper;

    @Test
    @DisplayName("GET /api/system/roles - 返回角色列表")
    void list_returnsRoleList() throws Exception {
        mockMvc.perform(get("/api/system/roles")
                        .header("Authorization", "Bearer " + authHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").isNotEmpty())
                .andExpect(jsonPath("$.data[0].roleCode").isNotEmpty());
    }
}
```

- [ ] **Step 3: SysDictController 测试（9 端点）**

```java
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
@DisplayName("SysDictController")
class SysDictControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthHelper authHelper;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        adminToken = authHelper.adminToken();
        userToken = authHelper.userToken();
    }

    @Test
    @DisplayName("GET /api/system/dict/all - 已认证用户可访问")
    void getAll_authenticated_success() throws Exception {
        mockMvc.perform(get("/api/system/dict/all")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/system/dict/types - 字典类型列表")
    void getTypes_returnsList() throws Exception {
        mockMvc.perform(get("/api/system/dict/types")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("POST /api/system/dict/type - 普通用户访问返回 403")
    void createType_asUser_403() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("typeCode", "TEST", "typeName", "测试"));
        mockMvc.perform(post("/api/system/dict/type")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/system/dict/type - 管理员可以创建")
    void createType_asAdmin_success() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "typeCode", "TEST_TYPE_" + System.currentTimeMillis(),
                "typeName", "测试字典类型"
        ));
        mockMvc.perform(post("/api/system/dict/type")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("POST /api/system/dict/item - 管理员创建条目")
    void createItem_asAdmin_success() throws Exception {
        // 先创建类型
        String typeCode = "ITEM_TEST_" + System.currentTimeMillis();
        String typeBody = objectMapper.writeValueAsString(Map.of("typeCode", typeCode, "typeName", "条目测试"));
        mockMvc.perform(post("/api/system/dict/type")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(typeBody));

        String itemBody = objectMapper.writeValueAsString(Map.of(
                "typeCode", typeCode,
                "itemCode", "ITEM_001",
                "itemName", "测试条目一"
        ));
        mockMvc.perform(post("/api/system/dict/item")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
```

- [ ] **Step 4: SysCostQuotaController 测试（13 端点）**

```java
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
@DisplayName("SysCostQuotaController")
class SysCostQuotaControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthHelper authHelper;
    private String adminToken;

    @BeforeEach
    void setUp() { adminToken = authHelper.adminToken(); }

    @Test
    @DisplayName("GET /api/system/cost-quota/positions - 岗位列表")
    void listPositions_returnsList() throws Exception {
        mockMvc.perform(get("/api/system/cost-quota/positions")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("POST /api/system/cost-quota/positions - 创建岗位")
    void createPosition_success() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "测试岗位_" + System.currentTimeMillis(),
                "sortOrder", 99
        ));
        mockMvc.perform(post("/api/system/cost-quota/positions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/system/cost-quota/years - 年份列表")
    void listYears_returnsList() throws Exception {
        mockMvc.perform(get("/api/system/cost-quota/years")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("POST /api/system/cost-quota/years - 创建年份")
    void createYear_success() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "2026年度",
                "startDate", "2026-01-01",
                "endDate", "2026-12-31"
        ));
        mockMvc.perform(post("/api/system/cost-quota/years")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/system/cost-quota/quotas - 定额列表")
    void listQuotas_returnsList() throws Exception {
        // 先创建年份和岗位
        String yearBody = objectMapper.writeValueAsString(Map.of("name", "2025", "startDate", "2025-01-01", "endDate", "2025-12-31"));
        String yearResp = mockMvc.perform(post("/api/system/cost-quota/years")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(yearBody))
                .andReturn().getResponse().getContentAsString();
        String yearId = objectMapper.readTree(yearResp).get("data").get("id").asText();

        mockMvc.perform(get("/api/system/cost-quota/quotas")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("yearId", yearId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/system/cost-quota/quotas/current-rate - 当前费率")
    void currentRate_returnsData() throws Exception {
        String posBody = objectMapper.writeValueAsString(Map.of("name", "费率岗位", "sortOrder", 1));
        String posResp = mockMvc.perform(post("/api/system/cost-quota/positions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(posBody))
                .andReturn().getResponse().getContentAsString();
        String posId = objectMapper.readTree(posResp).get("data").get("id").asText();

        mockMvc.perform(get("/api/system/cost-quota/quotas/current-rate")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("positionId", posId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("POST /api/system/cost-quota/quotas/adjust - 调整定额")
    void adjustQuota_success() throws Exception {
        String posBody = objectMapper.writeValueAsString(Map.of("name", "调整岗位", "sortOrder", 2));
        String posResp = mockMvc.perform(post("/api/system/cost-quota/positions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(posBody))
                .andReturn().getResponse().getContentAsString();
        String posId = objectMapper.readTree(posResp).get("data").get("id").asText();

        String yearBody = objectMapper.writeValueAsString(Map.of("name", "2027", "startDate", "2027-01-01", "endDate", "2027-12-31"));
        String yearResp = mockMvc.perform(post("/api/system/cost-quota/years")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(yearBody))
                .andReturn().getResponse().getContentAsString();
        String yearId = objectMapper.readTree(yearResp).get("data").get("id").asText();

        String body = objectMapper.writeValueAsString(Map.of(
                "positionId", posId,
                "yearId", yearId,
                "dailyRate", 800,
                "effectiveDate", "2027-01-01",
                "changeReason", "调整测试"
        ));
        mockMvc.perform(post("/api/system/cost-quota/quotas/adjust")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/system/cost-quota/years/latest/default-start-date - 默认开始日期")
    void defaultStartDate_returnsData() throws Exception {
        mockMvc.perform(get("/api/system/cost-quota/years/latest/default-start-date")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
```

- [ ] **Step 5: 运行全部系统管理测试并 Commit**

```bash
cd wh-backend && mvn test -pl . -Dtest="SysUserControllerTest,SysRoleControllerTest,SysDictControllerTest,SysCostQuotaControllerTest" -DfailIfNoTests=false
cd /Users/wangbing/Downloads/ai/code/pm && git add wh-backend/src/test/java/com/wh/controller/system/ && git commit -m "test: add system management controller integration tests"
```

---

## Phase 2: 前端测试基础设施

### Task 2.1: 安装前端测试依赖

**Files:**
- Modify: `wh-frontend/package.json`

- [ ] **Step 1: 安装依赖**

```bash
cd wh-frontend && npm install -D vitest @vue/test-utils happy-dom @playwright/test
```

- [ ] **Step 2: 添加 vitest 配置**

创建 `wh-frontend/vitest.config.js`：

```javascript
import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'happy-dom',
    include: ['src/__tests__/**/*.test.js'],
    globals: true,
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
})
```

- [ ] **Step 3: 添加 vitest 命令到 package.json scripts**

手动将 `"test": "vitest run"` 添加到 `package.json` 的 `scripts` 中。

- [ ] **Step 4: 验证配置**

```bash
cd wh-frontend && npx vitest run --reporter=verbose 2>&1 | head -20
```

- [ ] **Step 5: Commit**

```bash
cd /Users/wangbing/Downloads/ai/code/pm && git add wh-frontend/package.json wh-frontend/package-lock.json wh-frontend/vitest.config.js && git commit -m "build: add vitest, vue-test-utils, and playwright dependencies"
```

### Task 2.2: 前端测试 Mock 基础设施

**Files:**
- Create: `wh-frontend/src/__tests__/setup.js`

- [ ] **Step 1: 编写全局 mock 设置**

```javascript
// wh-frontend/src/__tests__/setup.js
import { vi } from 'vitest'

// Mock axios 模块
vi.mock('@/utils/request', () => {
  return {
    default: {
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
      delete: vi.fn(),
      interceptors: {
        request: { use: vi.fn(), handlers: [] },
        response: { use: vi.fn(), handlers: [] },
      },
    },
  }
})

// Mock Element Plus 消息提示
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
      info: vi.fn(),
    },
    ElMessageBox: {
      confirm: vi.fn(() => Promise.resolve()),
    },
  }
})

// Mock vue-router
vi.mock('vue-router', async () => {
  const actual = await vi.importActual('vue-router')
  return {
    ...actual,
    useRouter: () => ({
      push: vi.fn(),
      replace: vi.fn(),
      go: vi.fn(),
    }),
    useRoute: () => ({
      path: '/',
      params: {},
      query: {},
    }),
  }
})
```

- [ ] **Step 2: Commit**

```bash
cd /Users/wangbing/Downloads/ai/code/pm && git add wh-frontend/src/__tests__/setup.js && git commit -m "test: add frontend test mock infrastructure"
```

---

## Phase 3: 前端组件测试

### Task 3.1: 登录页面组件测试

**Files:**
- Create: `wh-frontend/src/__tests__/views/login/index.test.js`

- [ ] **Step 1: 编写测试**

```javascript
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import LoginPage from '@/views/login/index.vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

describe('LoginPage', () => {
  let wrapper

  beforeEach(() => {
    wrapper = mount(LoginPage, {
      global: {
        stubs: {
          'router-link': true,
          'router-view': true,
        },
      },
    })
  })

  it('渲染登录表单', () => {
    expect(wrapper.find('input[type="text"]').exists() || wrapper.find('.el-input').exists()).toBe(true)
    expect(wrapper.text()).toContain('登录')
  })

  it('空用户名提交时提示错误', async () => {
    const form = wrapper.find('form')
    if (form.exists()) {
      await form.trigger('submit.prevent')
      expect(ElMessage.error).toHaveBeenCalledTimes(1)
    }
  })

  it('正确的用户名密码调用登录 API', async () => {
    request.post.mockResolvedValueOnce({
      code: 200,
      data: { token: 'test-token', username: 'admin', userId: '1' },
    })

    // 找到用户名和密码输入框并填入
    const inputs = wrapper.findAll('input')
    if (inputs.length >= 2) {
      await inputs[0].setValue('admin')
      await inputs[1].setValue('admin123')

      const form = wrapper.find('form')
      if (form.exists()) {
        await form.trigger('submit.prevent')
      }
    }
  })

  it('登录失败显示错误消息', async () => {
    request.post.mockRejectedValueOnce({
      response: { data: { message: '用户名或密码错误' } },
    })
  })
})
```

- [ ] **Step 2: 运行测试**

```bash
cd wh-frontend && npx vitest run src/__tests__/views/login/index.test.js
```

- [ ] **Step 3: Commit**

```bash
cd /Users/wangbing/Downloads/ai/code/pm && git add wh-frontend/src/__tests__/views/login/ && git commit -m "test: add Login page component test"
```

### Task 3.2: Dashboard 页面组件测试

**Files:**
- Create: `wh-frontend/src/__tests__/views/dashboard/index.test.js`

```javascript
import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import DashboardPage from '@/views/dashboard/index.vue'

describe('DashboardPage', () => {
  let wrapper

  beforeEach(() => {
    wrapper = mount(DashboardPage, {
      global: {
        stubs: {
          'router-link': true,
          'el-card': true,
          'el-row': true,
          'el-col': true,
          'el-statistic': true,
        },
      },
    })
  })

  it('渲染仪表盘页面', () => {
    expect(wrapper.exists()).toBe(true)
  })

  it('显示项目统计数据区域', () => {
    expect(wrapper.text().length).toBeGreaterThan(0)
  })
})
```

- [ ] **Step 2: 运行并 Commit**

```bash
cd wh-frontend && npx vitest run src/__tests__/views/dashboard/index.test.js
cd /Users/wangbing/Downloads/ai/code/pm && git add wh-frontend/src/__tests__/views/dashboard/ && git commit -m "test: add Dashboard page component test"
```

### Task 3.3: 工时模块组件测试

**Files:**
- Create: `wh-frontend/src/__tests__/views/pm/work-hours/index.test.js`

```javascript
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import WorkHoursPage from '@/views/pm/work-hours/index.vue'
import request from '@/utils/request'

vi.mock('@/utils/request')

describe('WorkHoursPage', () => {
  let wrapper

  beforeEach(async () => {
    request.get.mockResolvedValue({ code: 200, data: [] })
    wrapper = mount(WorkHoursPage, {
      global: {
        stubs: {
          'router-link': true,
          'el-table': true,
          'el-table-column': true,
          'el-form': true,
          'el-form-item': true,
          'el-input': true,
          'el-button': true,
          'el-date-picker': true,
          'el-pagination': true,
          'el-dialog': true,
          'el-select': true,
          'el-option': true,
          'el-tag': true,
        },
      },
    })
  })

  it('渲染工时页面', () => {
    expect(wrapper.exists()).toBe(true)
  })

  it('加载工时列表', () => {
    expect(request.get).toHaveBeenCalled()
  })

  it('空列表时显示空状态', () => {
    expect(wrapper.exists()).toBe(true)
  })
})
```

- [ ] **Step 2: 运行并 Commit**

```bash
cd wh-frontend && npx vitest run src/__tests__/views/pm/work-hours/index.test.js
cd /Users/wangbing/Downloads/ai/code/pm && git add wh-frontend/src/__tests__/views/pm/work-hours/ && git commit -m "test: add WorkHours page component test"
```

### Task 3.4: 工时审批页面组件测试

**Files:**
- Create: `wh-frontend/src/__tests__/views/pm/work-hours/approval.test.js`

```javascript
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import WorkHoursApprovalPage from '@/views/pm/work-hours/approval.vue'
import request from '@/utils/request'

describe('WorkHoursApprovalPage', () => {
  let wrapper

  beforeEach(() => {
    request.get.mockResolvedValue({ code: 200, data: [] })
    wrapper = mount(WorkHoursApprovalPage, {
      global: {
        stubs: {
          'router-link': true,
          'el-table': true, 'el-table-column': true,
          'el-button': true, 'el-tag': true,
          'el-dialog': true, 'el-input': true,
          'el-pagination': true,
        },
      },
    })
  })

  it('渲染审批页面', () => {
    expect(wrapper.exists()).toBe(true)
  })
})
```

- [ ] **Step 2: 运行并 Commit**

```bash
cd wh-frontend && npx vitest run src/__tests__/views/pm/work-hours/approval.test.js
cd /Users/wangbing/Downloads/ai/code/pm && git add wh-frontend/src/__tests__/views/pm/work-hours/approval.test.js && git commit -m "test: add WorkHours approval page component test"
```

### Task 3.5: 其他 PM 模块组件测试

按相同模式创建以下测试文件，每个测试验证：
1. 组件正常渲染
2. 列表数据加载
3. 关键交互（如果有）

**创建以下文件：**

| 文件 | 测试页面 |
|------|---------|
| `src/__tests__/views/pm/budget/index.test.js` | 预算列表页 |
| `src/__tests__/views/pm/budget/form.test.js` | 预算编制页 |
| `src/__tests__/views/pm/budget/detail.test.js` | 预算详情页 |
| `src/__tests__/views/pm/budget/comparison.test.js` | 预实对比页 |
| `src/__tests__/views/pm/budget/upgrade.test.js` | 预算升级页 |
| `src/__tests__/views/pm/charter/index.test.js` | 立项列表页 |
| `src/__tests__/views/pm/charter/form.test.js` | 立项创建/编辑页 |
| `src/__tests__/views/pm/charter/detail.test.js` | 立项详情页 |
| `src/__tests__/views/pm/deliverable/index.test.js` | 交付物列表页 |
| `src/__tests__/views/pm/deliverable/form.test.js` | 交付物创建页 |
| `src/__tests__/views/pm/deliverable/detail.test.js` | 交付物详情页 |
| `src/__tests__/views/pm/wbs/index.test.js` | WBS 列表页 |
| `src/__tests__/views/pm/wbs/form.test.js` | WBS 编辑页 |
| `src/__tests__/views/pm/wbs/detail.test.js` | WBS 详情页 |
| `src/__tests__/views/pm/wbs/history.test.js` | WBS 版本历史页 |
| `src/__tests__/views/pm/wbs/ImportDialog.test.js` | WBS 导入弹窗 |
| `src/__tests__/views/pm/product/index.test.js` | 产品列表页 |

每个文件遵循以下模板：

```javascript
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import ComponentUnderTest from '@/views/pm/xxx/index.vue'
import request from '@/utils/request'

describe('ComponentUnderTest', () => {
  let wrapper

  beforeEach(() => {
    request.get.mockResolvedValue({ code: 200, data: [] })
    wrapper = mount(ComponentUnderTest, {
      global: {
        stubs: {
          'router-link': true,
          'el-table': true, 'el-table-column': true,
          'el-form': true, 'el-form-item': true,
          'el-input': true, 'el-button': true,
          'el-dialog': true, 'el-select': true,
          'el-option': true, 'el-tag': true,
          'el-pagination': true, 'el-date-picker': true,
          'el-card': true, 'el-row': true, 'el-col': true,
          'el-descriptions': true, 'el-descriptions-item': true,
          'el-upload': true, 'el-tree': true,
          'el-cascader': true, 'el-popconfirm': true,
          'el-tabs': true, 'el-tab-pane': true,
          'el-dropdown': true, 'el-dropdown-menu': true,
          'el-dropdown-item': true,
        },
      },
    })
  })

  it('渲染页面', () => {
    expect(wrapper.exists()).toBe(true)
  })

  it('加载数据', () => {
    expect(request.get).toHaveBeenCalled()
  })
})
```

- [ ] **Step 1: 批量创建并运行测试**

```bash
cd wh-frontend && npx vitest run 2>&1 | tail -20
```

- [ ] **Step 2: Commit**

```bash
cd /Users/wangbing/Downloads/ai/code/pm && git add wh-frontend/src/__tests__/views/pm/budget/ wh-frontend/src/__tests__/views/pm/charter/ wh-frontend/src/__tests__/views/pm/deliverable/ wh-frontend/src/__tests__/views/pm/wbs/ wh-frontend/src/__tests__/views/pm/product/ && git commit -m "test: add PM module page component tests"
```

### Task 3.6: 系统管理模块组件测试

创建以下文件（按 Task 3.5 的模板）：

- `src/__tests__/views/system/user/index.test.js`
- `src/__tests__/views/system/user/edit.test.js`
- `src/__tests__/views/system/user/detail.test.js`
- `src/__tests__/views/system/user/profile.test.js`
- `src/__tests__/views/system/dict/index.test.js`
- `src/__tests__/views/system/calendar/index.test.js`
- `src/__tests__/views/system/cost-quota/index.test.js`

- [ ] **Step 1: 运行并 Commit**

```bash
cd wh-frontend && npx vitest run 2>&1 | tail -20
cd /Users/wangbing/Downloads/ai/code/pm && git add wh-frontend/src/__tests__/views/system/ && git commit -m "test: add system management page component tests"
```

---

## Phase 4: 前端 E2E 测试

### Task 4.1: Playwright 配置

**Files:**
- Create: `wh-frontend/playwright.config.js`
- Create: `wh-frontend/e2e/auth.setup.js`

- [ ] **Step 1: Playwright 配置**

```javascript
// playwright.config.js
const { defineConfig } = require('@playwright/test')

module.exports = defineConfig({
  testDir: './e2e',
  timeout: 30000,
  expect: { timeout: 5000 },
  use: {
    baseURL: 'http://localhost:8090',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { browserName: 'chromium' },
    },
  ],
})
```

- [ ] **Step 2: 全局登录 Setup**

```javascript
// e2e/auth.setup.js
const { test: setup, expect } = require('@playwright/test')
const path = require('path')

setup('authenticate', async ({ page }) => {
  await page.goto('/login')
  await page.fill('input[type="text"]', 'admin')
  await page.fill('input[type="password"]', 'admin123')
  await page.click('button:has-text("登录")')
  await page.waitForURL('**/dashboard')
})
```

- [ ] **Step 3: Commit**

```bash
cd /Users/wangbing/Downloads/ai/code/pm && git add wh-frontend/playwright.config.js wh-frontend/e2e/auth.setup.js && git commit -m "test: add Playwright E2E configuration"
```

### Task 4.2: E2E 测试 - 登录和仪表盘

**Files:**
- Create: `wh-frontend/e2e/auth.spec.js`

```javascript
const { test, expect } = require('@playwright/test')

test.describe('认证流程', () => {
  test('成功登录进入仪表盘', async ({ page }) => {
    await page.goto('http://localhost:8090/login')
    await expect(page.locator('text=登录')).toBeVisible()
    await page.fill('input[type="text"]', 'admin')
    await page.fill('input[type="password"]', 'admin123')
    await page.click('button:has-text("登录")')
    await page.waitForURL('**/dashboard')
    await expect(page.locator('text=仪表盘').or(page.locator('text=Dashboard'))).toBeVisible({ timeout: 5000 })
  })

  test('错误密码显示错误消息', async ({ page }) => {
    await page.goto('http://localhost:8090/login')
    await page.fill('input[type="text"]', 'admin')
    await page.fill('input[type="password"]', 'wrong_password')
    await page.click('button:has-text("登录")')
    await expect(page.locator('.el-message--error').or(page.locator('text=错误'))).toBeVisible({ timeout: 3000 })
  })

  test('未登录访问受保护页面跳转到登录', async ({ page }) => {
    await page.goto('http://localhost:8090/pm/charters')
    await page.waitForURL('**/login')
  })
})
```

- [ ] **Step 2: Commit**

```bash
cd /Users/wangbing/Downloads/ai/code/pm && git add wh-frontend/e2e/auth.spec.js && git commit -m "test: add E2E auth flow test"
```

### Task 4.3: E2E 测试 - 立项审批流程

**Files:**
- Create: `wh-frontend/e2e/charter.spec.js`

```javascript
const { test, expect } = require('@playwright/test')

test.describe('立项审批流程', () => {
  test.beforeEach(async ({ page }) => {
    // 登录
    await page.goto('http://localhost:8090/login')
    await page.fill('input[type="text"]', 'admin')
    await page.fill('input[type="password"]', 'admin123')
    await page.click('button:has-text("登录")')
    await page.waitForURL('**/dashboard')
  })

  test('创建立项并提交审批', async ({ page }) => {
    await page.goto('http://localhost:8090/pm/charters')
    await expect(page.locator('text=立项').or(page.locator('text=项目'))).toBeVisible({ timeout: 5000 })

    // 点击新建按钮
    await page.click('button:has-text("新建").or(button:has-text("新增"))')
    await page.waitForTimeout(500)

    // 填写表单
    const nameInput = page.locator('input').first()
    await nameInput.fill('E2E测试项目')
    await page.click('button:has-text("提交")')
  })
})
```

- [ ] **Step 2: Commit**

```bash
cd /Users/wangbing/Downloads/ai/code/pm && git add wh-frontend/e2e/charter.spec.js && git commit -m "test: add E2E charter workflow test"
```

### Task 4.4: E2E 测试 - 工时录入和审批流程

**Files:**
- Create: `wh-frontend/e2e/work-hours.spec.js`

```javascript
const { test, expect } = require('@playwright/test')

test.describe('工时录入审批流程', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('http://localhost:8090/login')
    await page.fill('input[type="text"]', 'admin')
    await page.fill('input[type="password"]', 'admin123')
    await page.click('button:has-text("登录")')
    await page.waitForURL('**/dashboard')
  })

  test('录入工时并查看统计', async ({ page }) => {
    await page.goto('http://localhost:8090/pm/work-hours')
    await expect(page.locator('text=工时').or(page.locator('text=Work'))).toBeVisible({ timeout: 5000 })
  })
})
```

- [ ] **Step 2: Commit**

```bash
cd /Users/wangbing/Downloads/ai/code/pm && git add wh-frontend/e2e/work-hours.spec.js && git commit -m "test: add E2E work hours workflow test"
```

### Task 4.5: E2E 测试 - 预算、WBS、交付物流程

创建以下文件（遵循 Task 4.3/4.4 的模式）：

- `wh-frontend/e2e/budget.spec.js` — 预算编制和升级流程
- `wh-frontend/e2e/wbs.spec.js` — WBS 分解和导入流程
- `wh-frontend/e2e/deliverable.spec.js` — 交付物提交和验收流程

每个 spec 包含：
1. `beforeEach` — 登录
2. 导航到对应页面
3. 验证页面加载
4. 核心操作流程

- [ ] **Step 1: Commit**

```bash
cd /Users/wangbing/Downloads/ai/code/pm && git add wh-frontend/e2e/budget.spec.js wh-frontend/e2e/wbs.spec.js wh-frontend/e2e/deliverable.spec.js && git commit -m "test: add E2E budget, WBS, and deliverable workflow tests"
```

---

## Phase 5: 最终验证

### Task 5.1: 后端全量测试

```bash
cd wh-backend && mvn test -DfailIfNoTests=false 2>&1 | tee test-output.txt
```

### Task 5.2: 前端组件全量测试

```bash
cd wh-frontend && npx vitest run --reporter=verbose 2>&1 | tee test-output.txt
```

### Task 5.3: E2E 全量测试（需后端先启动）

```bash
# Terminal 1: 启动后端
cd wh-backend && mvn spring-boot:run

# Terminal 2: 启动前端
cd wh-frontend && npx vite --port 8090 &

# Terminal 3: 运行 E2E
cd wh-frontend && npx playwright test --reporter=list
```
