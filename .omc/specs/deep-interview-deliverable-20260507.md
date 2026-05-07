# Deep Interview Spec: 项目成果物管理

## Metadata
- Interview ID: deep-interview-deliverable-20260507
- Rounds: 5
- Final Ambiguity Score: 14.0%
- Type: brownfield
- Generated: 2026-05-07T10:01:00Z
- Threshold: 0.2 (20%)
- Initial Context Summarized: no
- Status: PASSED

## Clarity Breakdown
| Dimension | Score | Weight | Weighted |
|-----------|-------|--------|----------|
| Goal Clarity | 0.95 | 35% | 0.333 |
| Constraint Clarity | 0.85 | 25% | 0.213 |
| Success Criteria | 0.75 | 25% | 0.188 |
| Context Clarity | 0.85 | 15% | 0.128 |
| **Total Clarity** | | | **0.860** |
| **Ambiguity** | | | **14.0%** |

## Goal
在 WH PM 系统中新增**独立于 WBS 的项目成果物（交付物）管理功能**。每个项目维护一个独立的成果物清单，PM 创建成果物后通过 Flowable 审批流提交给发起人审批，审批通过后由 PM 标记实际交付。成果物支持文件附件（通过 MinIO 存储）和自动编号（DELIVERABLE-YYYY-NNN）。

**核心区别**：成果物（Deliverable）是「交付什么」（产出），WBS 是「怎么做」（工作分解）。两者独立管理，审批人也不同 —— 成果物由发起人确认，WBS 由产品经理+PM 审批。

## Constraints
- 使用现有 Flowable 6.8 审批引擎，新增 BPMN 流程文件
- 使用现有 MinIO 存储附件文件
- 成果物编号自动生成，格式：`DELIVERABLE-YYYY-NNN`
- 删除约束：草稿和驳回状态可删除；审批中/已通过/已交付状态不可删除
- 编辑约束：草稿和驳回状态可编辑重新提交；已通过后可标记交付；已交付后不可编辑
- 权限：PM（ROLE_PM）可以增删改查；SPONSOR 可以审批和查看；其他角色只读
- 仅已审批通过（APPROVED）的项目可以管理成果物
- 遵循现有后端分层：Entity → DAO → BO → Controller
- 遵循现有前端模式：Vue 3 + Element Plus，参照 charter 的列表/表单模式

## Non-Goals
- 不修改现有 WBS 实体或 WBS 审批流程
- 不与项目收尾（FR-PM-006）强制耦合（先独立运行，后续可选集成）
- 不支持成果物的批量导入导出（首个版本）
- 不实现多级成果物层级（成果物是平级清单）

## Acceptance Criteria
- [ ] PM 可以在项目下创建成果物，填写名称、描述、计划交付日期、上传附件
- [ ] 系统自动生成成果物编号（DELIVERABLE-YYYY-NNN）
- [ ] PM 可以提交成果物进入审批流程（状态：草稿 → 审批中）
- [ ] 发起人（SPONSOR）可以审批通过或驳回成果物
- [ ] 审批通过后，PM 可以标记「已交付」并填写实际交付日期
- [ ] 驳回的成果物回到草稿状态，PM 可重新编辑提交
- [ ] 草稿/驳回状态的成果物可被 PM 删除；审批中/已通过/已交付的不可删除
- [ ] 成果物列表页展示：编号、名称、状态（el-tag）、计划交付日期、实际交付日期
- [ ] 成果物详情页展示所有字段 + 附件列表 + 审批历史
- [ ] 仅已审批通过的项目可以进入成果物管理页面
- [ ] 后端 API 遵循 `/api/pm/deliverables` 路由前缀

## Assumptions Exposed & Resolved
| Assumption | Challenge | Resolution |
|------------|-----------|------------|
| 成果物可能是 WBS 的一部分 | Round 5 反调：如果只是 WBS 标记会不会更简单？ | 确认独立管理必要 — 成果物和WBS是不同概念，审批人也不同 |
| 成果物可能不需要审批 | Round 2：纯记录还是有审批？ | 需要简单审批流（PM → SPONSOR） |
| 成果物可能只需要两三种状态 | Round 3：状态流转是什么？ | 四状态：DRAFT → PENDING_APPROVAL → APPROVED → DELIVERED（+ REJECTED → DRAFT） |
| 可能不需要附件功能 | Round 4：是否需要附件？ | 需要附件，通过 MinIO 存储 |
| 可能不需要自动编号 | Round 4：是否需要编号？ | 需要自动编号 DELIVERABLE-YYYY-NNN |

