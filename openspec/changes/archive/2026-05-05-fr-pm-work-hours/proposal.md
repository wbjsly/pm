## Why

当前系统缺少工时管理功能，团队成员无法录入每日工时，项目经理无法审批和统计工时填报情况。需要新增工时录入、审批、工作日历配置和首页统计四大能力，形成完整的工时管理闭环。

## What Changes

- 新增工时录入月历视图（7×N 表格，周一为每周第一天），支持按日录入多条工时
- 新增工时审批页面，项目经理可逐条或批量审批自己负责项目的工时
- 新增工作日历管理页面，支持一键生成全年日历并手动调整节假日
- 新增主页工时统计板块，展示未录入天数、录入不足天数和缺口工时
- 工时审批采用轻量级状态机（DRAFT → APPROVED/REJECTED），不使用 Flowable BPMN
- 复用现有 `pm_work_log` 表，无需修改表结构

## Capabilities

### New Capabilities
- `work-hours`: 工时录入与查询，包括月历视图、录入弹窗、工时CRUD、重新提交
- `work-hours-approval`: 工时审批，包括待审批列表、逐条/批量审批通过与驳回
- `work-hours-stats`: 工时统计，包括缺口计算、主页统计展示
- `work-calendar`: 工作日历管理，包括全年一键生成、节假日调整、标准工时配置

### Modified Capabilities
<!-- 无现有能力的需求变更 -->

## Impact

- **前端**: 新增 3 个页面（工时录入、工时审批、工作日历）、2 个组件（月历、录入弹窗）、2 个 API 文件、修改 SidebarMenu 菜单和 router 路由、修改 Dashboard 首页
- **后端**: 新增 2 个实体类、2 个 DAO、2 个 BO、2 个 Controller、若干 DTO
- **数据库**: 新增 `wh_sys_work_calendar` 表及索引，新增 `pm_work_log` 查询索引
- **权限**: 工时录入与审批使用 `ROLE_PM`，工作日历使用 `ROLE_ADMIN`
