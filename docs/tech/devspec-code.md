# WH 管理系统 — 前端/后端代码结构与开发规范

> 项目： WH 管理系统（统一 ERP+CRM+PM+HR 管理平台）
> 分支：`dev`（主开发分支）
> 数据库：SQLite 3（唯一运行时数据库）

---

## 一、项目总览

```
wh/
├── package.json                  # 根目录：npm run dev 同时起前后端
├── scripts/                      # 开发脚本（dev.sh 等）
├── .env.local                    # 本地环境变量（不提交）
├── .claude/                      # AI 辅助开发规则
│   ├── CLAUDE.md                 # 项目级配置说明
│   └── rules/                    # 架构/规范/数据库/测试/基础设施规则
├── wh-backend/                  # Spring Boot 后端
├── wh-frontend/                 # Vue 3 前端
├── deploy/                       # 部署配置
│   ├── docker-compose.yml        # Redis + MinIO + App 容器编排
│   ├── docker-compose.sqlite-db.yml  # 本地 SQLite（嵌入式，无需外部服务）
│   ├── nginx.conf
│   └── init-sql/sqlite/          # SQLite 增量迁移脚本（权威路径）
└── docs/                         # 业务与设计文档
```

### 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 前端 | Vue 3 + Vite + Element Plus + Pinia | Vue 3.4 / Vite 5 / Element Plus 2.6 |
| 后端 | Spring Boot + MyBatis Plus + Flowable | 2.7.18 / 3.5.5 / 6.8.0 |
| 数据库 | SQLite 3 | 嵌入式文件数据库 |
| 连接池 | Alibaba Druid | 1.2.20 |
| 缓存 | Redis | 7.x |
| 对象存储 | MinIO | — |
| API 文档 | Knife4j | 4.3 |
| 认证 | JWT (jjwt) | 0.11.5 |

---

## 二、后端代码结构（wh-backend/）

### 2.1 顶层目录

```
wh-backend/
├── pom.xml                       # Maven 构建配置（Java 11）
├── src/
│   ├── main/
│   │   ├── java/com/wh/
│   │   │   ├── WhApplication.java        # 启动入口
│   │   │   ├── common/                    # 公共层（工具、实体基类、异常）
│   │   │   ├── config/                    # 配置层（Security、Redis、MyBatis 等）
│   │   │   └── module/                    # 领域模块（各业务功能）
│   │   └── resources/
│   │       ├── application.yml            # 主配置（默认 profiles: sqlite,dev）
│   │       ├── application-sqlite.yml     # SQLite 数据源配置
│   │       ├── application-dev.yml        # 开发环境覆盖
│   │       ├── mapper/                    # MyBatis XML 映射文件
│   │       └── db/                        # SQL 脚本（sqlite/ 为权威）
│   └── test/java/com/wh/                 # 单元测试（镜像 main 目录结构）
└── target/                                # 构建输出
```

### 2.2 入口类：WhApplication.java

```java
@SpringBootApplication
@EnableConfigurationProperties(WhCustomerMergeProperties.class)
@EnableTransactionManagement
@EnableScheduling
@EnableAsync
@MapperScan("com.wh.module.**.dao")
public class WhApplication { ... }
```

- 设置系统属性：`journal_mode=WAL`（SQLite WAL 模式提升并发性能）
- 自动扫描所有 `com.wh.module.**.dao` 下的 Mapper 接口
- 启用事务管理、定时任务、异步方法

### 2.3 公共层（com.wh.common.*）

| 子包 | 说明 |
|------|------|
| `entity/` | `BaseEntity` — 所有业务实体的抽象基类 |
| `constant/` | 系统常量（`WhConstants`、`SystemAdminConstants`） |
| `utils/` | 共享工具类（`SecurityUtils`、`ExcelUtils`、`UserDisplayUtils` 等） |
| `request/` | 通用请求 DTO（`PageRequest`、`IdsRequest`） |
| `response/` | 统一响应包装 `R<T>` |
| `exception/` | 自定义异常（`ServiceException`、`ErrorCode`、`GlobalExceptionHandler`） |
| `database/` | 数据库差异处理（`DbLimitClause`） |
| `importing/` | 导入工具 |

#### BaseEntity 核心字段

