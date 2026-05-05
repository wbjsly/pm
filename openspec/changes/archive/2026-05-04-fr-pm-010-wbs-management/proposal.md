## Why

当前 SRS（FR-PM-010）定义的 WBS 管理功能仅有基础数据模型和少量用户故事，缺乏完整的实现规格。项目从立项到执行需要完整的工作分解结构能力，包括多层级 WBS 创建、版本追踪、批量导入/导出、审批流程和 100% 规则校验。本 change 将 FR-PM-010 的需求细化为可执行的后端/前端实现规格。

## What Changes

- 扩展 `pm_wbs_element` 主表，新增产品、模块、优先级、技术难度、计划/实际责任人等字段
- 新建 `pm_wbs_version` 版本历史子表，记录每次日期变更的快照（版本号 0.1 起，每次 +0.1）
- 新建 `erp_product` 和 `erp_module` 产品主数据表
- 实现 WBS 树形 CRUD，自动生成分层编码（1.0, 1.1, 1.1.1...）
- 实现批量导入（CSV 模板，不含 WBS 编码列，按父节点名称构建层级）和导出功能
- 实现 100% 规则：子节点 effort 之和自动更新父节点 effort
- 实现 WBS 状态机（PLANNED / IN_PROGRESS / COMPLETED / SUSPENDED）及对应操作约束
- 实现进行中修改和 Reopen 的 Flowable 审批流程（BA → PM，提交人=BA 时跳过 BA 审批）
- 实现多项目列表页（按项目分页，每页 5 个，互斥展开）
- 实现 WBS 详情页（仅展示最新版本号）和历史版本表格页

## Capabilities

### New Capabilities
- `wbs-management`: WBS 树形结构的创建、编辑、删除、查看，自动编码生成，100% 规则校验
- `wbs-version`: WBS 版本历史管理，版本号累加，版本快照记录
- `wbs-import-export`: 批量导入（CSV）、导出（Excel）、模板下载
- `wbs-approval`: 进行中修改审批和 Completed 任务 Reopen 审批流程（Flowable BPMN）
- `wbs-ui`: WBS 多项目列表页、详情页、历史版本页的前端实现
- `product-master-data`: ERP 产品与模块主数据管理（新建表，含占位记录机制）

### Modified Capabilities
<!-- No existing capabilities are being modified -->

## Impact

- **数据库**: 新增 `pm_wbs_version`, `erp_product`, `erp_module` 三张表；`pm_wbs_element` 扩展字段
- **后端**: `wh-pm` 模块新增 Controller/BO/DAO 层；Flowable 新增 `PM_WBS_MODIFY_APPROVAL` 流程定义
- **前端**: `src/views/pm/wbs/` 新增列表页、详情页、历史版本页、导入对话框
- **API**: `/api/pm/wbs/` 下新增 14 个端点
- **依赖**: Flowable 流程引擎（已有），无新增外部依赖
