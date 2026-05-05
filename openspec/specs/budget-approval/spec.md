## Budget Approval

### 需求

预算提交后走独立的 Flowable 审批流程，审批人动态配置。

### BPMN 流程定义

流程 ID: `PM_BUDGET_APPROVAL`

```
Start ──► [动态审批人审批] ──► Gateway ──► Approved End
                                       │
                                       └──► Rejected End
```

- 使用 `flowable:executionListener` 绑定 `flowableProcessEndListener`
- 审批人从项目章程的 `SPONSOR_ID` 自动获取（默认），可在创建预算时覆盖

### 审批回调

实现 `ApprovalCompletedCallback` 接口：

```java
@Component
public class BudgetApprovalCallback implements ApprovalCompletedCallback {
    String getFlowCode() { return "PM_BUDGET_APPROVAL"; }

    void onApproved(String bizId, Map<String, Object> params) {
        // 1. 预算版本号 x.7 → x+1.0
        // 2. STATUS → APPROVED
        // 3. 记录 approval_comment
    }

    void onRejected(String bizId, String rejectReason) {
        // 1. 预算版本号 x.7 → x.1（退回草稿）
        // 2. STATUS → DRAFT
        // 3. 记录 approval_comment = rejectReason
    }
}
```

### API

| Method | Path | 说明 | 权限 |
|--------|------|------|------|
| POST | `/api/pm/budgets/{id}/approve` | 审批通过（设置 Flowable 变量 approvalResult=APPROVED） | ROLE_SPONSOR |
| POST | `/api/pm/budgets/{id}/reject` | 审批驳回（设置 Flowable 变量 approvalResult=REJECTED） | ROLE_SPONSOR |

### 审批流程状态变更

```
提交 (POST /submit):
  STATUS: DRAFT → PENDING
  VERSION: x.1 → x.7
  启动 Flowable 流程实例
  记录 PROCESS_INSTANCE_ID

审批通过:
  STATUS: PENDING → APPROVED
  VERSION: x.7 → (x+1).0

审批驳回:
  STATUS: PENDING → DRAFT
  VERSION: x.7 → x.1
```