| 字段           | 类型            | 说明                       |
| ------------ | ------------- | ------------------------ |
| `id`         | String (UUID) | 主键                       |
| `createBy`   | String        | 创建人                      |
| `createDate` | Date          | 创建时间                     |
| `updateBy`   | String        | 更新人                      |
| `updateDate` | Date          | 更新时间                     |
| `remarks`    | String        | 备注                       |
| `delFlag`    | String        | 逻辑删除标志（"0"=正常 / "1"=已删除） |
| `verNo`      | Integer       | 乐观锁版本号                   |
| `sysCode`    | String        | 系统编码                     |

#### 统一响应格式 R<T>

```java
{
    "code": 200,        // 状态码
    "message": "成功",   // 提示信息
    "data": {...},      // 业务数据
    "timestamp": 1234567890  // 时间戳
}
```

### 2.4 配置层（com.wh.config.*）

| 配置类 | 职责 |
|--------|------|
| `MyBatisPlusConfig` | 分页拦截器、乐观锁、动态表名、自动填充 |
| `SecurityConfig` | Spring Security + JWT 配置 |
| `JwtTokenProvider` | JWT 生成/校验 |
| `JwtAuthenticationFilter` | JWT 认证过滤器 |
| `MustChangePasswordFilter` | 强制改密过滤器 |
| `RedisConfig` | RedisTemplate 配置 |
| `Knife4jConfig` | API 文档（Swagger/OpenAPI） |
| `WebMvcConfig` | Web MVC 配置 |
| `OperationLogInterceptor` | 操作日志拦截器 |
| `*SqliteBootstrap` | SQLite 数据库初始化引导 |

### 2.5 领域模块（com.wh.module.*）

每个业务模块遵循统一的分层结构：

```
com.wh.module.<domain>/
├── controller/     # REST API 控制器（对外暴露 /api/...）
├── bo/             # 业务逻辑层（类名以 *Bo 结尾）
├── dao/            # 数据访问层（MyBatis Mapper 接口）
├── entity/         # 实体类（MyBatis Plus 注解）
├── request/        # 请求 DTO
├── response/       # 响应 DTO
├── vo/             # 视图对象（View Object）
├── excel/          # EasyExcel 导入/导出 DTO
├── callback/       # 审批流程回调
├── support/        # 辅助工具类/策略类
└── service/        # 额外服务层（如定时任务）
```

#### 业务模块清单（22+ 个领域）

| 模块 | 说明 |
|------|------|
| `crm/' ｜客户管理管理｜
	├── `bidding/` | 招投标管理 |
	├── `cert/` | 资质/证书管理 |
	├── `contract/` | 销售合同管理 |
	├── `customer/` | 客户与联系人管理 |
	├── `opportunity/` | 销售商机 |
	├── `payment/` | 回款计划与发票 |
	├── `sales/` | 销售与报价 |
	├── `leads/` | 销售线索 |
	├── `product/` | 产品目录 |
｜pm/ ｜项目管理 ｜
	├── common/    | 公共适配器 |
	├── cost/   | 成本管理 |
	├── create/    | 项目创建 |
	├── deliverable/  |  交付物 |
	├── equipment/  | 设备管理 |
	├── milestone/   |  里程碑与回款计划 |
	├── productivity/    | 生产力跟踪 |
	├── project/      |  项目基础 |
	├── risk/            | 风险管理 |
	├── stakeholder/     | 干系人 |
	├── task/       |  任务管理 |
	├── timesheet/      | 工时填报 |
	└── travel/       | 差旅费用 |
｜pur/ | 采购管理｜
｜fin/ |财务管理 |
	├──expense/  | 费用报销 |
    ├──fund/     | 资金周报 |
    ├──receive/ ｜ 回款｜
	├──pay /｜付款｜
	├──store /｜存货｜
｜hr/ | 人力资源管理 |
｜admin/| 行政管理｜
	├──`vehicle/` | 车队/车辆管理 |
	├──`adminoffice/` | 行政办公（名片、用餐、机票申请、设备） |
| `srm/ `| 供应商关系管理｜
| `governance/` | 治理与工作流 |
	├──`system/` | 系统管理（用户、角色、部门、字典） |
	├──`dashboard/` | 仪表盘与报表 |
	├──`report/` | 报表 |
	├──`approval/` | 审批引擎集成（Flowable 核心封装） |
	├──`migration/` | 数据迁移工具 |
	├──`wechat/` | 企业微信集成 |


### 2.6 审批引擎集成（Flowable 6.8.0）

```
com.wh.module.approval/
├── entity/       # WhApprovalInstance, WhApprovalFlow, WhApprovalNode, WhApprovalRecord
├── dao/          # 审批相关 Mapper
├── bo/           # 审批业务逻辑
├── controller/   # 审批 API
├── listener/     # FlowableHrProcessEndListener（流程结束监听）
└── callback/     # ApprovalCompletedCallback（业务回调接口）
```

**审批集成模式：**
1. 业务模块通过 `submitApproval` / `submit` 端点创建 `ApprovalInstance`
2. 各业务模块实现 `callback` 包中的回调接口（如 `ContractApprovalCallback`）
3. 审批流类型作为常量定义并在数据库播种（如 `CONTRACT`、`HR_RESIGNATION`、`ERP_SEAL_APP_*`）
4. Flowable 配置：`database-schema-update: true`，`async-executor-activate: false`

### 2.7 MyBatis Plus 配置

**MyBatisPlusConfig.java 核心功能：**

| 功能 | 说明 |
|------|------|
| 分页 | `PaginationInnerInterceptor`（动态数据库类型解析） |
| 乐观锁 | `OptimisticLockerInnerInterceptor` |
| 动态表名 | `DynamicTableNameInnerInterceptor`（处理长表名映射） |
| 自动填充 | `MetaObjectHandler` 自动填充 `createDate` / `updateDate` |
| 数据库识别 | 支持 SQLite，通过 vendor 检测 |

**application.yml 关键配置：**

```yaml
mybatis-plus:
  mapper-locations: classpath*:mapper/**/*.xml
  type-aliases-package: com.wh.module
  global-config:
    db-config:
      id-type: assign_uuid
      logic-delete-field: delFlag
      logic-delete-value: "1"
      logic-not-delete-value: "0"
  configuration:
    jdbc-type-for-null: VARCHAR   # application-sqlite.yml 中设置