## Technical Context
- **Backend**: Spring Boot 2.7 + MyBatis Plus 3.5.5 + Flowable 6.8 + SQLite
- **Frontend**: Vue 3.4 + Element Plus 2.6 + Vite 5 + Pinia + Axios
- **File Storage**: MinIO (已配置，bucket: wh-files)
- **Auth**: Spring Security + JWT，角色权限矩阵
- **Existing patterns**: WhPmCharter → WhPmCharterBo → WhPmCharterDao → WhPmCharterController
- **Existing BPMN patterns**: pm-charter-approval.bpmn20.xml (SPONSOR审批), pm-budget-approval.bpmn20.xml
- **Existing callback patterns**: ApprovalCallbackRegistry + ApprovalCompletedCallback (参考 BudgetApprovalCallback, CharterApprovalCallback)
- **Existing frontend patterns**: src/views/pm/charter/ (列表+表单+详情)

### New files needed:

**Backend (wh-backend/src/main/java/com/wh/)**:
- `entity/pm/WhPmDeliverable.java` — 成果物实体
- `dao/pm/WhPmDeliverableDao.java` — MyBatis Plus BaseMapper
- `bo/pm/DeliverableCreateRequest.java` — 创建请求 BO
- `bo/pm/DeliverableUpdateRequest.java` — 更新请求 BO
- `bo/pm/DeliverableSubmitRequest.java` — 提交审批请求 BO
- `vo/pm/DeliverableVO.java` — 列表/详情 VO
- `controller/pm/WhPmDeliverableController.java` — REST Controller
- `approval/pm/DeliverableApprovalCallback.java` — 审批回调

**Backend (wh-backend/src/main/resources/)**:
- `bpmn/pm-deliverable-approval.bpmn20.xml` — BPMN 审批流程
- `db/sqlite/037-pm-deliverable.sql` — DDL 迁移脚本

**Frontend (wh-frontend/src/)**:
- `views/pm/deliverable/index.vue` — 成果物列表页
- `views/pm/deliverable/form.vue` — 成果物创建/编辑页
- `views/pm/deliverable/detail.vue` — 成果物详情页
- `api/pm/deliverable.js` — API 调用

**Route addition** (router/index.js):
- `/pm/deliverable` — 成果物列表
- `/pm/deliverable/form` — 新增成果物
- `/pm/deliverable/form/:id` — 编辑成果物
- `/pm/deliverable/detail/:id` — 成果物详情

## Ontology (Key Entities)
| Entity | Type | Fields | Relationships |
|--------|------|--------|---------------|
| Project | core domain | id, name, charterCode, status | Project has many Deliverables |
| Deliverable | core domain | id, deliverableCode, name, description, plannedDeliveryDate, actualDeliveryDate, status, attachments, projectId | Deliverable belongs to Project; Deliverable triggers one Approval |
| Approval | supporting | flowableProcessId, status, submittedAt, approvedAt, rejectedAt | Approval belongs to Deliverable |

### Status State Machine
```
DRAFT ──(submit)──> PENDING_APPROVAL ──(approve)──> APPROVED ──(mark delivered)──> DELIVERED
  ^                      │
  └────(reject)──────────┘
```

## Ontology Convergence
| Round | Entity Count | New | Changed | Stable | Stability Ratio |
|-------|-------------|-----|---------|--------|----------------|
| 1 | 2 | 2 | - | - | - |
| 2 | 3 | 1 | 0 | 2 | 67% |
| 3 | 3 | 0 | 0 | 3 | 100% |
| 4 | 3 | 0 | 0 | 3 | 100% |
| 5 | 3 | 0 | 0 | 3 | 100% |

## Interview Transcript
<details>
<summary>Full Q&A (5 rounds)</summary>

### Round 1
**Q:** 成果物管理的核心是什么？（WBS标记/范围确认/完整生命周期/简单清单）
**A:** 简单成果物清单 — 独立于WBS，每个项目维护一个交付物清单
**Ambiguity:** 66.0% (Goal: 0.50, Constraints: 0.20, Criteria: 0.10, Context: 0.60)

### Round 2
**Q:** 权限和流程边界？（纯记录/PM管理/PM编辑+发起人确认/审批流）
**A:** 需要简单审批 — PM提交→发起人审批，使用Flowable
**Ambiguity:** 50.5% (Goal: 0.65, Constraints: 0.50, Criteria: 0.15, Context: 0.70)

### Round 3
**Q:** 成果物生命周期和状态流转？
**A:** 三段式：草稿→审批中→已通过→已交付。驳回回到草稿
**Ambiguity:** 33.3% (Goal: 0.80, Constraints: 0.60, Criteria: 0.50, Context: 0.75)

### Round 4
**Q:** 成果物字段和操作约束？（附件/编号/删除规则）
**A:** 基础字段+附件+编号。已审批/已交付不可删除
**Ambiguity:** 21.5% (Goal: 0.90, Constraints: 0.80, Criteria: 0.60, Context: 0.80)

### Round 5 [CONTRARIAN]
**Q:** 如果成果物只是WBS的一个标记会不会更简单？
**A:** 独立管理确实必要 — 成果物和WBS概念不同，审批人不同
**Ambiguity:** 14.0% (Goal: 0.95, Constraints: 0.85, Criteria: 0.75, Context: 0.85)

</details>
