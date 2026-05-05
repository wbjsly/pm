## Actual Cost

### 需求

记录项目实际发生的成本，通过 `budget_item_id` 关联到具体预算科目。

### 数据来源

| 来源 | sourceSystem | 取值时机 |
|------|-------------|---------|
| 工时系统 | timesheet | 工时单提交（SUBMITTED 状态） |
| 采购系统 | procurement | 采购订单提交（SUBMITTED 状态） |
| 报销系统 | reimbursement | 报销单提交（SUBMITTED 状态） |
| 手动录入 | manual | 直接通过 API 录入 |

> **注意**：取提交状态，不取审批通过状态。

### 成本事件总线接口

```java
public enum CostSourceSystem {
    TIMESHEET, PROCUREMENT, REIMBURSEMENT, MANUAL
}

public class CostEvent {
    String projectId;
    String budgetItemId;
    BigDecimal amount;          // 正=成本, 负=红冲
    LocalDate costDate;
    CostSourceSystem sourceSystem;
    String sourceRef;           // 单号/备注
    String sourceId;            // 来源记录ID（用于去重/撤回）
}

public interface CostEventPublisher {
    void publish(CostEvent event);
}

@Component
public class DefaultCostEventPublisher implements CostEventPublisher {
    // 默认实现：写入 pm_actual_cost
}
```

### 手动录入（当前兜底方案）

- `sourceSystem` = `"manual"`
- `sourceRef` = 用户填写的备注说明
- 手动录入的成本可删除（用于纠错）

### 实际成本允许负向计入（红冲）

### API

| Method | Path | 说明 | 权限 |
|--------|------|------|------|
| POST | `/api/pm/actual-costs` | 录入实际成本 | ROLE_PM |
| GET | `/api/pm/actual-costs` | 列表（projectId, budgetItemId, sourceSystem 筛选） | ROLE_PM |
| GET | `/api/pm/actual-costs/{id}` | 详情 | ROLE_PM |
| DELETE | `/api/pm/actual-costs/{id}` | 删除（仅 sourceSystem=manual） | ROLE_PM |

### 约束

- `budgetItemId` 必填，关联到具体预算科目
- `costType` 必须与 `pm_budget_item.CATEGORY` 一致
- 仅手动录入（manual）的成本可删除
- 负向金额允许（红冲场景）