```

### 2.8 资源文件结构

```
src/main/resources/
├── application.yml                # 主配置
├── application-sqlite.yml         # SQLite 数据源
├── application-dev.yml            # 开发环境覆盖
├── mapper/                        # MyBatis XML
│   ├── contract/
│   ├── opportunity/
│   ├── customer/
│   ├── erp/
│   └── ...（按模块组织）
└── db/
    ├── mysql/                     # MySQL 脚本（历史遗留）
    └── sqlite/                    # SQLite 脚本（权威）
```

### 2.9 测试目录结构

```
src/test/java/com/wh/
├── testsupport/                   # 测试辅助（SecurityContextTestHelper）
├── tools/                         # 测试工具（SqliteSqlFileRunner）
├── config/                        # 配置测试（JWT 过滤器合约测试）
├── module/                        # 各模块单元测试
│   ├── contract/support/          # 工具类测试（ContractTaxUtils 等）
│   ├── payment/support/           # 计算器测试（PaymentPlanDateCalculator）
│   ├── product/support/           # 策略测试（SoftwareProductCatalogPolicy）
│   ├── erp/pm/                    # PM 控制器/BO 测试
│   └── hr/social/                 # 社保相关测试
└── common/utils/                  # 公共工具测试
```

测试框架：JUnit 5 + Mockito（含 `mockito-inline` 静态 Mock）
控制器测试：使用 `MockMvc`（`*WebMvcTest` 模式）

### 2.10 关键依赖（pom.xml）

| 类别 | 依赖 | 版本 |
|------|------|------|
| **Spring Boot** | spring-boot-starter-web, security, data-redis, validation, aop | 2.7.18 |
| **数据库** | MyBatis Plus, Druid, ojdbc8, orai18n | 3.5.5 / 1.2.20 |
| **工作流** | Flowable Spring Boot Starter Process | 6.8.0 |
| **API 文档** | Knife4j OpenAPI2 | 4.3.0 |
| **安全** | JWT (jjwt) | 0.11.5 |
| **存储** | MinIO | 8.5.7 |
| **Excel** | EasyExcel | 3.3.3 |
| **工具** | Hutool, Apache Commons | 5.8.22 |
| **分布式锁** | ShedLock | 4.48.0 |
| **PDF** | Apache PDFBox | 2.0.30 |

---

## 三、前端代码结构（wh-frontend/）

### 3.1 顶层目录

```
wh-frontend/
├── index.html                    # HTML 入口
├── package.json                  # 依赖与脚本
├── vite.config.js               # Vite 构建配置
├── playwright.config.ts         # E2E 测试配置
├── .env.development             # 开发环境变量
├── public/                      # 静态资源（登录背景、Logo）
├── e2e/                         # Playwright E2E 测试
│   ├── helpers/wh-auth.ts
│   └── *.spec.ts
└── src/
    ├── main.js                  # 应用启动入口
    ├── App.vue                  # 根组件（仅 router-view）
    ├── api/                     # API 调用层（36 个 JS 文件）
    ├── bpmn/                    # BPMN 工作流引擎集成
    ├── components/              # 可复用组件
    ├── composables/             # Vue 3 组合式函数
    ├── config/                  # 路由配置与模块定义
    ├── constants/               # 业务常量（13 个文件）
    ├── layout/                  # 布局组件（侧边栏 + 头部）
    ├── router/                  # Vue Router 配置
    ├── store/                   # Pinia 状态管理
    ├── styles/                  # 全局样式
    ├── utils/                   # 工具函数
    └── views/                   # 页面组件（60+ 目录，132 个 Vue 文件）
