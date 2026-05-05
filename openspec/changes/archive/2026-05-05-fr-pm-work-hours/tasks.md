## 1. 数据库迁移

- [x] 1.1 创建 `031-work-hours-calendar.sql`，定义 `wh_sys_work_calendar` 表结构
- [x] 1.2 在迁移文件中新增 `pm_work_log` 查询索引（CREATE_BY+LOG_DATE+STATUS，PROJECT_ID+LOG_DATE）
- [x] 1.3 执行迁移脚本，验证表和索引创建成功

## 2. 工作日历后端

- [x] 2.1 创建 `WhSysWorkCalendar.java` 实体类
- [x] 2.2 创建 `WhSysWorkCalendarDao.java` 数据访问层
- [x] 2.3 创建 `WhSysWorkCalendarBo.java` 业务逻辑层（包含一键生成全年日历方法）
- [x] 2.4 创建 `WhSysWorkCalendarController.java` 控制器（CRUD + 批量设置 + 一键生成）
- [x] 2.5 创建 `WorkCalendarRequest.java` DTO（包含批量设置参数）
- [x] 2.6 编写 `WhSysWorkCalendarBo` 单元测试：验证全年生成逻辑和日期类型判定

## 3. 工时录入后端

- [x] 3.1 创建 `WhPmWorkLog.java` 实体类（复用 pm_work_log 表）
- [x] 3.2 创建 `WhPmWorkLogDao.java` 数据访问层（按月查询、按项目PM查询待审批）
- [x] 3.3 创建 `WhPmWorkLogBo.java` 业务逻辑层（CRUD + 审批 + 统计 + 工时校验）
- [x] 3.4 创建 `WhPmWorkLogController.java` 控制器（工时CRUD + 审批 + 统计API）
- [x] 3.5 创建 `WorkLogCreateRequest.java` / `WorkLogUpdateRequest.java` / `WorkLogApprovalRequest.java` DTOs
- [x] 3.6 实现当日累计工时≤24h校验逻辑
- [x] 3.7 实现未来日期拦截逻辑
- [x] 3.8 实现项目权限过滤（PM_ID / ownerId / plannedOwnerId 匹配）
- [x] 3.9 实现批量审批逻辑（自动过滤无权审批记录）
- [x] 3.10 实现工时缺口统计算法（JOIN工作日历 + 聚合）
- [x] 3.11 编写 `WhPmWorkLogBo` 单元测试：覆盖工时校验、审批流程、缺口统计

## 4. 工作日历前端

- [x] 4.1 创建 `wh_sys_work_calendar.js` API 封装
- [x] 4.2 创建 `views/system/calendar/index.vue` 工作日历管理页面
- [x] 4.3 实现年份选择器 + 12个月历网格展示
- [x] 4.4 实现一键生成全年日历按钮及交互
- [x] 4.5 实现点击格子切换日期类型（工作日/周末/节假日）
- [x] 4.6 实现节假日名称编辑和标准工时调整
- [x] 4.7 在 `SidebarMenu.vue` 新增"工作日历"菜单项（系统管理组，ROLE_ADMIN）

## 5. 工时录入前端

- [x] 5.1 创建 `api/pm/workHours.js` API 封装
- [x] 5.2 创建 `views/pm/work-hours/index.vue` 工时录入主页面
- [x] 5.3 创建 `components/MonthCalendar.vue` 月历组件（7列×N行，周一为首日）
- [x] 5.4 实现跨月格子展示和点击跳转月份
- [x] 5.5 实现格子内按项目分行显示+状态颜色标识
- [x] 5.6 实现每周工时小计和底部本月汇总
- [x] 5.7 创建 `components/WorkHourDialog.vue` 工时录入弹窗
- [x] 5.8 实现项目下拉筛选（仅显示用户有权限的项目）
- [x] 5.9 实现未来日期拦截和24小时校验前端提示
- [x] 5.10 实现工时删除功能（仅DRAFT/REJECTED可删除）
- [x] 5.11 实现工时修改功能
- [x] 5.12 实现重新提交按钮（REJECTED → DRAFT）
- [x] 5.13 在右上角添加"工时审批"链接（仅项目经理可见）
- [x] 5.14 在 `router/index.js` 添加 `/pm/work-hours` 路由
- [x] 5.15 在 `SidebarMenu.vue` 新增"工时管理"菜单项（项目管理组，ROLE_PM）

## 6. 工时审批前端

- [x] 6.1 创建 `views/pm/work-hours/approval.vue` 审批页面
- [x] 6.2 实现待审批工时列表（默认当月，可切换上月）
- [x] 6.3 实现逐条审批通过和驳回（含驳回原因输入）
- [x] 6.4 实现批量审批（勾选多选 + 批量通过/驳回）
- [x] 6.5 实现部分无权审批的提示反馈
- [x] 6.6 在 `router/index.js` 添加 `/pm/work-hours/approval` 路由

## 7. 主页工时统计

- [x] 7.1 在 `views/dashboard/index.vue` 新增工时统计板块
- [x] 7.2 调用工时统计API获取缺口数据
- [x] 7.3 实现4项指标展示：实际已录、未录入天数、录入不足天数、缺口工时
- [x] 7.4 实现点击跳转到工时录入页面

## 8. 集成测试与验证

- [x] 8.1 启动后端，验证所有新API可正常调用
- [x] 8.2 启动前端，验证月历视图正常渲染
- [x] 8.3 测试完整录入→审批→通过流程
- [x] 8.4 测试完整录入→驳回→修改→重新提交→通过流程
- [x] 8.5 测试批量审批流程
- [x] 8.6 测试工作日历一键生成和手动调整
- [x] 8.7 测试主页工时统计数据准确性
- [x] 8.8 测试权限控制（非PM看不到审批入口，非ADMIN看不到工作日历）
- [x] 8.9 测试24小时上限拦截和未来日期拦截
- [x] 8.10 验证项目可正常编译构建
