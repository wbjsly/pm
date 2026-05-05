## Cost Warning

### 需求

通过定时任务监控项目实际成本与预算的比率，达到阈值时生成预警。

### 全局配置

在 `application.yml` 中配置：

```yaml
cost:
  warning:
    cron: "0 0 6 * * ?"        # 定时任务执行时间，默认每天 6:00
    threshold:
      info: 0.80               # 80% 首页提醒
      warn: 0.95               # 95% 管理者预警
      critical: 1.00           # 100% 复盘预警
```

### 预警级别

| 级别 | 阈值 | 触发动作 | 关闭方式 |
|------|------|---------|---------|
| INFO | ≥ 80% | Dashboard 预警卡片展示 | 比率下降后自动关闭 |
| WARN | ≥ 95% | 预警列表通知管理者 | 仅管理者可手动关闭 |
| CRITICAL | > 100% | 通知 PM + 管理者，要求复盘 | 线下复盘后管理者手动关闭 |

### 定时任务流程

```
每天 6:00（可配置）
  │
  ├─ 查询所有 STATUS='APPROVED' 的预算
  │
  ├─ 对每个预算：
  │   ├─ SUM(actual_cost.amount WHERE project_id = X) / budget.total_budget = ratio
  │   ├─ ratio >= 1.00 → 创建/更新 CRITICAL 预警
  │   ├─ ratio >= 0.95 → 创建/更新 WARN 预警
  │   ├─ ratio >= 0.80 → 创建/更新 INFO 预警
  │   └─ ratio < 0.80  → 关闭已有的 ACTIVE 预警
  │
  └─ 写入/更新 pm_cost_warning 表
```

### 预警 API

| Method | Path | 说明 | 权限 |
|--------|------|------|------|
| GET | `/api/pm/cost-warnings` | 预警列表（projectId 筛选） | 已登录 |
| POST | `/api/pm/cost-warnings/{id}/close` | 关闭预警 | ROLE_ADMIN（WARN/CRITICAL）/ ROLE_PM（INFO） |
| POST | `/api/pm/cost-warnings/trigger` | 手动触发计算 | ROLE_ADMIN |

### Dashboard 集成

Dashboard 首页增加成本预警卡片组件：

```
┌─ 成本预警 ───────────────────────────────────────┐
│                                                   │
│ ⚠️  [项目A] 实际成本已达预算 98%                   │
│ ⚠️  [项目B] 实际成本已超过预算 105%，需复盘         │
│                                                   │
│ [查看更多] → 跳转预警列表                         │
└───────────────────────────────────────────────────┘
```

- 查询条件：`STATUS='ACTIVE' ORDER BY LEVEL DESC`
- 最多展示 5 条
