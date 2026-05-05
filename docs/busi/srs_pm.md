# WH 管理系统-项目管理 —软件需求功能规格

---

## 文档信息

| 属性 | 说明 |
|------|------|
| 文档名称 | WH 管理系统 — 项目管理模块 软件需求功能规格说明书 |
| 文档编号 | SRS-WH-PM-001 |
| 版本 | V1.0.0 |
| 状态 | 草稿 |
| 编写日期 | 2026-05-03 |
| 适用模块 | WH PM（项目管理模块） |
| 平台 | WH 中台平台（ERP+CRM+PM+HR 统一管理平台） |
| 技术栈 | Spring Boot + Flowable + SQLite + Element Plus |
| 架构 | 模块化单体架构（Modular Monolith） |

---

## PMBOK 映射参考

本文档以 **PMBOK 第6版 10大知识领域** 为功能模块骨架，并标注与 **PMBOK 第7版 8大绩效域** 的交叉映射关系。

| 本文档章节 | PMBOK 6th 知识领域 | PMBOK 7th 绩效域 |
|-----------|-------------------|-----------------|
| 第1章 概述 | 全局 | 全部 |
| 第2章 整合管理 | Integration Management | Planning, Project Work, Measurement |
| 第3章 范围管理 | Scope Management | Planning, Project Work |
| 第4章 进度管理 | Schedule Management | Planning, Project Work, Measurement |
| 第5章 成本管理 | Cost Management | Planning, Measurement, Delivery |
| 第6章 质量管理 | Quality Management | Project Work, Delivery, Measurement |
| 第7章 资源管理 | Resource Management | Team, Planning, Project Work |
| 第8章 沟通管理 | Communications Management | Stakeholder, Team |
| 第9章 风险管理 | Risk Management | Uncertainty, Planning, Project Work |
| 第10章 采购管理 | Procurement Management | Planning, Delivery, Stakeholder |
| 第11章 干系人管理 | Stakeholder Management | Stakeholder, Team |

---

## 术语与缩写

| 术语/缩写 | 说明 |
|-----------|------|
| PM | Project Management，项目管理 |
| WBS | Work Breakdown Structure，工作分解结构 |
| CPM | Critical Path Method，关键路径法 |
| EVM | Earned Value Management，挣值管理 |
| SPI | Schedule Performance Index，进度绩效指数 |
| CPI | Cost Performance Index，成本绩效指数 |
| BAC | Budget at Completion，完工预算 |
| EAC | Estimate at Completion，完工估算 |
| CV | Cost Variance，成本偏差 |
| SV | Schedule Variance，进度偏差 |
| RTM | Requirement Traceability Matrix，需求跟踪矩阵 |
| BPMN | Business Process Model and Notation，业务流程建模标记 |
| RACI | Responsible, Accountable, Consulted, Informed |
| MoSCoW | Must have, Should have, Could have, Won't have |
| WH PM | WH 项目管理模块 |

---

## 1. 系统概述

### 1.1 模块定位

WH PM 模块是 WH 中台平台（ERP+CRM+PM+HR 统一管理平台）的核心管理域之一，与 ERP（企业资源计划）、CRM（客户关系管理）、HR（人力资源）模块深度集成，为 WH 平台提供完整的项目管理能力。

PM 模块覆盖 PMBOK 定义的 10 大知识领域，为项目从立项到收尾的全生命周期提供功能支撑。

### 1.2 现有脚手架状态

| 类别 | 现状 | 处理方式 |
|------|------|---------|
| 数据库表 | 已存在 `pm_milestone`, `pm_timesheet`, `pm_risk` 等基础表的 DDL 骨架 | 保留字段命名约定，扩展为完整表结构 |
| Flowable 流程 | 已定义 `PM_TIMESHEET`、`PM_MILESTONE_CHANGE`、`PM_BUSINESS_TRIP_APPLY` 流程骨架 | 保留流程编码，重写节点定义和审批链 |
| 后端代码 | `wh-pm` 模块已创建，含基础 Controller/BO/DAO 分层骨架 | 保留包结构和分层约定，重写业务逻辑 |
| 前端页面 | 少量占位页面 | 按本 SRS 重新设计 |
| API 路由 | `/api/pm/` 前缀已注册，含少量占位端点 | 保留前缀，按本 SRS 定义完整 API 清单 |

### 1.3 架构约束

| 约束项 | 说明 |
|--------|------|
| 后端分层 | Controller → BO（业务对象） → Service → DAO → SQLite |
| 数据库 | SQLite 3（无自增序列，无存储过程，用 `AUTOINCREMENT` 和触发器替代） |
| 审批引擎 | Flowable 6.x（BPMN 2.0 流程定义） |
| 前端框架 | Vue 3 + Element Plus + Vite |
| UI 范式 | 参照 `src/views/customer/` 作为页面布局标准 |
| 认证授权 | Spring Security + JWT，角色权限矩阵 |
| 审计日志 | 所有 CUD 操作记录 `created_by`, `created_at`, `updated_by`, `updated_at` |

### 1.4 角色与权限矩阵

| 角色 | 代号 | PM 模块权限 |
|------|------|------------|
| 项目经理 | PM | 全部读写权限，变更审批决定权 |
| 业务分析设计师 | BA | 需求/范围相关功能读写 |
| 系统架构师 | ARCH | 技术方案相关功能只读+评论 |
| 后端开发 | BE | 工时/任务相关功能读写 |
| 前端开发 | FE | 工时/任务相关功能读写 |
| 数据库设计师 | DB | 数据相关功能读写 |
| 质量与测试经理 | QA | 质量/测试相关功能读写，其他只读 |
| 实施经理 | IMPL | 验收/收尾相关功能读写 |
| 项目发起人/客户 | SPONSOR | 全局只读，里程碑/验收审批权 |

---

## 2. 整合管理（Integration Management）

> **PMBOK 6th §4** | **PMBOK 7th Domains: Planning, Project Work, Measurement**

### 2.1 PMBOK 流程映射

| 流程 | 过程组 | 对应功能 |
|------|--------|---------|
| 4.1 制定项目章程 | 启动 | FR-PM-001 项目立项 |
| 4.2 制定项目管理计划 | 规划 | FR-PM-002 项目管理计划 |
| 4.3 指导与管理项目工作 | 执行 | FR-PM-003 工作执行跟踪 |
| 4.4 管理项目知识 | 执行 | FR-PM-004 项目知识库 |
| 4.5 实施整体变更控制 | 监控 | FR-PM-005 变更控制流程 |
| 4.6 结束项目或阶段 | 收尾 | FR-PM-006 项目收尾 |

---

### 2.2 功能需求：FR-PM-001 项目立项（项目章程）

**描述**：创建项目章程，定义项目的高层目标、范围概述、关键干系人、预算上限和里程碑。

**用户故事**：

| ID | 用户故事 |
|----|---------|
| US-PM-001-01 | 作为 PM，我想要创建项目章程，以便正式授权项目启动 |
| US-PM-001-02 | 作为 SPONSOR，我想要审批项目章程，以便确认项目目标和预算 |

**Given-When-Then 验收标准**：

| 场景 | Given | When | Then |
|------|-------|------|------|
| 创建章程 | PM 已进入项目管理页面 | 点击"创建项目"，填写章程信息并提交 | 系统生成章程编号（格式：`CHARTER-YYYY-NNN`），状态变为 `PENDING_APPROVAL` |
| 审批通过 | 章程状态为 `PENDING_APPROVAL` | SPONSOR 点击"审批通过" | 状态变为 `APPROVED`，项目正式立项，自动触发 4.2 流程创建项目管理计划 |
| 审批驳回 | 章程状态为 `PENDING_APPROVAL` | SPONSOR 点击"驳回"并填写意见 | 状态变为 `REJECTED`，通知 PM 修改后可重新提交 |
| 查看章程 | 章程已存在 | 用户点击查看 | 显示章程详情，包含所有字段及审批历史 |

**数据模型**：

```sql
CREATE TABLE pm_project_charter (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    charter_code    TEXT NOT NULL UNIQUE,          -- CHARTER-YYYY-NNN
    project_name    TEXT NOT NULL,
    project_code    TEXT NOT NULL UNIQUE,          -- PRJ-YYYY-NNN
    description     TEXT,
    objectives      TEXT,                          -- JSON 数组：[{objective, metric, target}]
    scope_summary   TEXT,
    sponsor_id      INTEGER NOT NULL,
    pm_id           INTEGER NOT NULL,
    budget_cap      DECIMAL(15,2),
    start_date      DATE,
    end_date        DATE,
    key_stakeholders TEXT,                         -- JSON 数组：[{role, name, org}]
    status          TEXT NOT NULL DEFAULT 'DRAFT', -- DRAFT/PENDING_APPROVAL/APPROVED/REJECTED
    approval_comment TEXT,
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by      TEXT,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

**UI/UX**：

| 元素 | 说明 |
|------|------|
| 页面 | `src/views/pm/charter/` |
| 列表页 | Element Plus Table，支持按状态/项目经理筛选，分页 |
| 创建/编辑页 | Element Plus Form，分步表单（基本信息 → 目标 → 干系人 → 预览） |
| 状态标签 | el-tag：DRAFT=info, PENDING_APPROVAL=warning, APPROVED=success, REJECTED=danger |

**审批流程**：

| 流程编码 | 节点 | 审批角色 | 动作 |
|---------|------|---------|------|
| `PM_CHARTER_APPROVAL` | 1. PM 提交 | PM | 提交 |
| | 2. 发起人审批 | SPONSOR | 通过/驳回 |

**API Surface**：

| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/pm/charters` | 项目章程列表 |
| POST | `/api/pm/charters` | 创建章程 |
| GET | `/api/pm/charters/{id}` | 章程详情 |
| PUT | `/api/pm/charters/{id}` | 更新章程 |
| POST | `/api/pm/charters/{id}/submit` | 提交审批 |
| POST | `/api/pm/charters/{id}/approve` | 审批通过 |
| POST | `/api/pm/charters/{id}/reject` | 审批驳回 |

**现有脚手架**：`/api/pm/` 前缀已注册。表 `pm_project_charter` 为新表。

**测试场景**：

| # | 场景 | 预期 |
|---|------|------|
| T1 | PM 创建章程并提交 | 章程编号自动生成，状态为 PENDING_APPROVAL |
| T2 | SPONSOR 审批通过 | 状态变为 APPROVED |
| T3 | 非 SPONSOR 尝试审批 | 返回 403 Forbidden |
| T4 | 章程编号唯一性 | 重复创建相同编号返回 409 Conflict |

---

### 2.3 功能需求：FR-PM-002 项目管理计划

**描述**：整合各知识领域的子计划（范围计划、进度计划、成本计划、质量计划、资源计划、沟通计划、风险计划、采购计划、干系人计划），形成统一的项目管理计划文档。

**用户故事**：

| ID | 用户故事 |
|----|---------|
| US-PM-002-01 | 作为 PM，我想要编制项目管理计划，以便整合各子计划形成统一的执行基准 |
| US-PM-002-02 | 作为 PM，我想要管理计划版本，以便追踪计划变更历史 |

**Given-When-Then 验收标准**：

| 场景 | Given | When | Then |
|------|-------|------|------|
| 创建计划 | 项目章程已审批通过 | PM 点击"创建管理计划"，选择子计划模板 | 系统生成计划版本号 v1.0，关联对应章程 |
| 版本管理 | 计划已存在 v1.0 | PM 修改并提交新版本 | 生成 v2.0，保留 v1.0 快照和变更说明 |
| 计划基线 | 计划已审批 | PM 点击"设置基线" | 创建 `pm_plan_baseline` 记录，后续变更需走变更流程 |

**数据模型**：

```sql
CREATE TABLE pm_management_plan (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    plan_version    TEXT NOT NULL,               -- v1.0, v2.0, ...
    status          TEXT NOT NULL DEFAULT 'DRAFT', -- DRAFT/APPROVED/BASELINED
    scope_plan      TEXT,                        -- JSON：范围管理策略
    schedule_plan   TEXT,                        -- JSON：进度管理策略
    cost_plan       TEXT,                        -- JSON：成本管理策略
    quality_plan    TEXT,                        -- JSON：质量管理策略
    resource_plan   TEXT,                        -- JSON：资源管理策略
    comm_plan       TEXT,                        -- JSON：沟通管理策略
    risk_plan       TEXT,                        -- JSON：风险管理策略
    procurement_plan TEXT,                       -- JSON：采购管理策略
    stakeholder_plan TEXT,                       -- JSON：干系人管理策略
    change_history  TEXT,                        -- JSON：[{version, date, author, changes}]
    baseline_id     INTEGER REFERENCES pm_plan_baseline(id),
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by      TEXT,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pm_plan_baseline (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    plan_id         INTEGER NOT NULL REFERENCES pm_management_plan(id),
    baseline_date   DATE NOT NULL,
    baseline_data   TEXT NOT NULL,               -- JSON 快照
    baseline_label  TEXT,                        -- "初始基线"、"变更基线1"
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

**API Surface**：

| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/pm/plans?projectId=` | 项目计划列表（按版本） |
| POST | `/api/pm/plans` | 创建计划版本 |
| PUT | `/api/pm/plans/{id}` | 更新计划 |
| POST | `/api/pm/plans/{id}/baseline` | 设置基线 |
| GET | `/api/pm/baselines?projectId=` | 基线列表 |

**测试场景**：

| # | 场景 | 预期 |
|---|------|------|
| T1 | 章程未审批时创建计划 | 返回 400，提示需先审批章程 |
| T2 | 版本号递增 | v1.0 后自动建议 v2.0 |
| T3 | 设置基线后修改计划 | 创建新版本，旧版本保持 BASELINED |

---

### 2.4 功能需求：FR-PM-003 工作执行跟踪

**描述**：记录和跟踪项目工作的执行情况，包括任务状态更新、里程碑完成情况、问题日志。

**用户故事**：

| ID | 用户故事 |
|----|---------|
| US-PM-003-01 | 作为团队成员，我想要更新任务状态，以便反映实际工作进度 |
| US-PM-003-02 | 作为 PM，我想要查看工作执行仪表盘，以便了解整体项目健康状况 |

**数据模型**：