```

### 3.2 应用启动流程（main.js）

```
index.html → main.js → App.vue → Layout → router-view → 具体页面
```

**main.js 启动序列：**
1. `createApp(App)` 创建 Vue 实例
2. 注册全局组件：`WhTableRowMore`
3. 注册所有 Element Plus 图标
4. 安装插件：Pinia、Router、Element Plus（zh-CN 本地化）
5. 挂载到 `#app`

### 3.3 路由系统（src/router/index.js）

**技术：** Vue Router 4 + `createWebHistory`

**路由结构：**

```
/router/index.js（836 行，扁平化嵌套数组）
├── 认证路由：/login, /force-change-password
├── 仪表盘：/dashboard
├── 系统：/system/*（角色、字典、日志、工作流）
└── 公开：/public/interview-apply
```

**路由构建模式：**

| 辅助函数 | 来源文件 | 用途 |
|----------|----------|------|
| `buildErpRouteChildren()` | `config/erp-modules.js` | 构建 ERP 子路由 |
| `buildHrRouteChildren()` | `config/hr-modules.js` | 构建 HR 子路由 |
| `buildAdminFleetProcurementInventoryChildren()` | 共享 ERP 模块 | 行政车队采购/库存路由 |

**权限控制：**
- 路由 `meta.perm` — 单个权限标识
- 路由 `meta.permAny` — 多权限 OR 逻辑
- 由 `src/utils/permission.js` 的 `routeMetaAllowsAccess()` 函数校验
- 权限数据来自 `GET /auth/info` 接口

**路由守卫：**
1. Token 校验（无 token → 跳转 /login）
2. 获取用户信息（首次加载时）
3. 权限检查（`routeMetaAllowsAccess()`）
4. 强制改密检查 → 跳转 /force-change-password

**重要：** 侧边栏不使用 `el-menu` 的 `router` 属性，而是通过 `@select` 事件调用 `router.push`。

### 3.4 API 调用层（src/api/）

**模式：** 按业务域拆分文件，每个文件导出一组 API 函数

```javascript
// 示例：auth.js
import request from '@/utils/request'

export function loginApi(data) {
  return request({ url: '/auth/login', method: 'post', data })
}
```

**API 模块清单（36 个文件）：**

| 文件 | 对应后端模块 |
|------|-------------|
| `auth.js` | 认证（登录、登出、用户信息） |

**Base URL：** 所有 API 路径以 `/api` 为前缀，由 Vite 代理转发到后端。

### 3.5 HTTP 客户端（src/utils/request.js）

基于 Axios，配置了请求/响应拦截器：

| 拦截器 | 功能 |
|--------|------|
| 请求拦截器 | 自动附加 JWT Token（从 localStorage 读取 `wh_token`） |
| 响应拦截器 | 统一处理 401/403/404/500 错误，使用 Element Plus Message 提示 |

### 3.6 状态管理（src/store/）

**技术：** Pinia（Vue 3 官方状态管理）

**单一 Store：user.js**

```javascript
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('wh_token') || '',
    userInfo: {},
    permissions: [],
    roles: [],
  }),
  getters: {
    isLoggedIn: state => !!state.token,
    userName: state => state.userInfo.realName || state.userInfo.username || '',
    avatar: state => state.userInfo.avatar || '',
  },
  actions: {
    async login(loginForm) { ... },
    async getUserInfo() { ... },
    async logout() { ... },
  }
})
```

Token 持久化到 `localStorage`，用户信息/权限从 `GET /auth/info` 获取。

### 3.7 页面组件（src/views/）

**按业务域组织的目录结构：**

| 目录 | 说明 |
|------|------|
| `login/` | 登录页 |
| `dashboard/` | 首页仪表盘 |
| `customer/` | 客户管理（列表 + 详情，**UI 参考标准**）, 联系人目录、跟进记录 |
| `system/` | 角色、用户、字典、日志、工作流管理 |

