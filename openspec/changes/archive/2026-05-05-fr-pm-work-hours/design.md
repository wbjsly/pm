## Context

当前系统已有 `pm_work_log` 表（定义于 `011-pm-integration.sql`），包含工时录入所需的全部字段（LOG_DATE、HOURS_WORKED、PROJECT_ID、STATUS 等），但尚未实现对应的 Java 实体、Controller 或前端页面。系统同时有 `pm_resource_calendar` 表用于个人请假管理，与系统级工作日历是不同概念，需要新增 `wh_sys_work_calendar` 表。

系统现有模式：
- 实体继承 `BaseEntity`，字段使用 `String` 类型
- DAO 继承 `BaseMapper<Entity>`，自定义查询用 `@Select` 注解
- BO 层使用 `@Service`，分页用 MyBatis-Plus Page
- 前端使用 Vue3 Composition API，KeepAlive 缓存
- 菜单通过 SidebarMenu.vue 的 `menuItems` 数组 + 路由 `meta.perm` 控制权限
- 审批采用状态机模式（DRAFT/PENDING/APPROVED/REJECTED），非 BPMN

## Goals / Non-Goals

**Goals:**
- 实现工时录入月历视图，支持按日录入、修改、删除工时
- 实现工时审批页面，支持逐条和批量审批
- 实现工作日历管理，支持全年一键生成和手动调整
- 实现主页工时统计，展示缺口指标
- 复用 `pm_work_log` 表，不修改表结构（仅加索引）

**Non-Goals:**
- 不实现工时审批的 BPMN 流程（使用轻量级状态机）
- 不实现 WBS 任务级别的工时关联（只关联到项目）
- 不实现加班时长统计、调休假等高级功能
- 不实现工时的定时自动提交或提醒

## Decisions

### 1. 审批模式：轻量级状态机 vs Flowable BPMN

**Decision**: 使用轻量级状态机（DRAFT → APPROVED/REJECTED），直接在 Bo 层控制状态流转。

**Rationale**: 工时审批只有单节点（项目经理确认），不需要多角色、多步骤审批。系统中预算审批使用 BPMN 是因为需要 Sponsor 角色匹配和流程监听，工时审批无此复杂度。

**Alternatives considered**: 
- Flowable BPMN：可复用已有的流程引擎和 endEvent listener，但对单节点审批过度设计
- 直接字段更新：最简单但缺少审计追踪

### 2. 数据库：复用 pm_work_log vs 新建表

**Decision**: 复用 `pm_work_log` 表，新增索引提升查询性能。

**Rationale**: 该表已有所有必需字段（PROJECT_ID、LOG_DATE、HOURS_WORKED、STATUS、BLOCKER_REASON、CREATE_BY）。STATUS 字段从"日志状态"转义为"审批状态"（DRAFT/APPROVED/REJECTED），语义自然匹配。

### 3. 工作日历存储：每日一条记录

**Decision**: `wh_sys_work_calendar` 表每天一条独立记录，而非范围存储。

**Rationale**: 查询简单（按 CALENDAR_DATE 精确匹配），支持个别日期特殊设置（如某天半天），便于工时统计时 JOIN 查询。

### 4. 工时录入项目权限：PM_ID / ownerId / plannedOwnerId

**Decision**: 用户可录入工时的项目 = 项目章程 PM_ID == 用户 ID，或 WBS 元素 OWNER_ID / PLANNED_OWNER_ID == 用户 ID。

**Rationale**: 利用现有字段，不需要新增分配关系表。覆盖项目经理和任务负责人两种角色。

### 5. 工时缺口统计：DRAFT 计入

**Decision**: 统计实际已录工时包括 DRAFT 和 APPROVED 状态，REJECTED 也计入（反映填报行为）。

**Rationale**: 缺口统计的目的是反映"是否按时填报"，而非"审批是否通过"。已录入但被驳回的记录也说明员工有填报行为。

### 6. 月历组件实现

**Decision**: 前端自研月历组件（使用 el-table 7列布局），不使用第三方日历库。

**Rationale**: 需求特殊（每个格子内按项目分行、状态颜色、周小计），第三方库定制成本高。el-table 的 7 列方案与现有系统 UI 风格一致。

### 7. 批量审批权限过滤

**Decision**: 后端批量审批时自动过滤无权审批的记录，而非前端拦截。

**Rationale**: 前端可能伪造请求，后端校验更安全。返回实际处理数量和跳过的数量，给用户明确反馈。

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| `pm_work_log` 表 STATUS 字段语义变化（原为日志状态，现为审批状态） | 代码注释中明确标注，不影响已有功能（因该表之前无 Java 实现） |
| 工作日历数据量大（365天/年 × N年） | 按 YEAR 字段建索引，查询时先按年过滤 |
| 月末/月初跨月跳转逻辑复杂 | 月历组件使用标准日期计算，以周一为周起点统一处理边界 |
| 批量审批性能（大量勾选） | 限制单次批量审批最多50条，超出分批处理 |
| 缺口统计查询性能（JOIN 工作日历 + 聚合工时） | 增加 `pm_work_log(CREATE_BY, LOG_DATE, STATUS)` 复合索引 |
| 24小时校验并发问题（用户同时打开两个录入弹窗） | 保存时再次校验当日累计工时，使用数据库查询保证一致性 |

## Migration Plan

### 部署步骤
1. 执行 `031-work-hours-calendar.sql`：创建 `wh_sys_work_calendar` 表和相关索引
2. 部署后端代码（新增的 Entity、DAO、BO、Controller）
3. 部署前端代码（新增页面、组件、路由、菜单）
4. 管理员登录 → 工作日历管理 → 选择当年 → 一键生成全年日历

### 回滚策略
- 数据库：`DROP TABLE wh_sys_work_calendar`（新表无业务依赖）
- 代码：直接回滚部署，不影响现有功能
- `pm_work_log` 表未修改结构，回滚无影响

## Open Questions

- 中国法定节假日数据是否需要在系统启动时预置种子数据？（如2026年春节、国庆等），当前设计为管理员手动一键生成后调整
- 是否需要支持"半天工作日"概念（标准工时4h）？当前设计已通过 `STANDARD_HOURS` 字段支持，但前端 UI 尚未设计
