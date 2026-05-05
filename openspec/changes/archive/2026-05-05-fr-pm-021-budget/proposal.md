## Why

当前 SRS（FR-PM-021）定义的预算制定功能仅有数据库迁移脚本（`014-pm-cost.sql`），但 Java 层和前端层完全未实现。项目管理从章程立项到执行需要完整的预算管理能力，包括两级预算科目编制、Flowable 审批、预实对比分析和成本预警机制。本 change 将 FR-PM-021 的需求细化为可执行的后端/前端实现规格。

## What Changes

- 新建 `pm_budget_item` 预算科目骨架表，支持 7 个一级科目（人工、采购、差旅、商务费用、客户招待费、活动费、其他）及其二级科目
- 新建 `pm_budget_item_labor` 人工明细表（岗位/角色 + 工时 + 成本定额）
- 新建 `pm_budget_item_procurement` 采购明细表（BOM 项 + 数量 + 单价）
- 新建 `pm_budget_item_other` 其他费用明细表（差旅/商务/招待/活动/其他共用，CATEGORY 区分）
- 扩展 `pm_actual_cost` 表，新增 `BUDGET_ITEM_ID`、`SOURCE_SYSTEM`、`SOURCE_ID` 字段
- 新建 `pm_cost_warning` 成本预警表
- 实现预算树形 CRUD，手动逐条添加科目
- 实现预算版本管理（v0.1→v0.7→v1.0→v1.1→v1.7→v2.0），审批通过后锁定
- 实现独立 BPMN 审批流程 `PM_BUDGET_APPROVAL` + 动态审批人配置
- 实现预实对比 API 和页面，严格按预算科目行一一对应展示
- 实现成本预警定时任务（全局可配置执行时间），Dashboard 预警卡片集成
- 实现实际成本手动录入兜底 + 成本事件总线接口（为后续工时/采购/报销模块对接预留）

## Capabilities

### New Capabilities
- `budget-planning`: 预算编制（两级科目）、版本管理、审批提交
- `budget-approval`: 预算 Flowable 审批流程（独立 BPMN，动态审批人）
- `budget-comparison`: 预实对比 API 和页面，历史版本切换
- `actual-cost`: 实际成本手动录入、删除、查询，成本事件总线接口
- `cost-warning`: 成本预警定时任务、Dashboard 集成、预警关闭
- `budget-database`: 预算相关 DDL、Entity、DAO 层

### Modified Capabilities
- `dashboard-home`: Dashboard 页面新增成本预警卡片组件

## Impact

- **数据库**: 新增 4 张表（`pm_budget_item`、`pm_budget_item_labor`、`pm_budget_item_procurement`、`pm_budget_item_other`、`pm_cost_warning`）；扩展 `pm_actual_cost` 表
- **后端**: `wh-pm` 模块新增 Budget/ActualCost/CostWarning 的 Controller/BO/DAO/Entity 层；Flowable 新增 `PM_BUDGET_APPROVAL` 流程定义；新增 `CostEventPublisher` 接口
- **前端**: `src/views/pm/budget/` 新增预算编制表单页、预实对比页；Dashboard 新增预警卡片；`src/api/pm/budget.js` 新增 API 模块
- **API**: `/api/pm/budgets/`、`/api/pm/actual-costs/`、`/api/pm/cost-warnings/` 下新增 15 个端点
- **依赖**: Flowable 流程引擎（已有），无新增外部依赖
- **不依赖**: `fr-pm-010-wbs-management`，可独立开发
