## ADDED Requirements

### Requirement: 待审批列表
项目经理 SHALL 能在独立的审批页面查看自己负责项目的待审批（DRAFT）工时列表。列表 SHALL 默认显示当月数据，支持切换到上月。项目经理只能看到自己 PM_ID 匹配的项目的工时。

#### Scenario: 项目经理查看待审批工时
- **WHEN** 项目经理进入审批页面
- **THEN** 列表仅显示自己负责项目中状态为 DRAFT 的工时记录

#### Scenario: 非项目经理不显示审批入口
- **WHEN** 非项目经理角色的用户访问工时录入页面
- **THEN** 页面右上角不显示"工时审批"链接

#### Scenario: 切换月份查看审批
- **WHEN** 项目经理点击月份切换按钮选择上月
- **THEN** 列表刷新显示上月待审批工时

### Requirement: 逐条审批通过
项目经理 SHALL 能逐条审批通过工时，状态从 DRAFT 变更为 APPROVED。

#### Scenario: 审批通过单条工时
- **WHEN** 项目经理点击"通过"按钮审批一条工时
- **THEN** 该工时状态变为 APPROVED，从待审批列表移除

### Requirement: 逐条审批驳回
项目经理 SHALL 能逐条驳回工时，填写驳回原因，状态从 DRAFT 变更为 REJECTED。

#### Scenario: 审批驳回单条工时
- **WHEN** 项目经理点击"驳回"按钮并填写驳回原因
- **THEN** 该工时状态变为 REJECTED，驳回原因保存到 BLOCKER_REASON 字段

### Requirement: 批量审批
项目经理 SHALL 能通过勾选多条工时后批量通过或批量驳回。批量驳回时可填写统一驳回原因。

#### Scenario: 批量审批通过
- **WHEN** 项目经理勾选3条工时并点击"批量通过"
- **THEN** 3条工时状态全部变为 APPROVED

#### Scenario: 批量审批驳回
- **WHEN** 项目经理勾选3条工时并点击"批量驳回"，填写"描述不够详细"
- **THEN** 3条工时状态全部变为 REJECTED，驳回原因统一设置为"描述不够详细"

#### Scenario: 部分记录无权审批
- **WHEN** 项目经理勾选5条工时，其中3条属于自己负责的项目、2条属于其他项目
- **THEN** 系统只审批属于自己项目的3条，提示"2条记录无权审批，已跳过"

### Requirement: 审批权限校验
只有项目章程中 PM_ID 等于当前用户 ID 的用户才能审批该项目的工时。

#### Scenario: 非项目PM无法审批
- **WHEN** 用户A不是项目P的PM，尝试审批项目P的工时
- **THEN** 项目P的工时不出现在用户A的待审批列表中