```sql
CREATE TABLE pm_work_log (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    task_id         INTEGER REFERENCES pm_task(id),
    log_date        DATE NOT NULL,
    work_description TEXT NOT NULL,
    hours_worked    DECIMAL(5,2),
    status          TEXT NOT NULL,                 -- IN_PROGRESS/COMPLETED/BLOCKED
    blocker_reason  TEXT,
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pm_issue_log (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    issue_code      TEXT NOT NULL UNIQUE,          -- ISSUE-NNN
    title           TEXT NOT NULL,
    description     TEXT,
    severity        TEXT NOT NULL,                 -- CRITICAL/HIGH/MEDIUM/LOW
    status          TEXT NOT NULL DEFAULT 'OPEN',  -- OPEN/IN_PROGRESS/RESOLVED/CLOSED
    assignee_id     INTEGER,
    resolution      TEXT,
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by      TEXT,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

**API Surface**：

| Method | Path | 说明 |
|--------|------|------|
| POST | `/api/pm/work-logs` | 记录工作日志 |
| GET | `/api/pm/work-logs?projectId=` | 工作日志列表 |
| POST | `/api/pm/issues` | 创建问题 |
| PUT | `/api/pm/issues/{id}` | 更新问题状态 |
| GET | `/api/pm/dashboard/{projectId}` | 项目健康仪表盘 |

---

### 2.5 功能需求：FR-PM-004 项目知识库

**描述**：集中管理项目过程中产生的文档、决策记录、经验教训。

**用户故事**：

| ID | 用户故事 |
|----|---------|
| US-PM-004-01 | 作为团队成员，我想要上传项目文档，以便团队成员共享信息 |
| US-PM-004-02 | 作为 PM，我想要记录经验教训，以便未来项目参考 |

**数据模型**：

```sql
CREATE TABLE pm_knowledge_base (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    kb_type         TEXT NOT NULL,                 -- DOCUMENT/DECISION/LESSON_LEARNED
    title           TEXT NOT NULL,
    content         TEXT,
    file_path       TEXT,                          -- 附件存储路径
    tags            TEXT,                          -- JSON 数组
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by      TEXT,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

### 2.6 功能需求：FR-PM-005 变更控制流程

**描述**：管理项目范围、进度、成本等基准的变更请求，通过 CCB（变更控制委员会）审批流程。

**用户故事**：

| ID | 用户故事 |
|----|---------|
| US-PM-005-01 | 作为 PM，我想要提交变更请求，以便正式管理项目基准的变更 |
| US-PM-005-02 | 作为 CCB 成员，我想要评审变更请求的影响分析，以便做出审批决策 |

**Given-When-Then 验收标准**：

| 场景 | Given | When | Then |
|------|-------|------|------|
| 提交变更 | 项目已设置基线 | PM 提交变更请求，填写变更说明和影响分析 | 生成变更编号（`CHANGE-NNN`），状态 `PENDING_CCB` |
| 影响分析 | 变更请求已提交 | 系统自动计算对进度（天数影响）和成本（金额影响）的偏差 | 在变更详情页展示偏差值 |
| CCB 审批 | 变更状态为 `PENDING_CCB` | CCB 成员审批 | 通过后更新基线，驳回则返回 `REJECTED` |

**数据模型**：

```sql
CREATE TABLE pm_change_request (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    change_code     TEXT NOT NULL UNIQUE,          -- CHANGE-NNN
    change_type     TEXT NOT NULL,                 -- SCOPE/SCHEDULE/COST/QUALITY/RESOURCE/OTHER
    description     TEXT NOT NULL,
    reason          TEXT NOT NULL,
    impact_analysis TEXT,                          -- JSON：{schedule_delta_days, cost_delta, quality_impact}
    affected_baselines TEXT,                       -- JSON：受影响的基线 ID 列表
    status          TEXT NOT NULL DEFAULT 'DRAFT', -- DRAFT/PENDING_CCB/APPROVED/REJECTED
    ccb_members     TEXT,                          -- JSON：CCB 成员列表
    approval_comment TEXT,
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by      TEXT,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

**审批流程**：

| 流程编码 | 节点 | 审批角色 | 动作 |
|---------|------|---------|------|
| `PM_CHANGE_CONTROL` | 1. PM 提交 | PM | 提交 |
| | 2. 影响分析 | 自动计算 | 系统自动 |
| | 3. CCB 评审 | CCB（PM + SPONSOR + 相关角色） | 通过/驳回 |

**API Surface**：

| Method | Path | 说明 |
|--------|------|------|
| POST | `/api/pm/changes` | 创建变更请求 |
| GET | `/api/pm/changes?projectId=` | 变更请求列表 |
| PUT | `/api/pm/changes/{id}` | 更新变更请求 |
| POST | `/api/pm/changes/{id}/approve` | CCB 审批通过 |
| POST | `/api/pm/changes/{id}/reject` | CCB 审批驳回 |

---

### 2.7 功能需求：FR-PM-006 项目收尾

**描述**：执行项目或阶段收尾，包括验收确认、文档归档、经验教训总结、资源释放。

**用户故事**：

| ID | 用户故事 |
|----|---------|
| US-PM-006-01 | 作为 PM，我想要发起项目收尾流程，以便正式结束项目 |
| US-PM-006-02 | 作为 SPONSOR，我想要进行最终验收，以便确认项目交付物符合要求 |

**数据模型**：

```sql
CREATE TABLE pm_project_closure (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    closure_type    TEXT NOT NULL,                 -- PHASE/FINAL
    status          TEXT NOT NULL DEFAULT 'INITIATED', -- INITIATED/UNDER_REVIEW/ACCEPTED/CLOSED
    deliverable_summary TEXT,                      -- JSON：交付物清单及验收状态
    lessons_learned TEXT,                          -- JSON：经验教训列表
    final_metrics   TEXT,                          -- JSON：{final_cost, final_duration, quality_score}
    acceptance_date DATE,
    acceptance_comment TEXT,
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by      TEXT,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

**API Surface**：

| Method | Path | 说明 |
|--------|------|------|
| POST | `/api/pm/closures` | 发起项目收尾 |
| GET | `/api/pm/closures/{projectId}` | 收尾详情 |
| POST | `/api/pm/closures/{id}/accept` | 最终验收 |
| POST | `/api/pm/closures/{id}/close` | 关闭项目 |

**测试场景**：

| # | 场景 | 预期 |
|---|------|------|
| T1 | 存在未关闭的变更请求时收尾 | 返回 400，需先处理所有变更 |
| T2 | SPONSOR 执行验收 | 状态变为 ACCEPTED，记录验收日期 |
| T3 | 收尾完成后更新项目状态 | 项目章程状态变为 CLOSED |

---

## 3. 范围管理（Scope Management）

> **PMBOK 6th §5** | **PMBOK 7th Domains: Planning, Project Work**

### 3.1 PMBOK 流程映射

| 流程 | 过程组 | 对应功能 |
|------|--------|---------|
| 5.1 规划范围管理 | 规划 | FR-PM-007 范围管理计划 |
| 5.2 收集需求 | 规划 | FR-PM-008 需求管理 |
| 5.3 定义范围 | 规划 | FR-PM-009 项目范围说明书 |
| 5.4 创建 WBS | 规划 | FR-PM-010 WBS 管理 |
| 5.5 确认范围 | 监控 | FR-PM-011 范围确认 |
| 5.6 控制范围 | 监控 | FR-PM-012 范围控制 |

---

### 3.2 功能需求：FR-PM-007 范围管理计划

**描述**：定义如何定义、确认和控制项目范围。

**数据模型**：范围管理计划作为 `pm_management_plan.scope_plan` JSON 字段存储，结构如下：

```json
{
  "scope_definition_method": "WBS",
  "wbs_levels": 4,
  "wbs_coding_rule": "hierarchical_numbering",
  "verification_method": "formal_inspection",
  "change_control_process": "integrated_with_FR-PM-005",
  "acceptance_criteria_template": "Given-When-Then"
}
```

---

### 3.3 功能需求：FR-PM-008 需求管理

**描述**：收集、记录、优先级排序和跟踪项目需求，维护需求跟踪矩阵（RTM）。

**用户故事**：

| ID | 用户故事 |
|----|---------|
| US-PM-008-01 | 作为 BA，我想要录入需求条目，以便将客户需求结构化 |
| US-PM-008-02 | 作为 PM，我想要查看 RTM，以便追踪需求到交付物的映射关系 |

**Given-When-Then 验收标准**：

| 场景 | Given | When | Then |
|------|-------|------|------|
| 录入需求 | BA 已进入需求管理页面 | 填写需求信息并提交 | 生成需求编号（`REQ-NNN`），状态 `ACTIVE` |
| 优先级调整 | 需求已存在 | BA 修改优先级为 Must/Should/Could/Won't | 系统记录变更历史 |
| 需求关联 | 需求已录入 | BA 关联到 WBS 工作包 | RTM 自动更新映射关系 |

**数据模型**：

```sql
CREATE TABLE pm_requirement (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    req_code        TEXT NOT NULL UNIQUE,          -- REQ-NNN
    title           TEXT NOT NULL,
    description     TEXT NOT NULL,
    source          TEXT,                          -- 需求来源：客户/法规/内部
    priority        TEXT NOT NULL,                 -- MUST/SHOULD/COULD/WONT (MoSCoW)
    status          TEXT NOT NULL DEFAULT 'ACTIVE', -- ACTIVE/CHANGED/DEFERRED/REJECTED
    acceptance_criteria TEXT,                      -- Given-When-Then 格式
    wbs_element_id  INTEGER REFERENCES pm_wbs_element(id),
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by      TEXT,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pm_rtm (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    req_id          INTEGER NOT NULL REFERENCES pm_requirement(id),
    wbs_id          INTEGER REFERENCES pm_wbs_element(id),
    design_doc_id   INTEGER REFERENCES pm_knowledge_base(id),
    test_case_id    TEXT,
    status          TEXT NOT NULL DEFAULT 'MAPPED', -- MAPPED/VERIFIED/CLOSED
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

**API Surface**：

| Method | Path | 说明 |
|--------|------|------|
| POST | `/api/pm/requirements` | 创建需求 |
| GET | `/api/pm/requirements?projectId=` | 需求列表 |
| PUT | `/api/pm/requirements/{id}` | 更新需求 |
| GET | `/api/pm/rtm?projectId=` | 需求跟踪矩阵 |
| POST | `/api/pm/rtm` | 添加 RTM 映射 |

---

### 3.4 功能需求：FR-PM-009 项目范围说明书

**描述**：详细描述项目范围，包括产品范围描述、验收标准、可交付成果、排除项、约束条件和假设条件。

**数据模型**：

```sql
CREATE TABLE pm_scope_statement (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    version         TEXT NOT NULL,
    product_description TEXT,
    acceptance_criteria TEXT,                      -- JSON 数组
    deliverables    TEXT NOT NULL,                 -- JSON：[{name, description, due_date}]
    exclusions      TEXT,                          -- JSON 数组：明确排除的内容
    constraints     TEXT,                          -- JSON 数组
    assumptions     TEXT,                          -- JSON 数组
    status          TEXT NOT NULL DEFAULT 'DRAFT', -- DRAFT/APPROVED
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by      TEXT,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

### 3.5 功能需求：FR-PM-010 WBS 管理

**描述**：创建和维护工作分解结构（WBS），将项目可交付成果逐层分解为可管理的工作包。项目WBS主表有产品、模块、优先级、WBS技术难度、任务状态，计划责任人，最新版本的计划完成日期，实际完成日期和和实际完成人字段，每个WBS有一个子表记录了该wbs的多个版本的计划开始日期、计划结束日期、实际开始日期、实际结束日期，每个版本按照0.1累加，计划日期的标识是0，实际日期的标识是1。
WBS列表页面包含了所有主表业务字段，列表中展示分为两个层级，第一层级是项目相关信息，第一层级中新增icon，点击进入新增wbs页面，一个批量导入的icon，点击上传模版文件批量该项目的WBS并允许从系统中下载模版，有一个批量导出icon点击导出该项目的WBS。
第二层级展示第一级项目下所有的WBS信息，操作列中有查看、编辑、删除、暂停/恢复icon、点击查看转入该wbs详情展示页面，点击编辑进入该wbs编辑页面。对已完成的任务不能删除和暂停。修改已经进行中的任务，需要走产品经理、项目经理审批的流程。


WBS系统明细页

**用户故事**：

| ID | 用户故事 |
|----|---------|
| US-PM-010-01 | 作为 PM，我想要创建 WBS 树形结构，以便将项目范围分解为可执行的工作包 |
| US-PM-010-02 | 作为团队成员，我想要查看 WBS 并了解自己负责的工作包，以便明确工作范围 |

**Given-When-Then 验收标准**：

| 场景 | Given | When | Then |
|------|-------|------|------|
| 创建 WBS | 范围说明书已审批 | PM 使用树形编辑器创建 WBS 层级 | 自动生成分层编码（1.1.2.3） |
| 添加工作包 | WBS 节点已存在 | PM 标记某节点为"工作包"级别 | 该节点可关联任务和工时 |
| WBS 校验 | WBS 已创建 | PM 保存时系统验证 | 100%规则检查：子节点工作量之和必须等于父节点 |

**数据模型**：

```sql
CREATE TABLE pm_wbs_element (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    wbs_code        TEXT NOT NULL,                 -- 1.0, 1.1, 1.1.1, ...
    parent_id       INTEGER REFERENCES pm_wbs_element(id),
    level           INTEGER NOT NULL,              -- 1/2/3/4
    name            TEXT NOT NULL,
    description     TEXT,
    element_type    TEXT NOT NULL,                 -- DELIVERABLE/WORK_PACKAGE/PLANNING_PACKAGE
    effort_estimate DECIMAL(10,2),                 -- 估算工时
    budget_estimate DECIMAL(15,2),                 -- 估算成本
    owner_id        INTEGER,                       -- 负责人
    status          TEXT NOT NULL DEFAULT 'PLANNED', -- PLANNED/IN_PROGRESS/COMPLETED
    sort_order      INTEGER DEFAULT 0,
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by      TEXT,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

**UI/UX**：

| 元素 | 说明 |
|------|------|
| 页面 | `src/views/pm/wbs/` |
| 视图模式 | 树形表格（el-table 树形数据）+ 思维导图模式（可选） |
| 操作 | 拖拽排序、右键菜单（新增子节点/删除/标记为工作包） |
| 验证提示 | 底部状态栏显示 100% 规则校验结果 |

**API Surface**：

| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/pm/wbs?projectId=` | WBS 树形结构 |
| POST | `/api/pm/wbs` | 创建 WBS 节点 |
| PUT | `/api/pm/wbs/{id}` | 更新 WBS 节点 |
| DELETE | `/api/pm/wbs/{id}` | 删除 WBS 节点 |
| POST | `/api/pm/wbs/validate` | WBS 100% 规则校验 |

---

### 3.6 功能需求：FR-PM-011 范围确认

**描述**：正式验收项目可交付成果，确保与客户/发起人达成一致。

**数据模型**：

```sql
CREATE TABLE pm_scope_verification (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    deliverable_id  INTEGER REFERENCES pm_wbs_element(id),
    verifier_id     INTEGER NOT NULL,              -- 验收人（通常 SPONSOR）
    verification_date DATE,
    result          TEXT NOT NULL,                 -- ACCEPTED/REJECTED/CONDITIONAL
    comments        TEXT,
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

### 3.7 功能需求：FR-PM-012 范围控制

**描述**：监控范围基准，识别和防范范围蔓延（Scope Creep）。

**用户故事**：

| ID | 用户故事 |
|----|---------|
| US-PM-012-01 | 作为 PM，我想要监控范围偏差，以便及时发现范围蔓延 |

**数据模型**：

```sql
CREATE TABLE pm_scope_variance (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    baseline_wbs_snapshot TEXT,                     -- JSON：基线 WBS 快照
    current_wbs_snapshot TEXT,                      -- JSON：当前 WBS 快照
    variance_description TEXT,
    variance_type   TEXT NOT NULL,                  -- ADDED/REMOVED/MODIFIED
    impact_assessment TEXT,
    detected_date   DATE NOT NULL,
    status          TEXT NOT NULL DEFAULT 'OPEN',   -- OPEN/RESOLVED/ACCEPTED
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

**API Surface**：

| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/pm/scope-variances?projectId=` | 范围偏差列表 |
| POST | `/api/pm/scope-variances` | 记录范围偏差 |
| GET | `/api/pm/scope-health/{projectId}` | 范围健康度评估 |

---

## 4. 进度管理（Schedule Management）

> **PMBOK 6th §6** | **PMBOK 7th Domains: Planning, Project Work, Measurement**

### 4.1 PMBOK 流程映射

| 流程 | 过程组 | 对应功能 |
|------|--------|---------|
| 6.1 规划进度管理 | 规划 | FR-PM-013 进度管理计划 |
| 6.2 定义活动 | 规划 | FR-PM-014 活动定义 |
| 6.3 排列活动顺序 | 规划 | FR-PM-015 活动排序 |
| 6.4 估算活动持续时间 | 规划 | FR-PM-016 活动工期估算 |
| 6.5 制定进度计划 | 规划 | FR-PM-017 进度计划与甘特图 |
| 6.6 控制进度 | 监控 | FR-PM-018 进度控制与偏差分析 |

---

### 4.2 功能需求：FR-PM-013 进度管理计划

**描述**：定义如何制定、管理和控制项目进度计划，包括进度单位、精度、计量方法、报告格式等。

**数据模型**：存储于 `pm_management_plan.schedule_plan` JSON 字段。

```json
{
  "schedule_unit": "day",
  "precision": "1 day",
  "estimation_method": "three_point",
  "reporting_frequency": "weekly",
  "schedule_threshold_spi": 0.9,
  "schedule_threshold_svi_days": 5,
  "baseline_update_rule": "via_change_control"
}
```

---

### 4.3 功能需求：FR-PM-014 活动定义

**描述**：将 WBS 工作包分解为具体的活动（任务），定义活动的属性。

**用户故事**：

| ID | 用户故事 |
|----|---------|
| US-PM-014-01 | 作为 PM，我想要将 WBS 工作包分解为活动，以便制定详细的进度计划 |
| US-PM-014-02 | 作为团队成员，我想要查看分配给我的活动，以便了解我的工作任务 |

**数据模型**：

```sql
CREATE TABLE pm_activity (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    wbs_id          INTEGER REFERENCES pm_wbs_element(id),
    activity_code   TEXT NOT NULL UNIQUE,          -- ACT-NNN
    name            TEXT NOT NULL,
    description     TEXT,
    activity_type   TEXT NOT NULL,                 -- TASK/MILESTONE/REVIEW/MEETING
    duration        INTEGER,                       -- 工期（天）
    effort          DECIMAL(10,2),                 -- 工时（小时）
    assignee_id     INTEGER,
    status          TEXT NOT NULL DEFAULT 'NOT_STARTED', -- NOT_STARTED/IN_PROGRESS/COMPLETED/CANCELLED
    actual_start    DATE,
    actual_end      DATE,
    planned_start   DATE,
    planned_end     DATE,
    percent_complete DECIMAL(5,2) DEFAULT 0,
    milestone_flag  INTEGER DEFAULT 0,             -- 1=里程碑，0=普通任务
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by      TEXT,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

**API Surface**：

| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/pm/activities?projectId=` | 活动列表 |
| POST | `/api/pm/activities` | 创建活动 |
| PUT | `/api/pm/activities/{id}` | 更新活动 |
| DELETE | `/api/pm/activities/{id}` | 删除活动 |

---

### 4.4 功能需求：FR-PM-015 活动排序

**描述**：定义活动之间的逻辑关系（FS/SS/FF/SF），构建项目网络图，识别关键路径。

**用户故事**：

| ID | 用户故事 |
|----|---------|
| US-PM-015-01 | 作为 PM，我想要定义活动间的依赖关系，以便构建正确的执行顺序 |
| US-PM-015-02 | 作为 PM，我想要查看关键路径，以便重点关注影响项目完工日期的活动 |

**Given-When-Then 验收标准**：

| 场景 | Given | When | Then |
|------|-------|------|------|
| 添加依赖 | 两个活动已存在 | PM 设置活动 B 依赖于活动 A（FS 关系） | 系统记录依赖关系并校验无循环依赖 |
| 循环检测 | 已有 A→B→C 的依赖链 | PM 尝试设置 C→A 的依赖 | 系统拒绝并提示"存在循环依赖" |
| 关键路径计算 | 所有活动工期和依赖已定义 | PM 点击"计算关键路径" | 系统标识关键路径活动，计算总工期 |

**数据模型**：

```sql
CREATE TABLE pm_activity_dependency (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    predecessor_id  INTEGER NOT NULL REFERENCES pm_activity(id),
    successor_id    INTEGER NOT NULL REFERENCES pm_activity(id),
    dependency_type TEXT NOT NULL,                 -- FS/SS/FF/SF
    lag_days        INTEGER DEFAULT 0,             -- 提前/滞后天数
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pm_critical_path (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    calculated_at   DATETIME NOT NULL,
    total_duration  INTEGER NOT NULL,              -- 关键路径总工期（天）
    activity_ids    TEXT NOT NULL,                 -- JSON：关键路径活动 ID 有序列表
    snapshot        TEXT                           -- JSON：完整网络图快照
);
```

**API Surface**：

| Method | Path | 说明 |
|--------|------|------|
| POST | `/api/pm/dependencies` | 创建活动依赖关系 |
| DELETE | `/api/pm/dependencies/{id}` | 删除依赖关系 |
| GET | `/api/pm/dependencies?projectId=` | 依赖关系列表 |
| POST | `/api/pm/critical-path/calculate?projectId=` | 计算关键路径 |
| GET | `/api/pm/critical-path?projectId=` | 关键路径结果 |

**关键路径算法**（CPM）：

```
1. 正向遍历：计算 ES（最早开始）和 EF（最早完成）
2. 逆向遍历：计算 LS（最迟开始）和 LF（最迟完成）
3. 浮动时间 = LS - ES，浮动时间为 0 的活动为关键活动
4. 所有关键活动组成关键路径
```

---

### 4.5 功能需求：FR-PM-016 活动工期估算

**描述**：使用多种方法估算活动工期，支持三点估算（乐观/最可能/悲观）和 PERT 计算。

**数据模型**：

```sql
CREATE TABLE pm_activity_estimation (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    activity_id     INTEGER NOT NULL REFERENCES pm_activity(id),
    method          TEXT NOT NULL,                 -- SINGLE_POINT/THREE_POINT/ANALOGOUS/PARAMETRIC
    optimistic_days DECIMAL(8,2),                  -- 三点估算：乐观
    most_likely_days DECIMAL(8,2),                 -- 三点估算：最可能
    pessimistic_days DECIMAL(8,2),                 -- 三点估算：悲观
    pert_days       DECIMAL(8,2),                  -- PERT = (O + 4M + P) / 6
    pert_std_dev    DECIMAL(8,2),                  -- σ = (P - O) / 6
    single_point_days DECIMAL(8,2),                -- 单点估算
    confidence_level DECIMAL(5,2),                 -- 置信度百分比
    basis_of_estimate TEXT,                        -- 估算依据
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by      TEXT,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

### 4.6 功能需求：FR-PM-017 进度计划与甘特图

**描述**：基于活动、工期、依赖关系生成项目进度计划，以甘特图形式可视化展示。

**用户故事**：

| ID | 用户故事 |
|----|---------|
| US-PM-017-01 | 作为 PM，我想要查看甘特图，以便直观了解项目进度和时间安排 |
| US-PM-017-02 | 作为 PM，我想要在甘特图上拖拽调整任务时间，以便快速调整计划 |

**UI/UX**：

| 元素 | 说明 |
|------|------|
| 页面 | `src/views/pm/schedule/` |
| 甘特图组件 | 基于 Element Plus + 自定义甘特图渲染（或集成 gantt-task-react 的 Vue 等价方案） |
| 视图 | 日/周/月三种时间粒度切换 |
| 交互 | 拖拽调整任务时间、右键设置依赖关系 |
| 基线叠加 | 基线进度以灰色条叠加在计划条下方，偏差一目了然 |
| 关键路径高亮 | 关键路径活动条以红色显示 |
| 里程碑标记 | 里程碑以菱形图标显示在时间轴上 |

**API Surface**：

| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/pm/gantt?projectId=` | 甘特图数据（活动+依赖+基线） |
| PUT | `/api/pm/schedule/bulk-update` | 甘特图拖拽后批量更新 |
| POST | `/api/pm/schedule/baseline-compare?projectId=` | 基线 vs 实际对比数据 |

---

### 4.7 功能需求：FR-PM-018 进度控制与偏差分析

**描述**：监控实际进度与计划的偏差，计算进度绩效指标（SPI、SV），生成进度预警。

**用户故事**：

| ID | 用户故事 |
|----|---------|
| US-PM-018-01 | 作为 PM，我想要查看进度偏差仪表盘，以便及时发现进度风险 |
| US-PM-018-02 | 作为 SPONSOR，我想要接收进度预警通知，以便了解项目是否按期推进 |

**Given-When-Then 验收标准**：

| 场景 | Given | When | Then |
|------|-------|------|------|
| SPI 计算 | 项目有已更新进度的活动 | 系统自动计算 SPI = EV / PV | SPI 显示在仪表盘，< 0.9 时标红预警 |
| SV 计算 | 项目有已更新进度的活动 | 系统自动计算 SV = EV - PV | SV 显示偏差天数 |
| 预警触发 | SPI < 阈值（0.9） | 系统检查到偏差 | 生成预警记录，通知 PM 和 SPONSOR |

**数据模型**：

```sql
CREATE TABLE pm_schedule_variance (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    analysis_date   DATE NOT NULL,
    pv              DECIMAL(15,2),                 -- 计划价值（Planned Value）
    ev              DECIMAL(15,2),                 -- 挣值（Earned Value）
    sv              DECIMAL(15,2),                 -- 进度偏差 SV = EV - PV
    spi             DECIMAL(8,4),                  -- 进度绩效指数 SPI = EV / PV
    variance_days   INTEGER,                       -- 偏差天数（正=提前，负=滞后）
    forecast_end_date DATE,                        -- 预测完工日期
    alert_triggered INTEGER DEFAULT 0,             -- 1=已触发预警
    analysis_notes  TEXT,
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pm_schedule_alert (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    alert_type      TEXT NOT NULL,                 -- SPI_BELOW_THRESHOLD/TASK_DELAYED/CRITICAL_PATH_CHANGED
    severity        TEXT NOT NULL,                 -- WARNING/CRITICAL
    message         TEXT NOT NULL,
    status          TEXT NOT NULL DEFAULT 'UNREAD', -- UNREAD/READ/RESOLVED
    notify_users    TEXT,                          -- JSON：接收通知的用户 ID 列表
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

**API Surface**：

| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/pm/schedule-variance?projectId=` | 进度偏差分析数据 |
| POST | `/api/pm/schedule-variance/calculate?projectId=` | 触发进度分析计算 |
| GET | `/api/pm/schedule-alerts?projectId=` | 进度预警列表 |
| PUT | `/api/pm/schedule-alerts/{id}/resolve` | 解决预警 |

---

## 5. 成本管理（Cost Management）

> **PMBOK 6th §7** | **PMBOK 7th Domains: Planning, Measurement, Delivery**

### 5.1 PMBOK 流程映射

| 流程 | 过程组 | 对应功能 |
|------|--------|---------|
| 7.1 规划成本管理 | 规划 | FR-PM-019 成本管理计划 |
| 7.2 估算成本 | 规划 | FR-PM-020 成本估算 |
| 7.3 制定预算 | 规划 | FR-PM-021 预算制定 |
| 7.4 控制成本 | 监控 | FR-PM-022 成本控制与挣值分析 |

---

### 5.2 功能需求：FR-PM-019 成本管理计划

**描述**：定义如何估算、预算和控制项目成本，包括计量单位、精度、控制阈值、报告格式。

**数据模型**：存储于 `pm_management_plan.cost_plan` JSON 字段。

```json
{
  "currency": "CNY",
  "precision": "2 decimal places",
  "estimation_methods": ["bottom_up", "three_point", "parametric"],
  "cost_threshold_pct": 10,
  "cpi_alert_threshold": 0.9,
  "reporting_frequency": "monthly",
  "ev_analysis_frequency": "monthly"
}
```

---

### 5.3 功能需求：FR-PM-020 成本估算

**描述**：对每个活动和 WBS 元素进行成本估算，支持自下而上、类比估算和参数估算。

**数据模型**：

```sql
CREATE TABLE pm_cost_estimate (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    wbs_id          INTEGER REFERENCES pm_wbs_element(id),
    activity_id     INTEGER REFERENCES pm_activity(id),
    estimate_type   TEXT NOT NULL,                 -- BOTTOM_UP/ANALOGOUS/PARAMETRIC/THREE_POINT
    labor_cost      DECIMAL(15,2) DEFAULT 0,
    material_cost   DECIMAL(15,2) DEFAULT 0,
    equipment_cost  DECIMAL(15,2) DEFAULT 0,
    other_cost    DECIMAL(15,2) DEFAULT 0,
    contingency_reserve DECIMAL(15,2) DEFAULT 0,
    total_estimate DECIMAL(15,2) NOT NULL,
    confidence_level DECIMAL(5,2),
    basis_of_estimate TEXT,
    version         TEXT NOT NULL DEFAULT 'v1.0',
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by      TEXT,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

### 5.4 功能需求：FR-PM-021 预算制定

**描述**：汇总成本估算，形成项目成本基准（Performance Measurement Baseline），包含应急储备和管理储备。
项目预算项目分为两个层级，一级预算项目分为人工，采购，差旅，商务费用，客户招待费，活动费和其他。二级预算人工细分为不同岗位/角色人员投入的工时，以及工时的成本定额，采购细分为一级BOM项。项目预算提交后需要走一个审批流，具体审批流程按需配置。
实际人工成本按照实际审批通过的工时乘以对应岗位/角色的定额来计算到工时发生日期，采购按照审批通过的采购订单金额来计算到采购订单发起审批的日期，差旅、商务费用、客户招待费、活动费按照项目报销单金额计算到发起报销的日期，其他费用由财务按照规则分配到具体项目中，实际成本允许计入负向。
每个项目允许设置成本控制预警值，其中成本达到或超过预算的80%项目需要在首页中进行提醒，预算达到或超过95%的项目需要预警到管理者，且该预警只有管理者才能关闭，预算超过100%的项目需要提醒项目经理和管理者进行项目复盘，复盘后由管理者关闭预警。
在项目明细页面中，增加项目预预实对比tab页，页面左边展示审批通过的树状结构展示的项目一二级预算（不分页），页面右侧按照预算维度展示实际预算（不分页），点击实际预算维度进入该项目实际预算明细分页页面，且默认展示点击进入的预算维度的数据。

**用户故事**：

| ID | 用户故事 |
|----|---------|
| US-PM-021-01 | 作为 PM，我想要制定项目预算，以便建立成本基准 |
| US-PM-021-02 | 作为 SPONSOR，我想要审批预算和管理储备，以便控制资金拨付 |

**数据模型**：

```sql
CREATE TABLE pm_budget (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    budget_code     TEXT NOT NULL UNIQUE,          -- BUDGET-YYYY-NNN
    version         TEXT NOT NULL,
    cost_baseline DECIMAL(15,2) NOT NULL,          -- 成本基准（不含管理储备）
    management_reserve DECIMAL(15,2) DEFAULT 0,   -- 管理储备
    total_budget    DECIMAL(15,2) NOT NULL,        -- 总预算 = 成本基准 + 管理储备
    time_phased_data TEXT,                          -- JSON：按月分布的 PV 计划值
    status          TEXT NOT NULL DEFAULT 'DRAFT', -- DRAFT/PENDING_APPROVAL/APPROVED
    approval_comment TEXT,
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by      TEXT,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pm_actual_cost (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    activity_id     INTEGER REFERENCES pm_activity(id),
    cost_date       DATE NOT NULL,
    cost_type       TEXT NOT NULL,                 -- LABOR/MATERIAL/EQUIPMENT/OTHER
    amount          DECIMAL(15,2) NOT NULL,
    description     TEXT,
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

**API Surface**：

| Method | Path | 说明 |
|--------|------|------|
| POST | `/api/pm/budgets` | 创建预算 |
| GET | `/api/pm/budgets?projectId=` | 预算列表 |
| POST | `/api/pm/budgets/{id}/approve` | 审批预算 |
| POST | `/api/pm/actual-costs` | 记录实际成本 |
| GET | `/api/pm/actual-costs?projectId=` | 实际成本列表 |

---

### 5.5 功能需求：FR-PM-022 成本控制与挣值分析

**描述**：监控项目成本绩效，执行挣值分析（EVM），计算 CV、CPI、EAC、ETC、VAC 等指标。

**用户故事**：

| ID | 用户故事 |
|----|---------|
| US-PM-022-01 | 作为 PM，我想要查看挣值分析仪表盘，以便了解项目的成本和进度健康状况 |
| US-PM-022-02 | 作为 SPONSOR，我想要查看完工估算，以便了解项目是否需要追加预算 |

**Given-When-Then 验收标准**：

| 场景 | Given | When | Then |
|------|-------|------|------|
| EVM 计算 | 项目有 PV、EV、AC 数据 | 系统计算 CV、SV、CPI、SPI、EAC、ETC、VAC | 仪表盘显示所有指标，偏差超阈值时预警 |
| EAC 趋势 | CPI 持续 < 1.0 | 系统显示 EAC > BAC | 提示预算超支风险 |
| 成本预警 | CPI < 0.9 | 系统检查 | 生成成本预警通知 |

**EVM 指标计算公式**：

| 指标 | 公式 | 说明 |
|------|------|------|
| CV（成本偏差） | CV = EV - AC | 正=节约，负=超支 |
| SV（进度偏差） | SV = EV - PV | 正=提前，负=滞后 |
| CPI（成本绩效指数） | CPI = EV / AC | >1=节约，<1=超支 |
| SPI（进度绩效指数） | SPI = EV / PV | >1=提前，<1=滞后 |
| EAC（完工估算） | EAC = BAC / CPI | 典型偏差假设 |
| ETC（完工尚需估算） | ETC = EAC - AC | 剩余工作成本 |
| VAC（完工偏差） | VAC = BAC - EAC | 正=预计节约，负=预计超支 |

**数据模型**：

```sql
CREATE TABLE pm_evm_analysis (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    analysis_date   DATE NOT NULL,
    bac             DECIMAL(15,2),                 -- 完工预算
    pv              DECIMAL(15,2),                 -- 计划价值
    ev              DECIMAL(15,2),                 -- 挣值
    ac              DECIMAL(15,2),                 -- 实际成本
    cv              DECIMAL(15,2),                 -- 成本偏差
    sv              DECIMAL(15,2),                 -- 进度偏差
    cpi             DECIMAL(8,4),                  -- 成本绩效指数
    spi             DECIMAL(8,4),                  -- 进度绩效指数
    eac             DECIMAL(15,2),                 -- 完工估算
    etc             DECIMAL(15,2),                 -- 完工尚需估算
    vac             DECIMAL(15,2),                 -- 完工偏差
    tcpi            DECIMAL(8,4),                  -- 完工尚需绩效指数
    alert_triggered INTEGER DEFAULT 0,
    analysis_notes  TEXT,
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

**UI/UX**：

| 元素 | 说明 |
|------|------|
| 页面 | `src/views/pm/cost/` |
| 仪表盘 | EVM 指标卡片（CPI/SPI 用仪表盘图，CV/SV 用柱状图） |
| S 曲线 | 累计 PV/EV/AC 三线 S 曲线图 |
| 预警列表 | 红色高亮超阈值指标 |

**API Surface**：

| Method | Path | 说明 |
|--------|------|------|
| POST | `/api/pm/evm/calculate?projectId=` | 执行挣值分析计算 |
| GET | `/api/pm/evm?projectId=` | 挣值分析历史数据 |
| GET | `/api/pm/cost-dashboard/{projectId}` | 成本仪表盘数据（含 S 曲线） |

---

## 6. 质量管理（Quality Management）

> **PMBOK 6th §8** | **PMBOK 7th Domains: Project Work, Delivery, Measurement**

### 6.1 PMBOK 流程映射

| 流程 | 过程组 | 对应功能 |
|------|--------|---------|
| 8.1 规划质量管理 | 规划 | FR-PM-023 质量管理计划 |
| 8.2 管理质量 | 执行 | FR-PM-024 质量保证 |
| 8.3 控制质量 | 监控 | FR-PM-025 质量控制 |

---

### 6.2 功能需求：FR-PM-023 质量管理计划

**描述**：定义项目的质量标准、质量度量指标、质量检查频率和验收标准。

**数据模型**：存储于 `pm_management_plan.quality_plan` JSON 字段。

```json
{
  "quality_standards": ["code_review", "unit_test_coverage", "e2e_test"],
  "metrics": {
    "unit_test_coverage_min": 80,
    "defect_density_max_per_kloc": 5,
    "e2e_pass_rate_min": 95,
    "code_review_required": true
  },
  "quality_gate_rules": {
    "backend": ["mvn test 通过率=100%", "Checkstyle 检查通过"],
    "frontend": ["ESLint 检查通过", "Playwright E2E 测试通过"],
    "database": ["迁移脚本执行成功", "数据一致性验证"]
  },
  "review_frequency": "per_sprint",
  "milestone_review_required": true
}
```

---

### 6.3 功能需求：FR-PM-024 质量保证

**描述**：执行质量审计和过程分析，确保项目活动符合质量标准和组织过程资产。

**用户故事**：

| ID | 用户故事 |
|----|---------|
| US-PM-024-01 | 作为 QA，我想要执行质量审计，以便确认项目过程符合质量标准 |
| US-PM-024-02 | 作为 PM，我想要查看质量审计结果，以便了解质量合规情况 |

**数据模型**：

```sql
CREATE TABLE pm_quality_audit (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    audit_code      TEXT NOT NULL UNIQUE,          -- AUDIT-NNN
    audit_type      TEXT NOT NULL,                 -- PROCESS_AUDIT/PRODUCT_AUDIT/DELIVERABLE_REVIEW
    audit_date      DATE NOT NULL,
    auditor_id      INTEGER NOT NULL,
    scope           TEXT NOT NULL,
    findings        TEXT,                          -- JSON：[{finding, severity, recommendation}]
    overall_result  TEXT NOT NULL,                 -- PASS/CONDITIONAL/FAIL
    corrective_actions TEXT,                       -- JSON：[{action, assignee, due_date}]
    status          TEXT NOT NULL DEFAULT 'PLANNED', -- PLANNED/IN_PROGRESS/COMPLETED
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by      TEXT,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

**API Surface**：

| Method | Path | 说明 |
|--------|------|------|
| POST | `/api/pm/quality-audits` | 创建质量审计 |
| GET | `/api/pm/quality-audits?projectId=` | 审计列表 |
| PUT | `/api/pm/quality-audits/{id}` | 更新审计结果 |

---

### 6.4 功能需求：FR-PM-025 质量控制

**描述**：监控具体可交付成果的质量，识别缺陷并跟踪纠正措施。

**用户故事**：

| ID | 用户故事 |
|----|---------|
| US-PM-025-01 | 作为 QA，我想要记录质量检查中发现的缺陷，以便跟踪修复 |
| US-PM-025-02 | 作为团队成员，我想要查看分配给我的缺陷修复任务，以便及时处理 |

**Given-When-Then 验收标准**：

| 场景 | Given | When | Then |
|------|-------|------|------|
| 记录缺陷 | QA 执行质量检查 | 填写缺陷信息并提交 | 生成缺陷编号（`DEFECT-NNN`），状态 `OPEN` |
| 缺陷修复 | 缺陷状态为 `OPEN` | 开发人员修复后标记为 `FIXED` | 状态变为 `FIXED`，待 QA 验证 |
| 缺陷验证 | 缺陷状态为 `FIXED` | QA 验证通过/不通过 | 通过 → `CLOSED`，不通过 → `REOPENED` |

**数据模型**：

```sql
CREATE TABLE pm_quality_defect (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    defect_code     TEXT NOT NULL UNIQUE,          -- DEFECT-NNN
    title           TEXT NOT NULL,
    description     TEXT NOT NULL,
    severity        TEXT NOT NULL,                 -- CRITICAL/MAJOR/MINOR/COSMETIC
    priority        TEXT NOT NULL,                 -- P0/P1/P2/P3
    source          TEXT NOT NULL,                 -- CODE_REVIEW/UNIT_TEST/E2E_TEST/UAT/PRODUCTION
    related_activity_id INTEGER REFERENCES pm_activity(id),
    status          TEXT NOT NULL DEFAULT 'OPEN',  -- OPEN/FIXED/CLOSED/REOPENED/DEFERRED
    reported_by     INTEGER NOT NULL,
    assigned_to     INTEGER,
    fix_description TEXT,
    fixed_date      DATETIME,
    verified_date   DATETIME,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

**API Surface**：

| Method | Path | 说明 |
|--------|------|------|
| POST | `/api/pm/defects` | 创建缺陷 |
| GET | `/api/pm/defects?projectId=` | 缺陷列表 |
| PUT | `/api/pm/defects/{id}` | 更新缺陷 |
| GET | `/api/pm/quality-dashboard/{projectId}` | 质量仪表盘（缺陷趋势、通过率） |

---

## 7. 资源管理（Resource Management）

> **PMBOK 6th §9** | **PMBOK 7th Domains: Team, Planning, Project Work**

### 7.1 PMBOK 流程映射

| 流程 | 过程组 | 对应功能 |
|------|--------|---------|
| 9.1 规划资源管理 | 规划 | FR-PM-026 资源管理计划 |
| 9.2 估算活动资源 | 规划 | FR-PM-027 资源估算 |
| 9.3 获取资源 | 执行 | FR-PM-028 资源分配 |
| 9.4 建设团队 | 执行 | FR-PM-029 团队管理 |
| 9.5 管理团队 | 执行 | FR-PM-030 团队绩效跟踪 |
| 9.6 控制资源 | 监控 | FR-PM-031 资源使用监控 |

---

### 7.2 功能需求：FR-PM-026 资源管理计划

**描述**：定义如何识别、获取、分配和管理项目资源（人力、设备、材料）。

**数据模型**：存储于 `pm_management_plan.resource_plan` JSON 字段。

```json
{
  "resource_categories": ["DEVELOPER", "DESIGNER", "TESTER", "INFRASTRUCTURE"],
  "allocation_method": "time_based",
  "max_allocation_pct": 100,
  "timesheet_required": true,
  "timesheet_frequency": "weekly",
  "utilization_target_pct": 85,
  "conflict_resolution": "pm_decision"
}
```

---

### 7.3 功能需求：FR-PM-027 资源估算

**描述**：估算每个活动所需的资源类型和数量。

**数据模型**：

```sql
CREATE TABLE pm_resource_estimate (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    activity_id     INTEGER REFERENCES pm_activity(id),
    resource_type   TEXT NOT NULL,                 -- HUMAN/EQUIPMENT/MATERIAL/FACILITY
    resource_role   TEXT,                          -- BE/FE/DB/QA/BA/ARCH（人力资源）
    quantity        DECIMAL(10,2) NOT NULL,        -- 数量或工时
    unit            TEXT NOT NULL,                 -- PERSON_DAY/PERSON_HOUR/UNIT
    unit_cost       DECIMAL(15,2),                 -- 单位成本
    total_cost      DECIMAL(15,2),                 -- 总成本
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

### 7.4 功能需求：FR-PM-028 资源分配

**描述**：将团队成员分配到具体项目和活动，跟踪资源利用率和冲突。

**用户故事**：

| ID | 用户故事 |
|----|---------|
| US-PM-028-01 | 作为 PM，我想要将团队成员分配到项目和活动，以便明确责任分工 |
| US-PM-028-02 | 作为 PM，我想要查看资源分配热力图，以便识别资源冲突和过度分配 |

**Given-When-Then 验收标准**：

| 场景 | Given | When | Then |
|------|-------|------|------|
| 分配资源 | 项目和成员已存在 | PM 设置成员的分配比例和起止日期 | 系统记录分配，计算利用率 |
| 过度分配 | 成员已在项目 A 分配 80% | PM 尝试在同一时期分配到项目 B（40%） | 系统提示"资源过度分配（120% > 100%）" |
| 资源日历 | 资源已分配 | 成员标记休假日期 | 系统在该时段内降低可用工时 |

**数据模型**：

```sql
CREATE TABLE pm_resource_assignment (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    activity_id     INTEGER REFERENCES pm_activity(id),
    assignee_id     INTEGER NOT NULL,
    role_in_project TEXT NOT NULL,                 -- 在该项目中的角色
    allocation_pct  DECIMAL(5,2) NOT NULL,         -- 分配比例（%）
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    status          TEXT NOT NULL DEFAULT 'ACTIVE', -- ACTIVE/COMPLETED/RELEASED
    notes           TEXT,
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by      TEXT,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pm_resource_calendar (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    assignee_id     INTEGER NOT NULL,
    leave_date      DATE NOT NULL,
    leave_type      TEXT NOT NULL,                 -- ANNUAL_LEAVE/SICK_LEAVE/HOLIDAY/TRAINING
    project_id      INTEGER REFERENCES pm_project_charter(id),
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

**API Surface**：

| Method | Path | 说明 |
|--------|------|------|
| POST | `/api/pm/resource-assignments` | 创建资源分配 |
| GET | `/api/pm/resource-assignments?projectId=` | 资源分配列表 |
| PUT | `/api/pm/resource-assignments/{id}` | 更新资源分配 |
| GET | `/api/pm/resource-heatmap?userId=&dateRange=` | 资源热力图 |
| POST | `/api/pm/resource-conflicts/detect?projectId=` | 检测资源冲突 |

---

### 7.5 功能需求：FR-PM-029 团队管理

**描述**：管理团队建设活动、技能评估、培训记录和团队氛围。

**数据模型**：

```sql
CREATE TABLE pm_team_skill (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    assignee_id     INTEGER NOT NULL,
    skill_name      TEXT NOT NULL,                 -- Java/Vue/DB/Testing/...
    proficiency     INTEGER NOT NULL,              -- 1-5
    last_assessed   DATE,
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pm_training_record (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    assignee_id     INTEGER NOT NULL,
    training_name   TEXT NOT NULL,
    training_date   DATE,
    hours           DECIMAL(5,2),
    cost            DECIMAL(15,2),
    outcome         TEXT,
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

### 7.6 功能需求：FR-PM-030 团队绩效跟踪

**描述**：跟踪团队成员的工作产出和效率，包括完成的任务数、工时利用率、质量指标。

**用户故事**：

| ID | 用户故事 |
|----|---------|
| US-PM-030-01 | 作为 PM，我想要查看团队成员的绩效指标，以便评估团队表现 |

**API Surface**：

| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/pm/team-performance?projectId=` | 团队绩效数据 |
| GET | `/api/pm/member-performance/{memberId}?projectId=` | 个人绩效数据 |

**关键指标**：

| 指标 | 计算方式 |
|------|---------|
| 任务完成率 | 已完成任务数 / 总分配任务数 |
| 工时偏差 | 实际工时 - 估算工时 |
| 缺陷密度 | 该成员相关缺陷数 / 完成任务数 |
| 资源利用率 | 实际工时 / 可用工时 × 100% |

---

### 7.7 功能需求：FR-PM-031 资源使用监控

**描述**：监控资源实际使用情况与计划的偏差，识别资源浪费或短缺。

**数据模型**：

```sql
CREATE TABLE pm_resource_utilization (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    assignee_id     INTEGER NOT NULL,
    period_start    DATE NOT NULL,
    period_end      DATE NOT NULL,
    planned_hours   DECIMAL(10,2),
    actual_hours    DECIMAL(10,2),
    utilization_pct DECIMAL(5,2),
    idle_hours      DECIMAL(10,2),
    overtime_hours  DECIMAL(10,2),
    notes           TEXT,
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

## 8. 沟通管理（Communications Management）

> **PMBOK 6th §10** | **PMBOK 7th Domains: Stakeholder, Team**

### 8.1 PMBOK 流程映射

| 流程 | 过程组 | 对应功能 |
|------|--------|---------|
| 10.1 规划沟通管理 | 规划 | FR-PM-032 沟通管理计划 |
| 10.2 管理沟通 | 执行 | FR-PM-033 沟通执行与记录 |
| 10.3 监督沟通 | 监控 | FR-PM-034 沟通效果评估 |

---

### 8.2 功能需求：FR-PM-032 沟通管理计划

**描述**：定义项目信息的需求、格式、频率、分发渠道和责任人。

**数据模型**：存储于 `pm_management_plan.comm_plan` JSON 字段。

```json
{
  "communication_matrix": [
    {
      "communication_type": "DAILY_STANDUP",
      "frequency": "daily",
      "time": "09:30",
      "channel": "video_call",
      "participants": ["PM", "BE", "FE", "DB", "QA"],
      "duration_min": 15
    },
    {
      "communication_type": "WEEKLY_REPORT",
      "frequency": "weekly",
      "day": "Friday",
      "channel": "system_generated",
      "recipients": ["PM", "SPONSOR"],
      "content": ["progress_summary", "risks", "issues", "next_week_plan"]
    },
    {
      "communication_type": "MILESTONE_REVIEW",
      "frequency": "per_milestone",
      "channel": "meeting",
      "participants": ["ALL"],
      "output": "milestone_review_report"
    },
    {
      "communication_type": "MONTHLY_REPORT",
      "frequency": "monthly",
      "channel": "system_generated",
      "recipients": ["SPONSOR", "PM"],
      "content": ["evm_summary", "risk_status", "budget_status", "schedule_status"]
    }
  ],
  "escalation_rules": {
    "critical_issue": "immediate_to_pm_and_sponsor",
    "schedule_delay_5d": "escalate_to_sponsor",
    "budget_overrun_10pct": "escalate_to_sponsor"
  }
}
```

---

### 8.3 功能需求：FR-PM-033 沟通执行与记录

**描述**：管理项目中的各类沟通活动，包括会议记录、沟通日志、报告生成和分发。

**用户故事**：

| ID | 用户故事 |
|----|---------|
| US-PM-033-01 | 作为 PM，我想要记录沟通活动，以便保留沟通历史备查 |
| US-PM-033-02 | 作为 PM，我想要自动生成周报/月报，以便减少手工报告的工作量 |

**数据模型**：

```sql
CREATE TABLE pm_communication_log (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    comm_type       TEXT NOT NULL,                 -- MEETING/EMAIL/REPORT/PHONE/AD_HOC
    subject         TEXT NOT NULL,
    description     TEXT,
    comm_date       DATE NOT NULL,
    participants    TEXT,                          -- JSON：参与者 ID 列表
    channel         TEXT,                          -- VIDEO_CALL/IN_PERSON/EMAIL/IM/REPORT
    attachments     TEXT,                          -- JSON：附件列表
    action_items    TEXT,                          -- JSON：[{item, assignee, due_date}]
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pm_meeting (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    meeting_type    TEXT NOT NULL,                 -- STANDUP/PLANNING/REVIEW/RETROSPECTIVE/AD_HOC
    title           TEXT NOT NULL,
    meeting_date    DATE NOT NULL,
    start_time      TIME,
    end_time        TIME,
    agenda          TEXT,                          -- JSON：议程列表
    minutes         TEXT,                          -- 会议纪要
    action_items    TEXT,                          -- JSON：行动项
    attendees       TEXT,                          -- JSON：参会者
    status          TEXT NOT NULL DEFAULT 'SCHEDULED', -- SCHEDULED/COMPLETED/CANCELLED
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by      TEXT,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pm_report (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    report_type     TEXT NOT NULL,                 -- WEEKLY/MONTHLY/MILESTONE/FINAL
    period_start    DATE,
    period_end      DATE,
    content         TEXT NOT NULL,                 -- JSON 或富文本
    status          TEXT NOT NULL DEFAULT 'DRAFT', -- DRAFT/GENERATED/SENT
    recipients      TEXT,                          -- JSON：接收者列表
    generated_at    DATETIME,
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

**API Surface**：

| Method | Path | 说明 |
|--------|------|------|
| POST | `/api/pm/comm-logs` | 创建沟通记录 |
| GET | `/api/pm/comm-logs?projectId=` | 沟通记录列表 |
| POST | `/api/pm/meetings` | 创建会议 |
| PUT | `/api/pm/meetings/{id}` | 更新会议纪要 |
| POST | `/api/pm/reports/generate` | 自动生成报告 |
| GET | `/api/pm/reports?projectId=` | 报告列表 |

---

### 8.4 功能需求：FR-PM-034 沟通效果评估

**描述**：评估沟通活动的有效性，确保信息被正确接收和理解。

**数据模型**：

```sql
CREATE TABLE pm_comm_effectiveness (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    evaluation_period TEXT NOT NULL,                 -- "2026-Q2"
    meeting_attendance_rate DECIMAL(5,2),            -- 平均参会率
    report_timeliness_pct DECIMAL(5,2),              -- 按时报告率
    action_item_completion_rate DECIMAL(5,2),        -- 行动项完成率
    stakeholder_satisfaction DECIMAL(5,2),           -- 满意度评分 1-5
    improvement_suggestions TEXT,                    -- JSON：改进建议
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

## 9. 风险管理（Risk Management）

> **PMBOK 6th §11** | **PMBOK 7th Domains: Uncertainty, Planning, Project Work**

### 9.1 PMBOK 流程映射

| 流程 | 过程组 | 对应功能 |
|------|--------|---------|
| 11.1 规划风险管理 | 规划 | FR-PM-035 风险管理计划 |
| 11.2 识别风险 | 规划 | FR-PM-036 风险识别 |
| 11.3 实施定性风险分析 | 规划 | FR-PM-037 定性风险分析 |
| 11.4 实施定量风险分析 | 规划 | FR-PM-038 定量风险分析 |
| 11.5 规划风险应对 | 规划 | FR-PM-039 风险应对计划 |
| 11.6 实施风险应对 | 执行 | FR-PM-040 风险应对执行 |
| 11.7 监督风险 | 监控 | FR-PM-041 风险监控台账 |

---

### 9.2 功能需求：FR-PM-035 风险管理计划

**描述**：定义如何进行风险识别、分析、应对和监控，包括概率-影响矩阵、风险阈值、报告频率。

**数据模型**：存储于 `pm_management_plan.risk_plan` JSON 字段。

```json
{
  "probability_scale": [1, 2, 3, 4, 5],
  "impact_scale": [1, 2, 3, 4, 5],
  "risk_score_matrix": "5x5",
  "risk_threshold_score": 12,
  "high_risk_threshold": 15,
  "review_frequency": "biweekly",
  "risk_categories": ["TECHNICAL", "SCHEDULE", "COST", "RESOURCE", "EXTERNAL", "QUALITY"],
  "escalation_rules": {
    "score_gte_20": "immediate_escalate_to_sponsor",
    "score_gte_15": "escalate_to_pm_within_24h"
  }
}
```

---

### 9.3 功能需求：FR-PM-036 风险识别

**描述**：识别项目中的潜在风险，记录风险描述、类别、触发条件和初步分析。

**用户故事**：

| ID | 用户故事 |
|----|---------|
| US-PM-036-01 | 作为项目成员，我想要提交识别到的风险，以便纳入风险管理台账 |
| US-PM-036-02 | 作为 PM，我想要查看风险登记册，以便全面了解项目面临的风险 |

**Given-When-Then 验收标准**：

| 场景 | Given | When | Then |
|------|-------|------|------|
| 识别风险 | 任何项目成员 | 填写风险描述、类别、触发条件并提交 | 生成风险编号（`RISK-NNN`），状态 `IDENTIFIED` |
| 风险去重 | 已存在类似风险 | 提交时系统检查相似度 | 提示可能重复的风险条目 |
| 风险分类 | 提交风险时 | 选择风险类别 | 按类别聚合显示 |

**数据模型**：

```sql
CREATE TABLE pm_risk_register (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    risk_code       TEXT NOT NULL UNIQUE,          -- RISK-NNN
    category        TEXT NOT NULL,                 -- TECHNICAL/SCHEDULE/COST/RESOURCE/EXTERNAL/QUALITY
    description     TEXT NOT NULL,
    cause           TEXT,                          -- 风险成因
    trigger_condition TEXT,                        -- 触发条件
    probability     INTEGER,                       -- 1-5
    impact          INTEGER,                       -- 1-5
    risk_score      INTEGER,                       -- probability × impact
    risk_level      TEXT NOT NULL,                 -- LOW/MEDIUM/HIGH/CRITICAL
    status          TEXT NOT NULL DEFAULT 'IDENTIFIED', -- IDENTIFIED/ANALYZED/RESPONSE_PLANNED/ACTIVE/MITIGATED/CLOSED/OCCURRED
    identified_by   INTEGER NOT NULL,
    identified_date DATE NOT NULL,
    owner_id        INTEGER,                       -- 风险责任人
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

**风险等级划分**：

| 风险分值 | 等级 | 颜色 |
|---------|------|------|
| 1-4 | LOW | 绿色 |
| 5-9 | MEDIUM | 黄色 |
| 10-16 | HIGH | 橙色 |
| 17-25 | CRITICAL | 红色 |

**API Surface**：

| Method | Path | 说明 |
|--------|------|------|
| POST | `/api/pm/risks` | 创建风险条目 |
| GET | `/api/pm/risks?projectId=` | 风险登记册 |
| PUT | `/api/pm/risks/{id}` | 更新风险信息 |
| GET | `/api/pm/risk-register/{projectId}` | 完整风险登记册（含分析） |

---

### 9.4 功能需求：FR-PM-037 定性风险分析

**描述**：评估风险的概率和影响，计算风险分值，进行优先级排序。

**用户故事**：

| ID | 用户故事 |
|----|---------|
| US-PM-037-01 | 作为 PM，我想要对风险进行定性分析，以便确定风险优先级 |

**数据模型**：定性分析字段已集成在 `pm_risk_register` 中（probability, impact, risk_score, risk_level）。

```sql
CREATE TABLE pm_risk_assessment (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    risk_id         INTEGER NOT NULL REFERENCES pm_risk_register(id),
    assessment_date DATE NOT NULL,
    probability     INTEGER NOT NULL,              -- 1-5
    impact          INTEGER NOT NULL,              -- 1-5
    risk_score      INTEGER NOT NULL,
    risk_level      TEXT NOT NULL,
    assessment_notes TEXT,
    assessed_by     INTEGER NOT NULL,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

**概率-影响矩阵**：

| | 影响 1(极低) | 影响 2(低) | 影响 3(中) | 影响 4(高) | 影响 5(极高) |
|---|---|---|---|---|---|
| 概率 5(极高) | 5 | 10 | 15 | **20** | **25** |
| 概率 4(高) | 4 | 8 | 12 | **16** | **20** |
| 概率 3(中) | 3 | 6 | 9 | **12** | **15** |
| 概率 2(低) | 2 | 4 | 6 | 8 | 10 |
| 概率 1(极低) | 1 | 2 | 3 | 4 | 5 |

---

### 9.5 功能需求：FR-PM-038 定量风险分析

**描述**：对高优先级风险进行定量分析，评估对项目整体目标的影响（金额/天数）。

**数据模型**：

```sql
CREATE TABLE pm_risk_quantitative (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    risk_id         INTEGER NOT NULL REFERENCES pm_risk_register(id),
    analysis_method TEXT NOT NULL,                 -- MONTE_CARLO/DECISION_TREE/SENSITIVITY/EMV
    cost_impact_min DECIMAL(15,2),                 -- 最小成本影响
    cost_impact_max DECIMAL(15,2),                 -- 最大成本影响
    cost_impact_expected DECIMAL(15,2),            -- 期望成本影响
    schedule_impact_days DECIMAL(8,2),             -- 工期影响天数
    emv             DECIMAL(15,2),                 -- 预期货币价值
    tornado_data    TEXT,                          -- JSON：敏感性分析龙卷风图数据
    analysis_notes  TEXT,
    analyzed_by     INTEGER NOT NULL,
    analyzed_at     DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

### 9.6 功能需求：FR-PM-039 风险应对计划

**描述**：为每个已识别的风险制定应对策略和具体行动计划。

**用户故事**：

| ID | 用户故事 |
|----|---------|
| US-PM-039-01 | 作为 PM，我想要为高风险条目制定应对计划，以便降低风险发生的概率或影响 |

**Given-When-Then 验收标准**：

| 场景 | Given | When | Then |
|------|-------|------|------|
| 制定策略 | 风险已分析 | PM 选择应对策略（规避/转移/减轻/接受）并制定行动项 | 生成应对记录，状态 `RESPONSE_PLANNED` |
| 策略执行 | 应对计划已创建 | 责任人在截止日期前执行应对行动 | 状态更新，记录执行结果 |
| 残余风险 | 应对执行后 | PM 评估残余风险水平 | 记录残余概率和影响 |

**数据模型**：

```sql
CREATE TABLE pm_risk_response (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    risk_id         INTEGER NOT NULL REFERENCES pm_risk_register(id),
    strategy        TEXT NOT NULL,                 -- AVOID/TRANSFER/MITIGATE/ACCEPT/ESCALATE
    description     TEXT NOT NULL,
    action_items    TEXT,                          -- JSON：[{action, assignee, due_date, status}]
    owner_id        INTEGER NOT NULL,
    due_date        DATE,
    cost_estimate   DECIMAL(15,2),                 -- 应对措施成本
    status          TEXT NOT NULL DEFAULT 'PLANNED', -- PLANNED/IN_PROGRESS/COMPLETED/INEFFECTIVE
    residual_probability INTEGER,                   -- 残余概率
    residual_impact INTEGER,                        -- 残余影响
    residual_score  INTEGER,                        -- 残余风险分值
    result          TEXT,                          -- 应对措施执行结果
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by      TEXT,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

**风险应对策略**：

| 策略 | 适用场景 | 示例 |
|------|---------|------|
| 规避（Avoid） | 风险影响极大且可控 | 更换技术方案，取消某功能 |
| 转移（Transfer） | 可转由第三方承担 | 购买保险、外包、签订固定价格合同 |
| 减轻（Mitigate） | 可降低概率或影响 | 增加测试、原型验证、培训 |
| 接受（Accept） | 影响较小或无法处理 | 设置应急储备，不采取行动 |
| 升级（Escalate） | 超出项目层面 | 上报至项目组合或组织层面 |

---

### 9.7 功能需求：FR-PM-040 风险应对执行

**描述**：执行已计划的风险应对措施，跟踪执行状态和效果。

**API Surface**：

| Method | Path | 说明 |
|--------|------|------|
| POST | `/api/pm/risk-responses` | 创建风险应对 |
| PUT | `/api/pm/risk-responses/{id}` | 更新应对状态 |
| POST | `/api/pm/risk-responses/{id}/execute` | 执行应对措施 |

---

### 9.8 功能需求：FR-PM-041 风险监控台账

**描述**：持续监控风险状态，跟踪已识别风险、新风险和残余风险，生成风险台账报告。

**用户故事**：

| ID | 用户故事 |
|----|---------|
| US-PM-041-01 | 作为 PM，我想要查看风险台账，以便了解项目当前面临的所有风险及其状态 |
| US-PM-041-02 | 作为 SPONSOR，我想要查看高风险条目摘要，以便关注重大风险 |

**Given-When-Then 验收标准**：

| 场景 | Given | When | Then |
|------|-------|------|------|
| 风险触发 | 风险状态为 `ACTIVE` | 责任人标记风险已发生 | 状态变为 `OCCURRED`，启动应急预案 |
| 风险关闭 | 风险已减轻或不再相关 | PM 关闭风险 | 状态变为 `CLOSED`，记录关闭原因 |
| 台账生成 | 项目有已识别风险 | PM 生成风险台账 | 显示所有风险的当前状态、趋势和应对措施 |

**数据模型**：

```sql
CREATE TABLE pm_risk_monitor (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    risk_id         INTEGER NOT NULL REFERENCES pm_risk_register(id),
    monitor_date    DATE NOT NULL,
    current_status  TEXT NOT NULL,
    status_trend    TEXT,                          -- INCREASING/DECREASING/STABLE
    new_triggers    TEXT,                          -- JSON：新发现的触发条件
    notes           TEXT,
    monitored_by    INTEGER NOT NULL,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

**UI/UX**：

| 元素 | 说明 |
|------|------|
| 页面 | `src/views/pm/risk/` |
| 台账列表 | el-table，按风险分值排序，颜色标识等级 |
| 热力图 | 5×5 概率-影响矩阵气泡图，气泡大小=风险数 |
| 趋势图 | 风险数量按等级随时间变化的堆叠面积图 |
| 仪表盘 | 高/危急风险数量卡片 |

**API Surface**：

| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/pm/risk-dashboard/{projectId}` | 风险仪表盘数据 |
| GET | `/api/pm/risk-heatmap?projectId=` | 风险热力图数据 |
| GET | `/api/pm/risk-trend?projectId=` | 风险趋势数据 |
| POST | `/api/pm/risk-monitor-report?projectId=` | 生成风险台账报告 |

**WH PM 内置风险模板**：

| 风险类别 | 内置风险条目示例 |
|---------|----------------|
| TECHNICAL | SQLite 兼容性限制导致部分 SQL 语法不支持 |
| TECHNICAL | Flowable 审批流程在高并发下的稳定性 |
| SCHEDULE | 前后端联调效率低于预期 |
| RESOURCE | 关键角色（架构师）同时服务多个项目 |
| QUALITY | 测试覆盖率不足导致上线后缺陷率高 |

---

## 10. 采购管理（Procurement Management）

> **PMBOK 6th §12** | **PMBOK 7th Domains: Planning, Delivery, Stakeholder**

### 10.1 PMBOK 流程映射

| 流程 | 过程组 | 对应功能 |
|------|--------|---------|
| 12.1 规划采购管理 | 规划 | FR-PM-042 采购管理计划 |
| 12.2 实施采购 | 执行 | FR-PM-043 供应商与合同管理 |
| 12.3 控制采购 | 监控 | FR-PM-044 采购控制 |
| 12.4 结束采购 | 收尾 | FR-PM-045 采购收尾 |

---

### 10.2 功能需求：FR-PM-042 采购管理计划

**描述**：定义项目的采购需求、采购方式、合同类型、供应商选择标准和采购时间表。

**数据模型**：存储于 `pm_management_plan.procurement_plan` JSON 字段。

```json
{
  "procurement_items": [
    {
      "item": "云服务基础设施",
      "procurement_method": "competitive_bidding",
      "contract_type": "fixed_price",
      "estimated_cost": 50000,
      "planned_date": "2026-06-01"
    }
  ],
  "vendor_evaluation_criteria": {
    "technical_capability": 30,
    "price": 30,
    "past_performance": 20,
    "delivery_schedule": 20
  },
  "approval_threshold": 100000,
  "contract_template": "standard_service_agreement"
}
```

---

### 10.3 功能需求：FR-PM-043 供应商与合同管理

**描述**：管理供应商信息、采购订单、合同和交付物验收。

**数据模型**：

```sql
CREATE TABLE pm_vendor (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    vendor_code     TEXT NOT NULL UNIQUE,          -- VEN-NNN
    name            TEXT NOT NULL,
    contact_person  TEXT,
    contact_email   TEXT,
    contact_phone   TEXT,
    category        TEXT,                          -- 供应商类别
    rating          DECIMAL(3,2),                  -- 评分 1-5
    notes           TEXT,
    status          TEXT NOT NULL DEFAULT 'ACTIVE', -- ACTIVE/BLOCKED
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pm_procurement_order (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    order_code      TEXT NOT NULL UNIQUE,          -- PO-YYYY-NNN
    vendor_id       INTEGER NOT NULL REFERENCES pm_vendor(id),
    item_description TEXT NOT NULL,
    quantity        DECIMAL(10,2) NOT NULL,
    unit_price      DECIMAL(15,2) NOT NULL,
    total_amount    DECIMAL(15,2) NOT NULL,
    contract_type   TEXT NOT NULL,                 -- FIXED_PRICE/TM/COST_REIMBURSABLE
    order_date      DATE NOT NULL,
    expected_delivery_date DATE,
    actual_delivery_date DATE,
    status          TEXT NOT NULL DEFAULT 'PENDING', -- PENDING/CONFIRMED/IN_DELIVERY/DELIVERED/ACCEPTED/REJECTED
    acceptance_criteria TEXT,
    acceptance_date DATE,
    acceptance_comment TEXT,
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by      TEXT,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

**API Surface**：

| Method | Path | 说明 |
|--------|------|------|
| POST | `/api/pm/vendors` | 创建供应商 |
| GET | `/api/pm/vendors` | 供应商列表 |
| POST | `/api/pm/procurement-orders` | 创建采购订单 |
| GET | `/api/pm/procurement-orders?projectId=` | 采购订单列表 |
| PUT | `/api/pm/procurement-orders/{id}` | 更新采购订单 |
| POST | `/api/pm/procurement-orders/{id}/accept` | 验收交付物 |

---

### 10.4 功能需求：FR-PM-044 采购控制

**描述**：监控采购执行情况，跟踪交付进度、质量和成本偏差。

**数据模型**：

```sql
CREATE TABLE pm_procurement_variance (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id        INTEGER NOT NULL REFERENCES pm_procurement_order(id),
    variance_type   TEXT NOT NULL,                 -- SCHEDULE/COST/QUALITY
    planned_value   DECIMAL(15,2),
    actual_value    DECIMAL(15,2),
    variance_amount DECIMAL(15,2),
    description     TEXT,
    status          TEXT NOT NULL DEFAULT 'OPEN',  -- OPEN/RESOLVED/ACCEPTED
    detected_date   DATE NOT NULL,
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

### 10.5 功能需求：FR-PM-045 采购收尾

**描述**：完成采购合同的收尾，包括最终验收、结算、经验教训归档。

**数据模型**：

```sql
CREATE TABLE pm_procurement_closure (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id        INTEGER NOT NULL REFERENCES pm_procurement_order(id),
    closure_type    TEXT NOT NULL,                 -- NORMAL/TERMINATED
    final_settlement DECIMAL(15,2),                -- 最终结算金额
    lessons_learned TEXT,                          -- JSON
    closure_date    DATE NOT NULL,
    closure_comment TEXT,
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

## 11. 干系人管理（Stakeholder Management）

> **PMBOK 6th §13** | **PMBOK 7th Domains: Stakeholder, Team**

### 11.1 PMBOK 流程映射

| 流程 | 过程组 | 对应功能 |
|------|--------|---------|
| 13.1 识别干系人 | 启动 | FR-PM-046 干系人识别 |
| 13.2 规划干系人参与 | 规划 | FR-PM-047 干系人参与计划 |
| 13.3 管理干系人参与 | 执行 | FR-PM-048 干系人参与管理 |
| 13.4 监督干系人参与 | 监控 | FR-PM-049 干系人满意度评估 |

---

### 11.2 功能需求：FR-PM-046 干系人识别

**描述**：识别所有可能影响项目或被项目影响的个人或组织，分析其利益、参与度和影响力。

**用户故事**：

| ID | 用户故事 |
|----|---------|
| US-PM-046-01 | 作为 PM，我想要识别和记录项目干系人，以便制定针对性的沟通策略 |
| US-PM-046-02 | 作为 PM，我想要使用权力-利益矩阵对干系人分类，以便确定管理优先级 |

**Given-When-Then 验收标准**：

| 场景 | Given | When | Then |
|------|-------|------|------|
| 登记干系人 | PM 已进入干系人管理页面 | 填写干系人信息（名称、角色、组织、权力/利益等级） | 生成干系人编号（`STAKEHOLDER-NNN`） |
| 矩阵分类 | 干系人已登记 | PM 设置权力等级（高/低）和利益等级（高/低） | 干系人自动落入四象限之一 |
| 参与度评估 | 干系人已登记 | PM 评估当前参与度和期望参与度 | 系统计算差距，提示需改进的干系人 |

**数据模型**：

```sql
CREATE TABLE pm_stakeholder (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    stakeholder_code TEXT NOT NULL UNIQUE,         -- STAKEHOLDER-NNN
    name            TEXT NOT NULL,
    organization    TEXT,
    role            TEXT NOT NULL,                 -- SPONSOR/USER/CUSTOMER/TEAM_MEMBER/VENDOR/REGULATOR
    power_level     TEXT NOT NULL,                 -- HIGH/LOW
    interest_level  TEXT NOT NULL,                 -- HIGH/LOW
    quadrant        TEXT NOT NULL,                 -- MANAGE_CLOSELY/KEEP_SATISFIED/KEEP_INFORMED/MONITOR
    current_engagement TEXT NOT NULL,              -- UNWARE/RESISTANT/NEUTRAL/SUPPORTIVE/LEADING
    desired_engagement TEXT NOT NULL,              -- UNWARE/RESISTANT/NEUTRAL/SUPPORTIVE/LEADING
    engagement_gap  TEXT,                          -- 差距描述
    influence_score INTEGER,                       -- 1-10
    expectations    TEXT,                          -- JSON：期望列表
    notes           TEXT,
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by      TEXT,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

**权力-利益矩阵四象限**：

| 权力 | 利益 | 策略 |
|------|------|------|
| 高 | 高 | **重点管理**（Manage Closely） |
| 高 | 低 | **令其满意**（Keep Satisfied） |
| 低 | 高 | **随时告知**（Keep Informed） |
| 低 | 低 | **监督**（Monitor） |

---

### 11.3 功能需求：FR-PM-047 干系人参与计划

**描述**：制定策略以促进干系人的有效参与，缩小当前参与度与期望参与度的差距。

**数据模型**：

```sql
CREATE TABLE pm_stakeholder_engagement_plan (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    stakeholder_id  INTEGER NOT NULL REFERENCES pm_stakeholder(id),
    strategy        TEXT NOT NULL,                 -- 参与策略描述
    action_items    TEXT,                          -- JSON：[{action, assignee, due_date}]
    communication_frequency TEXT,                  -- 沟通频率
    communication_method TEXT,                     -- 沟通方式
    success_criteria TEXT,                         -- 成功标准
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by      TEXT,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

### 11.4 功能需求：FR-PM-048 干系人参与管理

**描述**：执行干系人参与计划，记录与干系人的互动和参与效果。

**数据模型**：干系人互动记录复用 `pm_communication_log` 表，通过 `participants` 关联。

```sql
CREATE TABLE pm_stakeholder_interaction (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    stakeholder_id  INTEGER NOT NULL REFERENCES pm_stakeholder(id),
    interaction_type TEXT NOT NULL,                -- MEETING/PRESENTATION/DEMO/REPORT/AD_HOC
    interaction_date DATE NOT NULL,
    summary         TEXT NOT NULL,
    outcome         TEXT,                          -- 互动结果
    engagement_shift TEXT,                         -- 参与度变化
    follow_up_actions TEXT,                        -- JSON：跟进项
    created_by      TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

### 11.5 功能需求：FR-PM-049 干系人满意度评估

**描述**：定期评估干系人对项目的满意度，识别改进机会。

**用户故事**：

| ID | 用户故事 |
|----|---------|
| US-PM-049-01 | 作为 PM，我想要调查干系人满意度，以便及时调整管理策略 |

**Given-When-Then 验收标准**：

| 场景 | Given | When | Then |
|------|-------|------|------|
| 发起调查 | 项目执行中 | PM 发起满意度调查，选择评估维度和目标干系人 | 生成调查记录 |
| 评分 | 调查已发起 | PM 或干系人对各维度评分（1-5） | 系统计算综合满意度 |
| 趋势分析 | 有多次调查记录 | PM 查看趋势图 | 显示满意度随时间的变化 |

**数据模型**：

```sql
CREATE TABLE pm_stakeholder_satisfaction (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    evaluation_period TEXT NOT NULL,               -- "2026-Q2"
    stakeholder_id  INTEGER REFERENCES pm_stakeholder(id),
    dimensions      TEXT NOT NULL,                 -- JSON：{dimension: score}
    overall_score   DECIMAL(3,2),                  -- 1-5
    feedback        TEXT,
    improvement_actions TEXT,                      -- JSON
    evaluated_by    INTEGER NOT NULL,
    evaluated_at    DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

**评估维度**：

| 维度 | 说明 |
|------|------|
| 沟通满意度 | 信息是否及时、准确地传达 |
| 进度满意度 | 项目是否按计划推进 |
| 质量满意度 | 交付物是否符合预期质量标准 |
| 风险透明度 | 风险是否被有效识别和沟通 |
| 总体满意度 | 对项目的整体评价 |

**API Surface**：

| Method | Path | 说明 |
|--------|------|------|
| POST | `/api/pm/stakeholders` | 创建干系人 |
| GET | `/api/pm/stakeholders?projectId=` | 干系人列表 |
| PUT | `/api/pm/stakeholders/{id}` | 更新干系人信息 |
| GET | `/api/pm/stakeholder-matrix/{projectId}` | 权力-利益矩阵数据 |
| POST | `/api/pm/stakeholder-satisfaction` | 记录满意度评估 |
| GET | `/api/pm/stakeholder-satisfaction?projectId=` | 满意度历史及趋势 |

---

## 12. 全局约定

### 12.1 编号规则

| 实体 | 编号格式 | 示例 |
|------|---------|------|
| 项目章程 | `CHARTER-YYYY-NNN` | CHARTER-2026-001 |
| 项目编号 | `PRJ-YYYY-NNN` | PRJ-2026-001 |
| 需求编号 | `REQ-NNN` | REQ-001 |
| 活动编号 | `ACT-NNN` | ACT-001 |
| WBS 编码 | 分层编号 | 1.1.2.3 |
| 风险编号 | `RISK-NNN` | RISK-001 |
| 变更编号 | `CHANGE-NNN` | CHANGE-001 |
| 缺陷编号 | `DEFECT-NNN` | DEFECT-001 |
| 干系人编号 | `STAKEHOLDER-NNN` | STAKEHOLDER-001 |
| 审计编号 | `AUDIT-NNN` | AUDIT-001 |
| 问题编号 | `ISSUE-NNN` | ISSUE-001 |
| 采购订单 | `PO-YYYY-NNN` | PO-2026-001 |
| 供应商编号 | `VEN-NNN` | VEN-001 |
| 预算编号 | `BUDGET-YYYY-NNN` | BUDGET-2026-001 |

### 12.2 通用状态枚举

| 状态字段 | 可用值 |
|---------|--------|
| 审批状态 | DRAFT, PENDING_APPROVAL, APPROVED, REJECTED |
| 通用进度状态 | NOT_STARTED, IN_PROGRESS, COMPLETED, CANCELLED |
| 项目状态 | INITIATED, PLANNING, EXECUTING, MONITORING, CLOSING, CLOSED |

### 12.3 审计字段约定

所有表均包含以下审计字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `created_by` | TEXT | 创建人 ID |
| `created_at` | DATETIME | 创建时间（自动） |
| `updated_by` | TEXT | 最后修改人 ID |
| `updated_at` | DATETIME | 最后修改时间（自动） |

### 12.4 通用 UI 约定

| 约定项 | 说明 |
|--------|------|
| 列表页 | el-table + 分页 + 筛选 + 搜索栏，操作列含编辑/删除/查看 |
| 表单页 | el-form + 分步（>5 字段时） + 表单校验 + 草稿保存 |
| 详情页 | el-descriptions + 关联子表 + 操作历史时间线 |
| 状态展示 | el-tag 按状态着色 |
| 确认操作 | el-popconfirm 用于删除、提交、审批等危险操作 |
| 空状态 | el-empty + 引导操作按钮 |

### 12.5 审计与日志

| 要求 | 说明 |
|------|------|
| 操作日志 | 所有状态变更、审批操作记录到 `pm_audit_log` |
| 数据备份 | SQLite 文件级备份，每日自动 |
| 删除策略 | 软删除（`deleted` 标志），不物理删除业务数据 |

```sql
CREATE TABLE pm_audit_log (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id      INTEGER NOT NULL REFERENCES pm_project_charter(id),
    entity_type     TEXT NOT NULL,                 -- 操作的实体类型
    entity_id       INTEGER NOT NULL,              -- 操作的实体 ID
    action          TEXT NOT NULL,                 -- CREATE/UPDATE/DELETE/SUBMIT/APPROVE/REJECT
    old_values      TEXT,                          -- JSON：变更前的值
    new_values      TEXT,                          -- JSON：变更后的值
    performed_by    TEXT NOT NULL,
    performed_at    DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

## 附录 A：需求跟踪矩阵（RTM）

### PMBOK → 功能需求映射

| PMBOK 6th 流程 | 本文档功能编号 | 功能名称 | PMBOK 7th 绩效域 |
|---------------|--------------|---------|-----------------|
| 4.1 制定项目章程 | FR-PM-001 | 项目立项 | Planning |
| 4.2 制定项目管理计划 | FR-PM-002 | 项目管理计划 | Planning |
| 4.3 指导与管理项目工作 | FR-PM-003 | 工作执行跟踪 | Project Work |
| 4.4 管理项目知识 | FR-PM-004 | 项目知识库 | Project Work |
| 4.5 实施整体变更控制 | FR-PM-005 | 变更控制流程 | Project Work, Measurement |
| 4.6 结束项目或阶段 | FR-PM-006 | 项目收尾 | Delivery |
| 5.1 规划范围管理 | FR-PM-007 | 范围管理计划 | Planning |
| 5.2 收集需求 | FR-PM-008 | 需求管理 | Planning |
| 5.3 定义范围 | FR-PM-009 | 项目范围说明书 | Planning |
| 5.4 创建 WBS | FR-PM-010 | WBS 管理 | Planning, Project Work |
| 5.5 确认范围 | FR-PM-011 | 范围确认 | Delivery |
| 5.6 控制范围 | FR-PM-012 | 范围控制 | Measurement |
| 6.1 规划进度管理 | FR-PM-013 | 进度管理计划 | Planning |
| 6.2 定义活动 | FR-PM-014 | 活动定义 | Planning |
| 6.3 排列活动顺序 | FR-PM-015 | 活动排序 | Planning, Project Work |
| 6.4 估算活动持续时间 | FR-PM-016 | 活动工期估算 | Planning |
| 6.5 制定进度计划 | FR-PM-017 | 进度计划与甘特图 | Planning, Measurement |
| 6.6 控制进度 | FR-PM-018 | 进度控制与偏差分析 | Measurement |
| 7.1 规划成本管理 | FR-PM-019 | 成本管理计划 | Planning |
| 7.2 估算成本 | FR-PM-020 | 成本估算 | Planning |
| 7.3 制定预算 | FR-PM-021 | 预算制定 | Planning, Measurement |
| 7.4 控制成本 | FR-PM-022 | 成本控制与挣值分析 | Measurement |
| 8.1 规划质量管理 | FR-PM-023 | 质量管理计划 | Planning |
| 8.2 管理质量 | FR-PM-024 | 质量保证 | Project Work, Delivery |
| 8.3 控制质量 | FR-PM-025 | 质量控制 | Measurement |
| 9.1 规划资源管理 | FR-PM-026 | 资源管理计划 | Planning |
| 9.2 估算活动资源 | FR-PM-027 | 资源估算 | Planning |
| 9.3 获取资源 | FR-PM-028 | 资源分配 | Team, Project Work |
| 9.4 建设团队 | FR-PM-029 | 团队管理 | Team |
| 9.5 管理团队 | FR-PM-030 | 团队绩效跟踪 | Team, Measurement |
| 9.6 控制资源 | FR-PM-031 | 资源使用监控 | Measurement |
| 10.1 规划沟通管理 | FR-PM-032 | 沟通管理计划 | Stakeholder, Team |
| 10.2 管理沟通 | FR-PM-033 | 沟通执行与记录 | Stakeholder, Team |
| 10.3 监督沟通 | FR-PM-034 | 沟通效果评估 | Measurement |
| 11.1 规划风险管理 | FR-PM-035 | 风险管理计划 | Uncertainty, Planning |
| 11.2 识别风险 | FR-PM-036 | 风险识别 | Uncertainty |
| 11.3 实施定性风险分析 | FR-PM-037 | 定性风险分析 | Uncertainty, Planning |
| 11.4 实施定量风险分析 | FR-PM-038 | 定量风险分析 | Uncertainty |
| 11.5 规划风险应对 | FR-PM-039 | 风险应对计划 | Uncertainty, Planning |
| 11.6 实施风险应对 | FR-PM-040 | 风险应对执行 | Uncertainty, Project Work |
| 11.7 监督风险 | FR-PM-041 | 风险监控台账 | Uncertainty, Measurement |
| 12.1 规划采购管理 | FR-PM-042 | 采购管理计划 | Planning, Delivery |
| 12.2 实施采购 | FR-PM-043 | 供应商与合同管理 | Delivery, Stakeholder |
| 12.3 控制采购 | FR-PM-044 | 采购控制 | Measurement |
| 12.4 结束采购 | FR-PM-045 | 采购收尾 | Delivery |
| 13.1 识别干系人 | FR-PM-046 | 干系人识别 | Stakeholder |
| 13.2 规划干系人参与 | FR-PM-047 | 干系人参与计划 | Stakeholder, Planning |
| 13.3 管理干系人参与 | FR-PM-048 | 干系人参与管理 | Stakeholder |
| 13.4 监督干系人参与 | FR-PM-049 | 干系人满意度评估 | Stakeholder, Measurement |

---

## 附录 B：数据库 Schema 汇总

### B.1 实体关系总览

```
pm_project_charter (1)
  ├── pm_project_charter → (1:1) pm_management_plan
  ├── pm_project_charter → (1:1) pm_scope_statement
  ├── pm_project_charter → (1:N) pm_wbs_element (树形)
  ├── pm_project_charter → (1:N) pm_activity
  ├── pm_project_charter → (1:N) pm_requirement
  ├── pm_project_charter → (1:N) pm_rtm
  ├── pm_project_charter → (1:N) pm_cost_estimate
  ├── pm_project_charter → (1:N) pm_budget
  ├── pm_project_charter → (1:N) pm_actual_cost
  ├── pm_project_charter → (1:N) pm_quality_audit
  ├── pm_project_charter → (1:N) pm_quality_defect
  ├── pm_project_charter → (1:N) pm_resource_assignment
  ├── pm_project_charter → (1:N) pm_communication_log
  ├── pm_project_charter → (1:N) pm_meeting
  ├── pm_project_charter → (1:N) pm_report
  ├── pm_project_charter → (1:N) pm_risk_register
  ├── pm_project_charter → (1:N) pm_risk_response
  ├── pm_project_charter → (1:N) pm_procurement_order
  ├── pm_project_charter → (1:N) pm_stakeholder
  ├── pm_project_charter → (1:N) pm_change_request
  ├── pm_project_charter → (1:N) pm_issue_log
  ├── pm_project_charter → (1:N) pm_work_log
  ├── pm_project_charter → (1:N) pm_knowledge_base
  ├── pm_project_charter → (1:N) pm_project_closure
  ├── pm_project_charter → (1:N) pm_schedule_variance
  ├── pm_project_charter → (1:N) pm_evm_analysis
  └── pm_project_charter → (1:N) pm_audit_log

pm_activity (1)
  ├── pm_activity → (1:N) pm_activity_dependency (自引用)
  ├── pm_activity → (1:1) pm_activity_estimation
  └── pm_activity → (1:N) pm_work_log

pm_risk_register (1)
  ├── pm_risk_register → (1:N) pm_risk_assessment
  ├── pm_risk_register → (1:N) pm_risk_response
  └── pm_risk_register → (1:N) pm_risk_quantitative

pm_vendor (1) → (1:N) pm_procurement_order
```

### B.2 表清单

| # | 表名 | 所属知识领域 | 行数估算 |
|---|------|------------|---------|
| 1 | pm_project_charter | 整合管理 | - |
| 2 | pm_management_plan | 整合管理 | - |
| 3 | pm_plan_baseline | 整合管理 | - |
| 4 | pm_work_log | 整合管理 | - |
| 5 | pm_issue_log | 整合管理 | - |
| 6 | pm_knowledge_base | 整合管理 | - |
| 7 | pm_change_request | 整合管理 | - |
| 8 | pm_project_closure | 整合管理 | - |
| 9 | pm_requirement | 范围管理 | - |
| 10 | pm_rtm | 范围管理 | - |
| 11 | pm_scope_statement | 范围管理 | - |
| 12 | pm_wbs_element | 范围管理 | - |
| 13 | pm_scope_verification | 范围管理 | - |
| 14 | pm_scope_variance | 范围管理 | - |
| 15 | pm_activity | 进度管理 | - |
| 16 | pm_activity_dependency | 进度管理 | - |
| 17 | pm_critical_path | 进度管理 | - |
| 18 | pm_activity_estimation | 进度管理 | - |
| 19 | pm_schedule_variance | 进度管理 | - |
| 20 | pm_schedule_alert | 进度管理 | - |
| 21 | pm_cost_estimate | 成本管理 | - |
| 22 | pm_budget | 成本管理 | - |
| 23 | pm_actual_cost | 成本管理 | - |
| 24 | pm_evm_analysis | 成本管理 | - |
| 25 | pm_quality_audit | 质量管理 | - |
| 26 | pm_quality_defect | 质量管理 | - |
| 27 | pm_resource_estimate | 资源管理 | - |
| 28 | pm_resource_assignment | 资源管理 | - |
| 29 | pm_resource_calendar | 资源管理 | - |
| 30 | pm_team_skill | 资源管理 | - |
| 31 | pm_training_record | 资源管理 | - |
| 32 | pm_resource_utilization | 资源管理 | - |
| 33 | pm_communication_log | 沟通管理 | - |
| 34 | pm_meeting | 沟通管理 | - |
| 35 | pm_report | 沟通管理 | - |
| 36 | pm_comm_effectiveness | 沟通管理 | - |
| 37 | pm_risk_register | 风险管理 | - |
| 38 | pm_risk_assessment | 风险管理 | - |
| 39 | pm_risk_quantitative | 风险管理 | - |
| 40 | pm_risk_response | 风险管理 | - |
| 41 | pm_risk_monitor | 风险管理 | - |
| 42 | pm_vendor | 采购管理 | - |
| 43 | pm_procurement_order | 采购管理 | - |
| 44 | pm_procurement_variance | 采购管理 | - |
| 45 | pm_procurement_closure | 采购管理 | - |
| 46 | pm_stakeholder | 干系人管理 | - |
| 47 | pm_stakeholder_engagement_plan | 干系人管理 | - |
| 48 | pm_stakeholder_interaction | 干系人管理 | - |
| 49 | pm_stakeholder_satisfaction | 干系人管理 | - |
| 50 | pm_audit_log | 全局 | - |

---

## 附录 C：Flowable 流程定义清单

| 流程编码 | 流程名称 | 触发场景 | 审批节点 |
|---------|---------|---------|---------|
| `PM_CHARTER_APPROVAL` | 项目章程审批 | 章程提交 | PM → SPONSOR |
| `PM_CHANGE_CONTROL` | 变更控制流程 | 变更请求提交 | PM → 自动影响分析 → CCB |
| `PM_BUDGET_APPROVAL` | 预算审批 | 预算提交 | PM → SPONSOR |
| `PM_MILESTONE_CHANGE` | 里程碑变更 | 里程碑变更申请 | PM → SPONSOR |
| `PM_TIMESHEET` | 工时填报审批 | 工时提交 | 成员 → PM |
| `PM_BUSINESS_TRIP_APPLY` | 出差申请 | 出差申请提交 | 成员 → PM → HR |
| `PM_CLOSURE_ACCEPTANCE` | 项目验收 | 收尾提交 | PM → SPONSOR → QA |
| `PM_PROCUREMENT_ACCEPTANCE` | 采购验收 | 交付物提交 | PM → 验收人 |
| `PM_DEFECT_REOPEN` | 缺陷重开审批 | 缺陷验证不通过 | QA → PM |
| `PM_RISK_ESCALATION` | 风险升级 | 高风险自动触发 | PM → SPONSOR |

---

## 附录 D：API 完整清单

### 整合管理

| Method | Path | 功能编号 |
|--------|------|---------|
| GET | `/api/pm/charters` | FR-PM-001 |
| POST | `/api/pm/charters` | FR-PM-001 |
| GET | `/api/pm/charters/{id}` | FR-PM-001 |
| PUT | `/api/pm/charters/{id}` | FR-PM-001 |
| POST | `/api/pm/charters/{id}/submit` | FR-PM-001 |
| POST | `/api/pm/charters/{id}/approve` | FR-PM-001 |
| POST | `/api/pm/charters/{id}/reject` | FR-PM-001 |
| GET | `/api/pm/plans` | FR-PM-002 |
| POST | `/api/pm/plans` | FR-PM-002 |
| PUT | `/api/pm/plans/{id}` | FR-PM-002 |
| POST | `/api/pm/plans/{id}/baseline` | FR-PM-002 |
| GET | `/api/pm/baselines` | FR-PM-002 |
| POST | `/api/pm/work-logs` | FR-PM-003 |
| GET | `/api/pm/work-logs` | FR-PM-003 |
| POST | `/api/pm/issues` | FR-PM-003 |
| PUT | `/api/pm/issues/{id}` | FR-PM-003 |
| GET | `/api/pm/dashboard/{projectId}` | FR-PM-003 |
| GET/POST | `/api/pm/knowledge-base` | FR-PM-004 |
| POST | `/api/pm/changes` | FR-PM-005 |
| GET | `/api/pm/changes` | FR-PM-005 |
| PUT | `/api/pm/changes/{id}` | FR-PM-005 |
| POST | `/api/pm/changes/{id}/approve` | FR-PM-005 |
| POST | `/api/pm/changes/{id}/reject` | FR-PM-005 |
| POST | `/api/pm/closures` | FR-PM-006 |
| GET | `/api/pm/closures/{projectId}` | FR-PM-006 |
| POST | `/api/pm/closures/{id}/accept` | FR-PM-006 |
| POST | `/api/pm/closures/{id}/close` | FR-PM-006 |

### 范围管理

| Method | Path | 功能编号 |
|--------|------|---------|
| POST/GET | `/api/pm/requirements` | FR-PM-008 |
| PUT | `/api/pm/requirements/{id}` | FR-PM-008 |
| GET | `/api/pm/rtm` | FR-PM-008 |
| POST | `/api/pm/rtm` | FR-PM-008 |
| GET/POST/PUT/DELETE | `/api/pm/wbs` | FR-PM-010 |
| POST | `/api/pm/wbs/validate` | FR-PM-010 |
| GET | `/api/pm/scope-variances` | FR-PM-012 |
| POST | `/api/pm/scope-variances` | FR-PM-012 |
| GET | `/api/pm/scope-health/{projectId}` | FR-PM-012 |

### 进度管理

| Method | Path | 功能编号 |
|--------|------|---------|
| GET/POST/PUT/DELETE | `/api/pm/activities` | FR-PM-014 |
| GET/POST/DELETE | `/api/pm/dependencies` | FR-PM-015 |
| POST | `/api/pm/critical-path/calculate` | FR-PM-015 |
| GET | `/api/pm/critical-path` | FR-PM-015 |
| GET | `/api/pm/gantt` | FR-PM-017 |
| PUT | `/api/pm/schedule/bulk-update` | FR-PM-017 |
| POST | `/api/pm/schedule/baseline-compare` | FR-PM-017 |
| GET/POST | `/api/pm/schedule-variance` | FR-PM-018 |
| GET | `/api/pm/schedule-alerts` | FR-PM-018 |
| PUT | `/api/pm/schedule-alerts/{id}/resolve` | FR-PM-018 |

### 成本管理

| Method | Path | 功能编号 |
|--------|------|---------|
| GET/POST | `/api/pm/budgets` | FR-PM-021 |
| POST | `/api/pm/budgets/{id}/approve` | FR-PM-021 |
| GET/POST | `/api/pm/actual-costs` | FR-PM-021 |
| POST | `/api/pm/evm/calculate` | FR-PM-022 |
| GET | `/api/pm/evm` | FR-PM-022 |
| GET | `/api/pm/cost-dashboard/{projectId}` | FR-PM-022 |

### 质量管理

| Method | Path | 功能编号 |
|--------|------|---------|
| GET/POST/PUT | `/api/pm/quality-audits` | FR-PM-024 |
| GET/POST/PUT | `/api/pm/defects` | FR-PM-025 |
| GET | `/api/pm/quality-dashboard/{projectId}` | FR-PM-025 |

### 资源管理

| Method | Path | 功能编号 |
|--------|------|---------|
| GET/POST/PUT | `/api/pm/resource-assignments` | FR-PM-028 |
| GET | `/api/pm/resource-heatmap` | FR-PM-028 |
| POST | `/api/pm/resource-conflicts/detect` | FR-PM-028 |
| GET | `/api/pm/team-performance` | FR-PM-030 |
| GET | `/api/pm/member-performance/{memberId}` | FR-PM-030 |

### 沟通管理

| Method | Path | 功能编号 |
|--------|------|---------|
| GET/POST | `/api/pm/comm-logs` | FR-PM-033 |
| GET/POST/PUT | `/api/pm/meetings` | FR-PM-033 |
| POST | `/api/pm/reports/generate` | FR-PM-033 |
| GET | `/api/pm/reports` | FR-PM-033 |

### 风险管理

| Method | Path | 功能编号 |
|--------|------|---------|
| GET/POST/PUT | `/api/pm/risks` | FR-PM-036 |
| GET | `/api/pm/risk-register/{projectId}` | FR-PM-036 |
| GET/POST/PUT | `/api/pm/risk-responses` | FR-PM-039/040 |
| POST | `/api/pm/risk-responses/{id}/execute` | FR-PM-040 |
| GET | `/api/pm/risk-dashboard/{projectId}` | FR-PM-041 |
| GET | `/api/pm/risk-heatmap` | FR-PM-041 |
| GET | `/api/pm/risk-trend` | FR-PM-041 |
| POST | `/api/pm/risk-monitor-report` | FR-PM-041 |

### 采购管理

| Method | Path | 功能编号 |
|--------|------|---------|
| GET/POST | `/api/pm/vendors` | FR-PM-043 |
| GET/POST/PUT | `/api/pm/procurement-orders` | FR-PM-043 |
| POST | `/api/pm/procurement-orders/{id}/accept` | FR-PM-043 |

### 干系人管理

| Method | Path | 功能编号 |
|--------|------|---------|
| GET/POST/PUT | `/api/pm/stakeholders` | FR-PM-046 |
| GET | `/api/pm/stakeholder-matrix/{projectId}` | FR-PM-046 |
| GET/POST | `/api/pm/stakeholder-satisfaction` | FR-PM-049 |

### 全局

| Method | Path | 功能编号 |
|--------|------|---------|
| GET | `/api/pm/audit-log` | §12.5 |

---

## 附录 E：前端页面路由清单

| 路由路径 | 页面文件 | 对应章节 |
|---------|---------|---------|
| `/pm/dashboard` | `src/views/pm/dashboard/index.vue` | 全局仪表盘 |
| `/pm/charter` | `src/views/pm/charter/index.vue` | §2.2 |
| `/pm/plan` | `src/views/pm/plan/index.vue` | §2.3 |
| `/pm/wbs` | `src/views/pm/wbs/index.vue` | §3.5 |
| `/pm/requirements` | `src/views/pm/requirements/index.vue` | §3.3 |
| `/pm/schedule` | `src/views/pm/schedule/index.vue` | §4.6 |
| `/pm/cost` | `src/views/pm/cost/index.vue` | §5.5 |
| `/pm/quality` | `src/views/pm/quality/index.vue` | §6.3-6.4 |
| `/pm/resource` | `src/views/pm/resource/index.vue` | §7.4 |
| `/pm/communication` | `src/views/pm/communication/index.vue` | §8.3 |
| `/pm/risk` | `src/views/pm/risk/index.vue` | §9.8 |
| `/pm/procurement` | `src/views/pm/procurement/index.vue` | §10.3 |
| `/pm/stakeholder` | `src/views/pm/stakeholder/index.vue` | §11.2 |
| `/pm/change` | `src/views/pm/change/index.vue` | §2.6 |
| `/pm/closure` | `src/views/pm/closure/index.vue` | §2.7 |

---

*文档版本 V1.0.0 | 2026-05-03 | SRS-WH-PM-001*
