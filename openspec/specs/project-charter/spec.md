## ADDED Requirements

### Requirement: 创建项目章程

系统 SHALL 允许具有 PM 角色的用户创建项目章程，包含以下字段：章程编号（自动生成）、项目名称、项目编号（自动生成）、项目描述、项目目标（JSON 数组）、范围概述、发起人 ID、项目经理 ID、预算上限、计划开始日期、计划结束日期、关键干系人（JSON 数组）。章程创建后初始状态为 DRAFT。

#### Scenario: PM 成功创建章程

- **WHEN** PM 用户提交章程创建表单，包含必填的项目名称、发起人、项目经理
- **THEN** 系统生成章程编号（格式 CHARTER-YYYY-NNN）和项目编号（格式 PRJ-YYYY-NNN），状态设为 DRAFT，返回创建成功

#### Scenario: 缺少必填字段时创建失败

- **WHEN** 用户提交章程创建请求但未提供项目名称
- **THEN** 系统返回 400 错误，提示项目名称为必填项

#### Scenario: 章程编号唯一性保证

- **WHEN** 同时创建两个章程
- **THEN** 系统生成不同的章程编号，不存在重复

### Requirement: 查询章程列表

系统 SHALL 提供分页查询接口，支持按状态（DRAFT/PENDING_APPROVAL/APPROVED/REJECTED/CLOSED）和项目经理筛选。返回结果包含章程核心字段及创建人姓名（非持久化字段）。

#### Scenario: 分页查询章程列表

- **WHEN** 用户请求章程列表，指定页码 1 和每页 20 条
- **THEN** 系统返回最多 20 条章程记录，包含总数，仅返回 DEL_FLAG='0' 的记录

#### Scenario: 按状态筛选

- **WHEN** 用户请求章程列表并指定 status=PENDING_APPROVAL
- **THEN** 系统仅返回状态为 PENDING_APPROVAL 的章程

#### Scenario: 按项目经理筛选

- **WHEN** 用户请求章程列表并指定 pmId=U000002
- **THEN** 系统仅返回项目经理为该用户的章程

### Requirement: 查看章程详情

系统 SHALL 提供根据 ID 查看章程详情的接口，返回章程全部字段及审批历史记录。

#### Scenario: 查看章程详情

- **WHEN** 用户请求查看章程详情，提供有效章程 ID
- **THEN** 系统返回章程全部业务字段、审计字段和审批历史时间线

#### Scenario: 查看已删除章程

- **WHEN** 用户请求查看已逻辑删除的章程详情
- **THEN** 系统返回 404 错误

### Requirement: 更新章程

系统 SHALL 允许 PM 用户更新状态为 DRAFT 或 REJECTED 的章程。已提交审批（PENDING_APPROVAL）或已审批通过（APPROVED）的章程不可直接更新。

#### Scenario: 更新草稿状态章程

- **WHEN** PM 用户更新状态为 DRAFT 的章程信息
- **THEN** 系统更新章程字段，记录 UPDATE_BY 和 UPDATE_DATE

#### Scenario: 尝试更新已提交章程

- **WHEN** 用户尝试更新状态为 PENDING_APPROVAL 的章程
- **THEN** 系统返回 400 错误，提示已提交的章程不可直接修改

### Requirement: 提交章程审批

系统 SHALL 允许 PM 用户将 DRAFT 或 REJECTED 状态的章程提交审批，创建 Flowable 流程实例，状态变为 PENDING_APPROVAL。

#### Scenario: PM 提交章程审批

- **WHEN** PM 用户对状态为 DRAFT 的章程执行提交审批操作
- **THEN** 系统创建 Flowable 流程实例，记录 PROCESS_INSTANCE_ID，状态变为 PENDING_APPROVAL

#### Scenario: 提交已审批通过的章程

- **WHEN** 用户尝试提交状态为 APPROVED 的章程审批
- **THEN** 系统返回 400 错误

### Requirement: 审批通过章程

系统 SHALL 允许具有 SPONSOR 角色的用户审批通过的章程，将状态更新为 APPROVED。审批通过后，章程不可再编辑。

#### Scenario: SPONSOR 审批通过

- **WHEN** SPONSOR 用户对状态为 PENDING_APPROVAL 的章程执行审批通过操作
- **THEN** 系统更新章程状态为 APPROVED，记录审批时间和审批意见

#### Scenario: 非 SPONSOR 尝试审批

- **WHEN** 不具有 SPONSOR 角色的用户尝试审批章程
- **THEN** 系统返回 403 Forbidden

### Requirement: 审批驳回章程

系统 SHALL 允许具有 SPONSOR 角色的用户驳回章程，将状态更新为 REJECTED，并记录驳回意见。驳回后 PM 可修改并重新提交。

#### Scenario: SPONSOR 驳回章程

- **WHEN** SPONSOR 用户对章程执行驳回操作，填写驳回意见
- **THEN** 系统更新章程状态为 REJECTED，记录驳回意见，通知 PM

#### Scenario: 驳回时未填写意见

- **WHEN** SPONSOR 用户驳回章程但未填写驳回意见
- **THEN** 系统返回 400 错误，提示驳回意见为必填项

### Requirement: 章程状态枚举

章程状态 SHALL 包含以下值：DRAFT（草稿）、PENDING_APPROVAL（待审批）、APPROVED（已审批）、REJECTED（已驳回）、CLOSED（已收尾）。状态流转遵循：DRAFT → PENDING_APPROVAL → APPROVED / REJECTED，REJECTED → PENDING_APPROVAL（重新提交），APPROVED → CLOSED。

#### Scenario: 初始状态为草稿

- **WHEN** 章程创建完成
- **THEN** 状态自动设为 DRAFT

#### Scenario: 状态流转 - 驳回后重新提交

- **WHEN** 章程状态为 REJECTED，PM 执行重新提交操作
- **THEN** 状态变为 PENDING_APPROVAL
