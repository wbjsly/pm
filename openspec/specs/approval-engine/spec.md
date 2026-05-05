## ADDED Requirements

### Requirement: ApprovalCompletedCallback 接口定义

系统 SHALL 定义 ApprovalCompletedCallback 接口，包含以下方法：getFlowCode() 返回流程类型编码（String）；onApproved(bizId, params) 处理审批通过回调；onRejected(bizId, rejectReason) 处理审批驳回回调。

#### Scenario: 接口方法调用

- **WHEN** 审批流程完成，调用 CharterApprovalCallback.getFlowCode()
- **THEN** 返回 "PM_CHARTER_APPROVAL"

### Requirement: 回调自动注册机制

ApprovalCallbackRegistry SHALL 在 Spring 启动时通过 @Autowired Map<String, ApprovalCompletedCallback> 自动收集所有回调实现，并以 flowCode 为键建立索引。

#### Scenario: 回调自动注册

- **WHEN** Spring 容器启动，CharterApprovalCallback 标注 @Component
- **THEN** ApprovalCallbackRegistry 中注册 flowCode=PM_CHARTER_APPROVAL → CharterApprovalCallback 实例

### Requirement: Flowable 流程结束监听

FlowableProcessEndListener SHALL 实现 Flowable 的流程结束事件监听。当流程完成时，从流程变量中获取 flowCode，在回调注册中心查找对应回调，根据审批结果调用 onApproved 或 onRejected。

#### Scenario: 流程完成触发回调

- **WHEN** PM_CHARTER_APPROVAL 流程完成且审批通过
- **THEN** FlowableProcessEndListener 查找 CharterApprovalCallback，调用 onApproved(bizId, params)

#### Scenario: 未找到对应回调

- **WHEN** 流程完成的 flowCode 在注册中心无对应回调
- **THEN** 记录警告日志，不抛出异常

### Requirement: 审批实例实体

系统 SHALL 提供 WhApprovalInstance 实体，记录审批实例信息：业务实体 ID、流程类型编码、流程实例 ID、审批状态、创建时间。

#### Scenario: 创建审批实例

- **WHEN** 用户提交章程审批
- **THEN** 创建 WhApprovalInstance 记录，关联章程 ID 和流程类型

### Requirement: 审批记录实体

系统 SHALL 提供 WhApprovalRecord 实体，记录每次审批操作：审批实例 ID、审批人、审批动作（通过/驳回）、审批意见、审批时间。

#### Scenario: 记录审批操作

- **WHEN** SPONSOR 审批通过章程
- **THEN** 创建 WhApprovalRecord 记录，包含审批人、动作、时间和意见
