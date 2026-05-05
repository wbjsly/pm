## ADDED Requirements

### Requirement: 章程审批回调实现

CharterApprovalCallback SHALL 实现 ApprovalCompletedCallback 接口，流程编码为 PM_CHARTER_APPROVAL。onApproved 方法将章程状态更新为 APPROVED；onRejected 方法将章程状态更新为 REJECTED 并记录驳回意见。

#### Scenario: 审批通过回调执行

- **WHEN** Flowable 流程结束且审批结果为通过，bizId 为章程 ID
- **THEN** CharterApprovalCallback.onApproved() 被调用，章程状态更新为 APPROVED

#### Scenario: 审批驳回回调执行

- **WHEN** Flowable 流程结束且审批结果为驳回，bizId 为章程 ID，rejectReason 为驳回意见
- **THEN** CharterApprovalCallback.onRejected() 被调用，章程状态更新为 REJECTED 并记录驳回意见

### Requirement: Flowable BPMN 流程定义

系统 SHALL 包含 PM_CHARTER_APPROVAL 的 BPMN 2.0 流程定义文件。流程包含：开始事件 → 用户任务（SPONSOR 审批）→ 排他网关（通过/驳回）→ 结束事件。SPONSOR 审批任务使用 candidateGroups="ROLE_SPONSOR"。

#### Scenario: 流程定义自动部署

- **WHEN** Spring Boot 应用启动
- **THEN** Flowable 自动扫描并部署 bpmn/pm-charter-approval.bpmn20.xml 流程定义

#### Scenario: SPONSOR 角色可看到审批任务

- **WHEN** 章程提交审批后，查询 SPONSOR 角色的待办任务
- **THEN** 返回对应的审批任务

### Requirement: 审批通过行为边界

审批通过后，CharterApprovalCallback.onApproved() SHALL 仅将章程状态更新为 APPROVED。不自动创建管理计划、不触发其他下游流程。管理计划的创建留到 FR-PM-002 实现。

#### Scenario: 审批通过后不触发管理计划创建

- **WHEN** 章程审批通过
- **THEN** 仅更新章程状态为 APPROVED，不创建 pm_management_plan 记录
