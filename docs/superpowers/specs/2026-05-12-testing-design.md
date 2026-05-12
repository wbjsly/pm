# 全栈自动化测试设计

## 目标

对整个工程进行全栈自动化测试，覆盖所有接口的所有场景。
- 后端：集成测试（真实 SQLite），覆盖 15 个 Controller ~105 个 API 端点
- 前端：组件测试（Vitest）+ E2E 测试（Playwright），覆盖 28 个页面
- 策略：按测试层级水平推进（后端 → 前端组件 → 前端 E2E），全部模块一次性覆盖

## 后端测试架构

### 工具栈

| 工具 | 用途 |
|------|------|
| JUnit 5 | 测试框架 |
| Spring Boot Test + MockMvc | 集成测试容器 + Controller HTTP 测试 |
| SQLite | 真实数据库（与生产一致） |
| @Transactional | 每个测试自动回滚，数据隔离 |
| spring-security-test | 认证/权限测试支持 |

### 目录结构

```
wh-backend/src/test/java/com/wh/
├── WhApplicationTests.java          # 已有：上下文加载
├── controller/                       # Controller 集成测试（新增）
│   ├── AuthControllerTest.java
│   ├── pm/
│   │   ├── WhPmBudgetControllerTest.java
│   │   ├── WhPmCharterControllerTest.java
│   │   ├── WhPmWbsElementControllerTest.java
│   │   ├── WhPmWorkLogControllerTest.java
│   │   ├── WhPmDeliverableControllerTest.java
│   │   ├── WhPmActualCostControllerTest.java
│   │   ├── WhPmCostWarningControllerTest.java
│   │   ├── ErpModuleControllerTest.java
│   │   ├── ErpProductControllerTest.java
│   │   └── WhSysWorkCalendarControllerTest.java
│   └── system/
│       ├── SysUserControllerTest.java
│       ├── SysRoleControllerTest.java
│       ├── SysDictControllerTest.java
│       └── SysCostQuotaControllerTest.java
├── bo/                               # BO 层测试
│   ├── WhPmWorkLogBoTest.java        # 已有
│   ├── WhPmWorkflowBoTest.java       # 已有
│   └── ...                           # 补充其余 BO 测试
└── fixtures/                         # 测试数据工厂（新增）
    ├── TestFixtures.java
    └── AuthHelper.java
```

### 场景覆盖矩阵（每个端点）

| # | 场景类型 | 说明 |
|---|---------|------|
| 1 | Happy path | 正常请求 200 |
| 2 | Auth missing | 无 token → 401 |
| 3 | Permission | 角色权限不足 → 403 |
| 4 | Validation | 参数校验失败 → 400 |
| 5 | Business rule | 业务规则冲突 → 错误码 |
| 6 | Not found | 资源不存在 → 404 |
| 7 | Edge cases | 空列表、边界值、重复提交 |

### 新增依赖

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

## 前端测试架构

### 工具栈

| 层级 | 工具 | 用途 |
|------|------|------|
| 组件测试 | Vitest + Vue Test Utils + happy-dom | 渲染、交互、状态 |
| API Mock | vitest vi.mock | 拦截 API 返回可控数据 |
| E2E | Playwright | 浏览器真实用户操作 |

### 目录结构

```
wh-frontend/
├── src/__tests__/                    # 组件测试（新增）
│   ├── views/
│   │   ├── login/index.test.js
│   │   ├── dashboard/index.test.js
│   │   ├── pm/budget/*.test.js
│   │   ├── pm/charter/*.test.js
│   │   ├── pm/deliverable/*.test.js
│   │   ├── pm/wbs/*.test.js
│   │   ├── pm/work-hours/*.test.js
│   │   ├── pm/product/index.test.js
│   │   └── system/**/*.test.js
│   └── api/*.test.js
├── e2e/                              # E2E 测试（新增）
│   ├── auth.spec.js
│   ├── charter.spec.js
│   ├── wbs.spec.js
│   ├── work-hours.spec.js
│   ├── budget.spec.js
│   └── deliverable.spec.js
└── vitest.config.js
```

### 组件测试场景（每个页面）

1. 正常渲染（元素可见）
2. 数据加载（列表渲染正确）
3. 表单提交（调用 API，刷新列表）
4. 提交失败（错误消息展示）
5. 权限校验（无权限提示）
6. 空状态（空占位展示）
7. 批量操作（勾选 → 批量 API 调用）

### E2E 核心流程

| # | 流程 | 涉及页面 |
|---|------|---------|
| 1 | 登录 → 仪表盘 → 登出 | login, dashboard |
| 2 | 创建立项 → 提交审批 → 审批通过 | charter/form, charter/detail |
| 3 | 创建 WBS → 分解子任务 → 导入/导出 | wbs/form, wbs/index |
| 4 | 录入工时 → 提交 → 审批 → 驳回 → 重新提交 | work-hours/index, approval |
| 5 | 编制预算 → 升级版本 → 预实对比 | budget/form, upgrade, comparison |
| 6 | 提交交付物 → 审批 → 上传附件 → 确认交付 | deliverable/form, detail |

### 新增依赖

```bash
npm install -D vitest @vue/test-utils happy-dom @playwright/test
```

## 运行命令

```bash
# 后端
mvn test

# 前端组件
npx vitest run

# 前端 E2E（需后端先启动）
npx playwright test
```

## 工作量

| 层级 | 测试文件数 | 预估用例数 |
|------|----------|-----------|
| 后端 Controller Test | 15 | ~150 |
| 后端 BO Test | 13 | ~100 |
| 前端组件 Test | 28 | ~140 |
| 前端 E2E | 8 | ~40 |
| 测试夹具/工具 | 5 | - |
| **合计** | **~70** | **~430** |
