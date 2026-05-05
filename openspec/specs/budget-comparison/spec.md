## Budget Comparison

### 需求

在项目明细页面增加预实对比 Tab 页，左侧展示审批通过的树状预算科目，右侧按同一科目展示实际成本。

### 数据规则

- **严格一一对应**：每个预算科目行必有一条 `actualAmount`，无数据则为 `0`
- 按 `budget_item_id` 关联聚合 `pm_actual_cost`
- 父科目 `actualAmount` = 所有子科目 `actualAmount` 之和
- `ratio` = `actualAmount / budgetAmount`（预算为 0 时 ratio = 0）

### API

| Method | Path | 说明 | 权限 |
|--------|------|------|------|
| GET | `/api/pm/budgets/{id}/comparison` | 预实对比数据 | ROLE_PM |
| GET | `/api/pm/budgets/versions/{projectId}` | 项目历史版本列表 | ROLE_PM |

#### 预实对比请求参数

```
GET /api/pm/budgets/{id}/comparison?version=v1.0
```

- `version` 可选，默认最新 APPROVED 版本
- 返回预算科目树 + 每个科目的 actualAmount + ratio

#### 预实对比响应结构

```json
{
  "budgetId": "xxx",
  "version": "v1.0",
  "projectId": "xxx",
  "projectName": "xxx",
  "totalBudget": 500000,
  "totalActual": 380000,
  "totalRatio": 0.76,
  "items": [
    {
      "id": "item-001",
      "category": "LABOR",
      "level": 1,
      "budgetAmount": 200000,
      "actualAmount": 180000,
      "ratio": 0.90,
      "children": [
        {
          "id": "item-002",
          "category": "LABOR",
          "level": 2,
          "budgetAmount": 120000,
          "actualAmount": 80000,
          "ratio": 0.67,
          "roleCode": "DEV",
          "hours": 120,
          "costRate": 1000
        }
      ]
    }
  ]
}
```

### UI

- 项目详情页增加"预实对比"Tab
- 左侧：树状预算科目（不可编辑，仅 APPROVED 版本）
- 右侧：实际金额列 + 占比列
- 顶部：版本切换下拉框（默认最新 APPROVED）
- 超预算科目（ratio > 1.0）标红提醒
- 点击科目行可钻取实际成本明细列表