**页面模式：**
- 主入口使用 `index.vue`
- 子功能使用描述性名称（`list.vue`、`detail.vue`、`apply.vue`、`manage.vue`）
- 复杂模块使用布局包装器（如 `PmLayout.vue`、`TravelLayout.vue`）

### 3.8 可复用组件（src/components/）

**策略：** 最小化自定义组件库，大量使用 Element Plus 原生组件 + SCSS 定制样式

| 组件 | 说明 |
|------|------|
| `wh/WhTableRowMore.vue` | 表格行操作下拉菜单（全局注册） |

### 3.9 组合式函数（src/composables/）

| 文件 | 说明 |
|------|------|
| `useLocalTable.js` | 本地表格数据管理（排序、筛选、分页） |

### 3.10 BPMN 工作流引擎（src/bpmn/）

```
bpmn/
├── flowable/              # Flowable BPMN 建模器集成
└── normalizeFlowableModdle.js  # Flowable 模型规范化
```

### 3.11 业务常量（src/constants/）

| 文件 | 说明 |
|------|------|

### 3.12 工具函数（src/utils/）

| 文件 | 说明 |
|------|------|
| `request.js` | Axios HTTP 客户端（核心） |
| `permission.js` | 权限校验工具（`routeMetaAllowsAccess`） |
| `excel.js` | Excel 导出功能 |
| `idCard.js` | 身份证号校验 |
| `social-fund-month.js` | 社保月份计算 |
| `inventoryQtyFormat.js` | 库存数量格式化 |
| `inferRegionFromCity.js` | 城市推断省份 |
| `parseCustomerAddressLocal.js` | 客户地址解析 |
| `wecom.js` | 企业微信集成 |
| `ctrip-biz-travel-jump.js` | 携程商旅跳转 |
| `flight-apply-display.js` | 机票申请展示格式化 |
| `mojibake.js` | 字符编码修复 |

### 3.13 样式架构（src/styles/index.scss）

**策略：** 单一 SCSS 文件（约 1000 行），大量使用 CSS 自定义属性

**设计令牌：**

```scss
--wh-brand: #0096e1;
--wh-bg: #f8fafc;
--wh-panel: #ffffff;
--wh-border: #e2e8f0;
--wh-text: #0f172a;
--wh-sidebar: #ffffff;
--wh-radius-md: 16px;
--wh-shadow-md: 0 12px 32px rgba(15, 23, 42, 0.08);
```

**常用工具类：**

| 类名模式 | 说明 |
|----------|------|
| `.page-container` | 白色卡片容器 |
| `.wh-page-toolbar` | 列表页工具栏 |
| `.wh-page-toolbar--*` | 工具栏变体 |
| `.wh-form-grid-2` | 两列表单布局 |
| `.section-card` | 内容子卡片 |
| `.wh-table` / `.wh-table--fill` | 表格样式 |
| `.wh-cell-ellipsis` | 单元格省略 |
| `--wh-layout-content-pad-*` | 内容区 padding |
| `--wh-layout-header-height` | 头部高度 |

**响应式断点：**

| 断点 | 范围 |
|------|------|
| Desktop | > 1200px |
| Tablet | 992px - 1200px |
| Mobile | < 992px |

### 3.14 关键配置

**vite.config.js：**
- Vue 3 插件
- Element Plus 组件自动导入
- 代理：`/api` → `http://127.0.0.1:8080`（可通过环境变量配置）
- HMR 配置
- SCSS modern compiler API
- 路径别名：`@` → `src/`

**package.json 核心依赖：**

| 依赖 | 版本 |
|------|------|
| Vue | 3.4+ |
| Vue Router | 4.3+ |
| Pinia | 2.1+ |
| Element Plus | 2.6+ |
| Vite | 5.4+ |
| Axios | 1.7+ |
| ECharts | 5.6+ |
| BPMN-JS | 17.11+ |
| Playwright | 1.49+ |

---

## 四、前后端关系与数据流

### 4.1 请求-响应链路

```
用户操作（浏览器）
    ↓
Vue 页面组件（src/views/）
    ↓
API 函数（src/api/xxx.js）
    ↓
Axios 拦截器（src/utils/request.js） — 附加 JWT Token
    ↓
Vite Dev Server 代理（/api → http://127.0.0.1:8080）
    ↓
Spring Boot JwtAuthenticationFilter — 验证 Token
    ↓
Controller（com.wh.module.xxx.controller）
    ↓
BO（com.wh.module.xxx.bo） — 业务逻辑
    ↓
DAO（com.wh.module.xxx.dao） — MyBatis Mapper
    ↓
SQLite 数据库
    ↓
Entity → Response/VO DTO → R<T> 包装
    ↓
返回前端 → Axios 响应拦截器 → 页面更新
```

