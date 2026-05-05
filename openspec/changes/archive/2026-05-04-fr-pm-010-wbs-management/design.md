## Context

WH PM 模块采用 Spring Boot + Flowable + SQLite + Element Plus 技术栈，模块化单体架构。`pm_wbs_element` 表已有基础骨架但缺少完整功能。SRS（FR-PM-010）定义了 WBS 管理的高层需求，但缺少可实现的数据模型、API 规格和前端交互细节。

现有脚手架：`wh-pm` 模块已创建，`/api/pm/` 前缀已注册，Flowable 流程引擎已集成。

## Goals / Non-Goals

**Goals:**
- 提供完整的 WBS 生命周期管理能力：创建、编辑、删除、查看、暂停/恢复、Reopen
- 支持版本追踪，记录每次计划/实际日期变更
- 支持批量导入/导出，降低数据录入成本
- 实现 100% 规则，确保 WBS 工作量一致性
- 实现审批流程管控，防止进行中任务被随意修改
- 多项目 WBS 列表视图，互斥展开，按项目分页

**Non-Goals:**
- 不包含甘特图渲染（属于 FR-PM-017 进度计划）
- 不包含思维导图模式（本期仅树形表格）
- 不包含资源分配与排期计算
- 不包含 ERP 产品模块的完整 CRUD（仅 PM 侧引用能力）

## Decisions

### D1: 版本号从 0.1 起，步长 0.1

版本号使用 `DECIMAL(4,1)` 存储：0.1, 0.2, 0.3... 最大支持到 999.9，足够覆盖 WBS 生命周期。每次修改自动生成新版本，无需用户手动标记版本号。

**Alternatives considered:** 语义化版本号（v1.0, v1.1），但业务方明确要求 0.1 累加规则。

### D2: 单表存储版本日期，null 区分计划/实际

`pm_wbs_version` 表一行包含计划+实际日期，`actual_start_date` / `actual_end_date` 为 null 表示纯计划版本。删除了冗余的 `date_type` 字段。

### D3: 产品/模块为独立新表（erp_product + erp_module）

PM 模块自建产品主数据，不映射现有 ERP 表。`erp_module` 通过 `product_id` 关联 `erp_product`。产品编码不存在时自动创建 `PLACEHOLDER` 状态记录。

### D4: 100% 规则 — 自动调整父节点 effort

子节点 effort 变更时，后端自动递归更新所有祖先节点的 `effort_estimate` 为子节点之和。非强制阻止，而是自动修正。

### D5: 导入父节点查找 — 三级 fallback

先在同批次内存映射中查找 → 再在已有 WBS 节点中按名称查找 → 都没找到则降级为一级节点。降级行为在导入结果中明确标记。

### D6: 审批流程复用单一 Flowable 模板

`PM_WBS_MODIFY_APPROVAL` 流程同时处理"进行中修改"和"Completed Reopen"两种场景，通过 `approval_type` 变量区分。提交人 = BA 时自动跳过 BA 节点。

### D7: WBS 编码系统自动生成

手动创建时根据父节点自动建议下一个编码。导入模板不含 WBS 编码列，按"名称+父节点名称"构建层级后自动生成。

### D8: 禁止删除有子节点的 WBS

删除前检查 `parent_id`，存在子节点时拒绝并提示先删除/迁移子节点。

## Risks / Trade-offs

| Risk | Impact | Mitigation |
|------|--------|------------|
| 递归更新 effort 在深层级 WBS 中可能性能差 | 中 | SQLite 单次更新快，4 层限制天然控制深度 |
| 导入时父节点降级为一级可能导致结构混乱 | 低 | 导入结果明确标记降级明细，用户可修正后重导 |
| PLACEHOLDER 产品记录可能积累脏数据 | 低 | 仅 PM 模块内可见，后续可通过产品管理清理 |
| 单流程模板处理两种审批语义可能混淆 | 低 | 通过 `approval_type` 变量区分，审批表单动态渲染 |
| 多项目列表一次加载大量 WBS 数据 | 中 | 互斥展开 + 按项目分页控制数据量 |

## Migration Plan

1. 执行 DDL：创建 `erp_product`, `erp_module`, `pm_wbs_version` 表，ALTER TABLE 扩展 `pm_wbs_element`
2. 部署 Flowable 流程定义 `PM_WBS_MODIFY_APPROVAL.bpmn20.xml`
3. 部署后端 Controller/BO/DAO
4. 部署前端页面
5. 如有问题：回滚代码，DDL 可逆（新表可 DROP，ALTER 新增字段保留无害）

## Open Questions

- ERP 产品表的 `product_code` 命名规范（是否需要前缀如 `PROD-`）需与 ERP 模块对齐
- WBS 导入模板的 Excel 格式（.xlsx）还是 CSV 格式需确认
