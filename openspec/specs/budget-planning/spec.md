## Budget Planning

### 需求

作为项目经理（PM），能够创建、编辑、提交项目预算，建立两级预算科目结构。

### 预算科目结构

一级科目（7 个）：
- LABOR（人工）
- PROCUREMENT（采购）
- TRAVEL（差旅）
- BUSINESS（商务费用）
- ENTERTAINMENT（客户招待费）
- ACTIVITY（活动费）
- OTHER（其他）

二级科目：
- 人工 → 按岗位/角色细分（DEV/TEST/BA/ARCH/QA/PM 等），每个角色包含工时和成本定额
- 采购 → 按 BOM 项细分，每项包含数量和单价
- 差旅/商务/招待/活动/其他 → 可设置二级科目或直接填写金额

### 预算版本规则

| 阶段 | 版本号 | 状态 | 说明 |
|------|--------|------|------|
| PM 创建 | v0.1 | DRAFT | 初始草稿 |
| PM 提交审批 | v0.7 | PENDING | 提交后锁定 |
| Sponsor 审批通过 | v1.0 | APPROVED | 锁定，成为有效预算 |
| 调整预算 | v1.1 | DRAFT | 基于 v1.0 复制科目结构 |
| 调整提交 | v1.7 | PENDING | 提交后锁定 |
| 调整审批通过 | v2.0 | APPROVED | 新版本锁定 |

- 整数位 = 已审批通过次数（0 = 从未审批）
- 小数位 = 1: 草稿, 7: 已提交, 0: 审批通过
- 审批通过后锁定，不可修改
- 同一项目同一时刻只有一个 APPROVED 版本
- 创建新版本时，从当前 APPROVED 版本复制科目结构

### API

| Method | Path | 说明 | 权限 |
|--------|------|------|------|
| POST | `/api/pm/budgets` | 创建预算（含科目明细） | ROLE_PM |
| GET | `/api/pm/budgets` | 预算列表（projectId, status 筛选） | ROLE_PM |
| GET | `/api/pm/budgets/{id}` | 预算详情（含完整科目树） | ROLE_PM |
| PUT | `/api/pm/budgets/{id}` | 更新预算（仅 DRAFT 状态） | ROLE_PM |
| DELETE | `/api/pm/budgets/{id}` | 删除预算（仅 DRAFT 状态） | ROLE_PM |
| POST | `/api/pm/budgets/{id}/submit` | 提交审批（版本号 x.1 → x.7） | ROLE_PM |

### 约束

- 创建预算时 `budget_code` 自动生成：`BUDGET-YYYY-NNN`（使用 SequenceService）
- `cost_baseline` = 所有一级科目金额之和（不含管理储备）
- `total_budget` = `cost_baseline` + `management_reserve`
- 仅 DRAFT 状态的预算可编辑/删除
- 更新预算时需同步更新 `pm_budget_item` 及其子表