### 4.2 命名映射

| 层级 | 命名规范 | 示例 |
|------|----------|------|
| 前端 API 参数 | camelCase | `contractId`, `customerName` |
| 后端 Request/Response | camelCase（Java 字段） | `contractId`, `customerName` |
| 后端 Entity | camelCase（Java 字段） | `contractId`, `customerName` |
| SQLite 列名 | UPPER_SNAKE_CASE | `CONTRACT_ID`, `CUSTOMER_NAME` |
| Java 类名 | PascalCase | `WhContractController` |
| Vue 组件名 | PascalCase / kebab-case | `CustomerDetail.vue` / `customer-detail.vue` |

MyBatis Plus 自动处理 camelCase ↔ UPPER_SNAKE_CASE 映射。手写 SQL 必须使用 SQLite 列名。

### 4.3 新增功能全链路步骤

以"新增一个业务模块"为例：

1. **后端 DDL** — 在 `deploy/init-sql/sqlite/` 新增迁移脚本
2. **后端 Entity** — `com.wh.module.xxx.entity.WhXxx`（继承 `BaseEntity`）
3. **后端 DAO** — `com.wh.module.xxx.dao.WhXxxDao`（继承 `BaseMapper`）
4. **后端 XML** — `resources/mapper/xxx/WhXxxDao.xml`（复杂查询）
5. **后端 BO** — `com.wh.module.xxx.bo.WhXxxBo`（业务逻辑）
6. **后端 Controller** — `com.wh.module.xxx.controller.WhXxxController`（REST API）
7. **后端 Request/Response** — DTO 类
8. **前端 API** — `src/api/xxx.js` 新增 API 函数
9. **前端路由** — `src/router/index.js` 或 `config/xxx-modules.js` 新增路由
10. **前端页面** — `src/views/xxx/index.vue`
11. **前端权限** — `deploy/init-sql/sqlite/` 添加 `sys_permission` 种子数据
12. **前端权限标注** — 路由 `meta.perm` 设置权限标识

### 4.4 审批流程集成链路

```
业务页面提交审批
    ↓
前端 API → POST /api/xxx/submitApproval
    ↓
Controller → BO（创建 ApprovalInstance）
    ↓
Approval BO → Flowable 引擎启动流程
    ↓
审批人操作（通过/驳回）
    ↓
Flowable 流程结束事件
    ↓
FlowableHrProcessEndListener 触发
    ↓
业务模块 callback 实现类执行后处理
    ↓
更新业务状态
```

---

## 五、开发规范

### 5.1 Git 分支管理

| 分支类型 | 命名格式 | 用途 |
|----------|----------|------|
| 主分支 | `dev` | 持续集成分支 |
| 功能分支 | `feature/{issue-id}-{short-description}` | 新功能开发 |
| 修复分支 | `bugfix/{issue-id}-{description}` | 缺陷修复 |
| 发布分支 | `release/v{version}` | 版本发布 |

- PR 必须通过 CI 检查 + 至少一位 Reviewer 审批
- 使用 **Conventional Commits** 规范

### 5.2 Commit 规范

| 类型 | 说明 | 示例 |
|------|------|------|
| `feat:` | 新功能 | `feat: 合同管理新增税率字段` |
| `fix:` | 缺陷修复 | `fix(crm,sqlite): 商机导入列迁移` |
| `refactor:` | 重构 | `refactor: 审批回调逻辑提取` |
| `perf:` | 性能优化 | `perf: 列表查询添加索引` |
| `test:` | 测试 | `test: 添加合同计算器单元测试` |
| `docs:` | 文档 | `docs: 补充审批流程说明` |
| `chore:` | 构建/配置 | `chore: 更新 Docker Compose 配置` |

### 5.3 数据删除规范（严格）

**所有业务数据删除一律为逻辑删除。**

- 通过 `DEL_FLAG`（`CHAR(1)`：`"0"` 正常 / `"1"` 已删除）标记
- 同时维护 `UPDATE_BY` / `UPDATE_DATE` 审计字段
- **禁止**在业务代码中对业务主表、业务明细表执行 `DELETE` 物理删行
- 列表、详情、统计与权限查询**默认过滤** `del_flag=0`

**例外情况（需单独评审）：**
- 运维级数据清理
- 法规要求的不可逆数据销毁
- 非业务主数据（临时会话表、幂等去重缓存表）

### 5.4 分层调用规范

```
Controller → BO → DAO → Entity
```

- **禁止跨层调用**：Controller 不能直接调用 DAO，BO 不能直接操作 Entity 以外的包
- 业务边界在 Service（BO）层
- 所有业务方法必须有对应的单元测试
- 跨服务调用必须通过 Feign Client，配置熔断降级

### 5.5 命名规范

| 对象 | 规范 | 示例 |
|------|------|------|
| 类名 | PascalCase，与文件名一致 | `WhContractBo.java` |
| 方法/变量 | camelCase | `getContractById()` |
| 常量 | UPPER_SNAKE_CASE | `CONTRACT_STATUS_APPROVED` |
| 包名 | 全小写，点分隔 | `com.wh.module.contract` |
| REST API 路径 | kebab-case | `/api/contract/list` |
| SQLite 表名 | UPPER_SNAKE_CASE | `WH_CONTRACT` |
| SQLite 列名 | UPPER_SNAKE_CASE | `CONTRACT_ID` |
| 前端组件 | PascalCase 文件名 | `CustomerDetail.vue` |
| 前端 API 函数 | camelCase + Api 后缀 | `getContractListApi()` |

### 5.6 异常处理规范

- 业务异常使用自定义 `ServiceException`，由 `GlobalExceptionHandler` 统一处理
- 日志分级：DEBUG（开发调试）/ INFO（关键流程）/ WARN（可恢复异常）/ ERROR（需人工介入）
- 敏感信息（密钥、数据库密码）禁止硬编码，使用环境变量或配置中心
- 全局异常处理器映射 SQLite SQL 错误码到友好提示

### 5.7 SQLite 数据库规范

- **唯一运行时数据库**：SQLite 3
- 连接方式：文件路径（`jdbc:sqlite:/path/to/wh.sqlite`）
- 校验语句：`SELECT 1`
- 分页：MyBatis Plus 自动使用 SQLite 方言（LIMIT/OFFSET）（当 `app.database-type=sqlite`）
- NULL 处理：`jdbc-type-for-null: VARCHAR`（避免 null 绑定问题）
- **仅使用表和索引**：不依赖视图、存储过程、函数、触发器、事件
- 复杂分析继续使用 **表 + 索引 + 业务代码**，不引入额外数据库对象
- 业务规则、数据权限、状态回算、筛选组合放在后端业务代码与 SQL 查询层

### 5.8 前端 UI 规范

- 新页面应遵循 **customer 模块** 模式（`src/views/customer/index.vue`, `detail.vue`）
- 使用全局 SCSS 工具类（`wh-page-toolbar--*`、`wh-table--*`、`wh-cell-ellipsis`）
- 禁止在业务页面中覆盖 `.layout-content` padding
- 布局尺寸通过 CSS 变量控制（`--wh-layout-content-pad-*`、`--wh-layout-header-height`）
- 前端使用 camelCase 参数名，后端 SQLite 列使用 UPPER_SNAKE_CASE

### 5.9 全栈开发规范

- **同步修改**：修改 API 时，同时更新后端 Controller/DTO 和前端 API 模块（`src/api/`）
- **权限同步**：新增后端端点需要在 `deploy/init-sql/sqlite/` 添加对应 `sys_permission` 记录
- **路由标注**：前端路由使用 `meta.perm` 标注权限标识
- **前后端命名差异**：前端 camelCase ↔ 后端/SQLite UPPER_SNAKE_CASE，由 MyBatis Plus 处理映射

### 5.10 测试规范

**后端：**
- 测试目录镜像 main 目录结构（`src/test/java/com/wh/...`）
- 控制器测试使用 `MockMvc`（`*WebMvcTest` 模式）
- 工具类/计算器/策略类需有独立单元测试
- 框架：JUnit 5 + Mockito（含 `mockito-inline` 静态 Mock）

**前端：**
- E2E 测试使用 Playwright（`wh-frontend/e2e/`）
- 认证辅助：`e2e/helpers/wh-auth.ts`
- 测试账号：环境变量 `WH_TEST_USER` / `WH_TEST_PASSWORD`（默认 `admin` / `admin123`）

### 5.11 数据库迁移规范

- 所有迁移脚本放在 `deploy/init-sql/sqlite/`
- 脚本按序号递增命名（如 `001_xxx.sql`、`002_xxx.sql`）
- 通过 `npm run dev:db:migrate` 执行（自动检测 sqlplus 或 JDBC）
- JDBC 模式：`npm run dev:db:migrate:jdbc`（无需 sqlplus）
- 模块专用迁移：`npm run dev:db:migrate:sqlite:payment` 等
- 迁移脚本读取 `DB_URL` / `DB_USER` / `DB_PASS` 从 `.env.local` 或环境变量

### 5.12 环境变量

| 变量 | 说明 | 示例 |
|------|------|------|
| `SPRING_PROFILES_ACTIVE` | 激活的 Spring 配置 | `sqlite,dev` |
| `DB_URL` | SQLite 数据库文件路径 | `jdbc:sqlite:data/wh.sqlite` |
| `DB_USER` | 数据库用户 | `whdev` |
| `DB_PASS` | 数据库密码 | `123456` |
| `SERVER_PORT` | 后端端口 | `8080` |
| `WECHAT_CORP_ID` | 企业微信 CorpID | — |
| `WECHAT_AGENT_ID` | 企业微信 AgentID | — |
| `WECHAT_SECRET` | 企业微信 Secret | — |

### 5.13 SQLite Profile 切换

| 场景 | `SPRING_PROFILES_ACTIVE` | 说明 |
|------|--------------------------|------|
| 本地文件 SQLite | `sqlite,dev` | 默认配置，数据库文件在 `data/wh.sqlite` |
| 生产路径自定义 | `sqlite,dev` | 设置 `DB_URL=jdbc:sqlite:/data/prod/wh.sqlite` |

### 5.14 开发原则

- **手术式修改**：只触碰必须修改的代码，匹配现有风格
- **简单优先**：不做超出需求的抽象或特性
- **目标驱动**：实施前先定义可验证的成功标准
- **Karpathy 准则**：
  - 编码前先思考，不假设，不隐藏困惑
  - 最小代码解决问题，不做猜测性开发
  - 不"改进"相邻无关代码，不重构没问题的部分
  - 匹配现有风格，即使你会有不同做法

### 5.15 前端代码质量

```bash
# ESLint 检查
cd wh-frontend && npm run lint

# ESLint 自动修复
cd wh-frontend && npm run lint:fix

# Prettier 格式化
cd wh-frontend && npm run format
```

### 5.16 后端代码质量

```bash
# Checkstyle 检查
cd wh-backend && mvn checkstyle:check
```

---

## 六、基础设施

| 组件 | 用途 | 说明 |
|------|------|------|
| Redis | 缓存（字典项、JWT 黑名单）+ 会话状态 | `application.yml` 配置 |
| MinIO | 附件对象存储 | `application.yml` 配置 |
| Knife4j | API 文档 | 访问 `/api/doc.html`（启用时） |
| Docker Compose | 容器编排 | `deploy/docker-compose.yml`（Redis + MinIO + App） |
| ShedLock | 分布式锁 | 多实例部署时防止定时任务重复执行 |
| 企业微信 | 消息推送/回调 | URL: `/api/wechat/callback` |
| 用友 U8 | 财务集成 | `WhU8Configuration` 配置 |

---

## 七、常用命令速查

### 开发

```bash
npm install              # 安装依赖
npm run dev              # 同时启动前后端
npm run dev:fe           # 仅前端（Vite HMR，http://localhost:4175）
npm run dev:be           # 仅后端（Spring Boot，http://localhost:8080/api）
```

### 构建

```bash
# 后端编译
cd wh-backend && mvn -q -DskipTests compile

# 前端生产构建
cd wh-frontend && npm run build

# Docker 生产容器
npm run prod:up
npm run prod:rebuild
npm run prod:stop
```

### 测试

```bash
# 后端全量测试
cd wh-backend && mvn test

# 后端单个测试类
cd wh-backend && mvn test -Dtest=ClassName

# 前端 E2E 测试
cd wh-frontend && npm run test:e2e
cd wh-frontend && npm run test:e2e:ui
```

### 数据库迁移

```bash
npm run dev:db:migrate           # 全量迁移（自动检测 sqlplus/JDBC）
npm run dev:db:migrate:jdbc      # JDBC 模式（无需 sqlplus）
npm run dev:db:migrate:sqlite:payment  # 支付模块专用迁移
```

### 代码质量

```bash
cd wh-frontend && npm run lint       # ESLint 检查
cd wh-frontend && npm run lint:fix   # ESLint 自动修复
cd wh-frontend && npm run format     # Prettier 格式化
cd wh-backend && mvn checkstyle:check # Checkstyle 检查
```

---
