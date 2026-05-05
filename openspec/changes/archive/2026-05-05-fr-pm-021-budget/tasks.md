## 1. Database DDL & Bootstrap

- [x] 1.1 Create `pm_budget_item` table DDL in new migration script `resources/db/sqlite/024-pm-budget.sql` (columns: ID TEXT PK, BUDGET_ID TEXT FK→pm_budget, CATEGORY TEXT, PARENT_ID TEXT self-ref, AMOUNT TEXT, LEVEL INTEGER, SORT_ORDER INTEGER, + audit columns)
- [x] 1.2 Create `pm_budget_item_labor` table DDL (columns: ID TEXT PK, BUDGET_ITEM_ID TEXT FK→pm_budget_item, ROLE_CODE TEXT, HOURS TEXT, COST_RATE TEXT, AMOUNT TEXT, + audit columns)
- [x] 1.3 Create `pm_budget_item_procurement` table DDL (columns: ID TEXT PK, BUDGET_ITEM_ID TEXT FK→pm_budget_item, BOM_ITEM TEXT, QTY TEXT, UNIT_PRICE TEXT, AMOUNT TEXT, + audit columns)
- [x] 1.4 Create `pm_budget_item_other` table DDL (columns: ID TEXT PK, BUDGET_ITEM_ID TEXT FK→pm_budget_item, CATEGORY TEXT, DESCRIPTION TEXT, AMOUNT TEXT, + audit columns)
- [x] 1.5 Create `pm_cost_warning` table DDL (columns: ID TEXT PK, PROJECT_ID TEXT FK, BUDGET_ID TEXT FK, LEVEL TEXT, RATIO TEXT, STATUS TEXT, TRIGGERED_AT TEXT, CLOSED_BY TEXT, CLOSED_AT TEXT, + audit columns)
- [x] 1.6 Create ALTER TABLE script for `pm_actual_cost` to add: BUDGET_ITEM_ID TEXT FK→pm_budget_item, SOURCE_SYSTEM TEXT, SOURCE_ID TEXT
- [x] 1.7 Verify `SqliteBootstrap` picks up new script on startup (check `wh_bootstrap_marker` table)

## 2. Backend — Entity & DAO (Budget)

- [x] 2.1 Create `WhPmBudgetItem` entity: `extends BaseEntity`, `@TableName("pm_budget_item")`, fields: `budgetId`, `category`, `parentId`, `amount` (BigDecimal via @TableField with typeHandler or String per convention), `level`, `sortOrder`. Add `@TableField(exist = false)` for `children` list and display fields
- [x] 2.2 Create `WhPmBudgetItemLabor` entity: `@TableName("pm_budget_item_labor")`, fields: `budgetItemId`, `roleCode`, `hours`, `costRate`, `amount`
- [x] 2.3 Create `WhPmBudgetItemProcurement` entity: `@TableName("pm_budget_item_procurement")`, fields: `budgetItemId`, `bomItem`, `qty`, `unitPrice`, `amount`
- [x] 2.4 Create `WhPmBudgetItemOther` entity: `@TableName("pm_budget_item_other")`, fields: `budgetItemId`, `category`, `description`, `amount`
- [x] 2.5 Create `WhPmBudgetItemDao extends BaseMapper<WhPmBudgetItem>`: add custom `@Select` for tree query by budgetId (recursive CTE or parent-child), `@Select` for items by budgetId and level
- [x] 2.6 Create `WhPmBudgetItemLaborDao extends BaseMapper<WhPmBudgetItemLabor>`
- [x] 2.7 Create `WhPmBudgetItemProcurementDao extends BaseMapper<WhPmBudgetItemProcurement>`
- [x] 2.8 Create `WhPmBudgetItemOtherDao extends BaseMapper<WhPmBudgetItemOther>`

## 3. Backend — Entity & DAO (Actual Cost & Warning)

- [x] 3.1 Create `WhPmActualCost` entity: `extends BaseEntity`, `@TableName("pm_actual_cost")`, fields: `projectId`, `budgetItemId`, `activityId`, `costDate`, `costType`, `amount`, `description`, `sourceSystem`, `sourceRef`, `sourceId`. Add `@TableField(exist = false)` for `budgetItemCategory`
- [x] 3.2 Create `WhPmActualCostDao extends BaseMapper<WhPmActualCost>`: add `@Select` for sum by budgetItemId, `@Select` for list by projectId and budgetItemId
- [x] 3.3 Create `WhPmCostWarning` entity: `extends BaseEntity`, `@TableName("pm_cost_warning")`, fields: `projectId`, `budgetId`, `level`, `ratio`, `status`, `triggeredAt`, `closedBy`, `closedAt`
- [x] 3.4 Create `WhPmCostWarningDao extends BaseMapper<WhPmCostWarning>`: add `@Select` for active warnings by projectId, `@Select` for warnings ordered by level DESC

## 4. Backend — BO (Budget Planning)

- [x] 4.1 Create `WhPmBudgetBo` with `@Service` + `@Transactional`: `pageList(pageNum, pageSize, projectId, status)` using `LambdaQueryWrapper` + `Page<T>`
- [x] 4.2 Implement `create()`: generate budget_code via SequenceService (`BUDGET-YYYY-NNN`), set version=`v0.1`, status=`DRAFT`, cost_baseline=0, management_reserve=0, total_budget=0. Create all budget items (primary categories + secondary items with sub-table records)
- [x] 4.3 Implement `getDetail(id)`: return budget + full item tree (with sub-table details via JOIN), fill user names for createdBy/updatedBy
- [x] 4.4 Implement `update(id, data)`: check status=DRAFT, update budget fields, sync pm_budget_item tree (delete removed items, update existing, create new), recalculate cost_baseline and total_budget
- [x] 4.5 Implement `delete(id)`: check status=DRAFT, soft delete budget + all items + sub-table records
- [x] 4.6 Implement `submit(id)`: check status=DRAFT, update version x.1→x.7, status→PENDING, start Flowable process, record processInstanceId

## 5. Backend — BO (Budget Approval Callback)

- [x] 5.1 Create `BudgetApprovalCallback` component: `@Component` implements `ApprovalCompletedCallback`, `getFlowCode()` returns `"PM_BUDGET_APPROVAL"`, auto-registered via `ApprovalCallbackRegistry`
- [x] 5.2 Implement `onApproved(bizId, params)`: version x.7→(x+1).0, status→APPROVED, save approval_comment
- [x] 5.3 Implement `onRejected(bizId, rejectReason)`: version x.7→x.1, status→DRAFT, save approval_comment=rejectReason

## 6. Backend — BO (Budget Comparison)

- [x] 6.1 Implement `getComparison(budgetId, version)`: query budget by version (default latest APPROVED), build item tree, for each item SUM(pm_actual_cost.amount WHERE budget_item_id IN descendants), calculate ratio
- [x] 6.2 Implement `getVersionHistory(projectId)`: list all budgets for project ordered by version, filter by status=APPROVED
- [x] 6.3 Ensure parent item actualAmount = sum of all children actualAmount (recursive aggregation)

## 7. Backend — BO (Actual Cost)

- [x] 7.1 Create `WhPmActualCostBo` with `@Service` + `@Transactional`: `pageList(pageNum, pageSize, projectId, budgetItemId, sourceSystem)` using `LambdaQueryWrapper` + `Page<T>`
- [x] 7.2 Implement `create(data)`: validate budgetItemId exists and matches costType, insert record, fill sourceSystem/sourceRef
- [x] 7.3 Implement `delete(id)`: check sourceSystem="manual", soft delete
- [x] 7.4 Implement `getDetail(id)`: return entity + budget item category name

## 8. Backend — Cost Event Bus

- [x] 8.1 Create `CostEvent` class: fields projectId, budgetItemId, amount (BigDecimal), costDate (LocalDate), sourceSystem (enum), sourceRef (String), sourceId (String)
- [x] 8.2 Create `CostEventPublisher` interface: `void publish(CostEvent event)`
- [x] 8.3 Create `DefaultCostEventPublisher` implementation: `@Component` implements `CostEventPublisher`, writes to `pm_actual_cost` table
- [x] 8.4 Create `CostEventListener` component: listens for Spring events or direct calls, validates budget item exists, creates actual cost record

## 9. Backend — BO & Controller (Cost Warning)

- [x] 9.1 Create `WhPmCostWarningBo` with `@Service` + `@Transactional`: `listByProjectId(projectId, status)`
- [x] 9.2 Implement `closeWarning(id, userId, role)`: validate user has permission (WARN/CRITICAL requires ROLE_ADMIN), update status=CLOSED, closedBy, closedAt
- [x] 9.3 Implement `triggerCalculation()`: query all APPROVED budgets, calculate ratio for each, create/update pm_cost_warning records per threshold rules
- [x] 9.4 Create `CostWarningScheduledTask` component: `@Scheduled` with cron from `cost.warning.cron` config, calls `triggerCalculation()`
- [x] 9.5 Create `WhPmCostWarningController` at `/api/pm/cost-warnings`: `GET` list, `POST /{id}/close`, `POST /trigger`

## 10. Backend — Controller (Budget)

- [x] 10.1 Create `WhPmBudgetController` at `/api/pm/budgets`: constructor injection of `WhPmBudgetBo`, endpoints: `GET` (list), `GET /{id}` (detail), `POST` (create), `PUT /{id}` (update), `DELETE /{id}` (delete), `POST /{id}/submit`, all returning `R<T>`
- [x] 10.2 Add approval endpoints: `POST /{id}/approve`, `POST /{id}/reject` (called by sponsor to complete Flowable task)
- [x] 10.3 Add comparison endpoint: `GET /{id}/comparison` (calls comparison BO)
- [x] 10.4 Add versions endpoint: `GET /versions/{projectId}` (returns version history)
- [x] 10.5 Add Knife4j annotations for API documentation

## 11. Backend — BPMN Workflow

- [x] 11.1 Create BPMN file at `resources/bpmn/pm-budget-approval.bpmn20.xml`: process id=`PM_BUDGET_APPROVAL`, one userTask with dynamic approver (from process variable `approverId`), exclusiveGateway for APPROVED/REJECTED, endEvents with `flowable:executionListener delegateExpression="${flowableProcessEndListener}"`
- [x] 11.2 Register `BudgetApprovalCallback` in `ApprovalCallbackRegistry` (auto-registration via `@Component` + `@PostConstruct`)

## 12. Backend — Integration & Testing

- [x] 12.1 Verify `SqliteBootstrap` picks up new migration scripts
- [x] 12.2 Verify budget creation with all 7 primary categories works end-to-end
- [x] 12.3 Verify version progression: v0.1→v0.7→v1.0→v1.1→v1.7→v2.0
- [x] 12.4 Verify approval callback: approve → APPROVED, reject → DRAFT
- [x] 12.5 Verify comparison API: all budget items have actualAmount (0 if no data)
- [x] 12.6 Verify cost event publisher: manual creation writes to pm_actual_cost
- [x] 12.7 Verify scheduled task: triggers warning creation at configured time
- [x] 12.8 Verify delete constraint: only DRAFT budgets deletable
- [x] 12.9 Verify update constraint: only DRAFT budgets editable
- [x] 12.10 Verify single APPROVED version constraint per project

## 13. Frontend — Setup & API Layer

- [x] 13.1 Create `src/api/pm/budget.js` with all API functions: `getBudgetListApi`, `getBudgetDetailApi`, `createBudgetApi`, `updateBudgetApi`, `deleteBudgetApi`, `submitBudgetApi`, `approveBudgetApi`, `rejectBudgetApi`, `getBudgetComparisonApi`, `getBudgetVersionsApi`
- [x] 13.2 Create `src/api/pm/actualCost.js` with: `getActualCostListApi`, `createActualCostApi`, `deleteActualCostApi`
- [x] 13.3 Create `src/api/pm/costWarning.js` with: `getCostWarningListApi`, `closeCostWarningApi`, `triggerCostWarningApi`
- [x] 13.4 Add routes in `src/router/index.js`: `/pm/budget` (BudgetList, group: '项目管理', perm: 'ROLE_PM'), `/pm/budget/form` (BudgetForm, hidden), `/pm/budget/comparison/:projectId` (BudgetComparison, hidden)

## 14. Frontend — Budget Create/Edit Form

- [x] 14.1 Create `src/views/pm/budget/form.vue` with header: back button + dynamic title ("新增预算" / "编辑预算")
- [x] 14.2 Implement primary category section: 7 expandable rows (人工、采购、差旅、商务费用、客户招待费、活动费、其他), each with amount input
- [x] 14.3 Implement LABOR secondary items: add/remove rows with role select (from sys_role), hours input (el-input-number), cost rate input (el-input-number), auto-calculate amount = hours × rate
- [x] 14.4 Implement PROCUREMENT secondary items: add/remove rows with BOM item name input, qty input, unit price input, auto-calculate amount
- [x] 14.5 Implement OTHER secondary items (TRAVEL/BUSINESS/ENTERTAINMENT/ACTIVITY/OTHER): add/remove rows with description input and amount input
- [x] 14.6 Implement management reserve input at bottom
- [x] 14.7 Auto-calculate cost_baseline (sum of all primary categories) and total_budget (cost_baseline + management_reserve)
- [x] 14.8 Form validation: all amounts >= 0, hours > 0 for labor items, role required for labor items, BOM item name required for procurement items
- [x] 14.9 Submit: call createBudgetApi or updateBudgetApi with full tree structure, show success message, navigate back
- [x] 14.10 Load existing budget on edit: call getBudgetDetailApi, populate all sections

## 15. Frontend — Budget List Page

- [x] 15.1 Create `src/views/pm/budget/index.vue` with standard pattern: el-card → header with title "预算管理" + search + pagination
- [x] 15.2 Implement table columns: 预算编码(budgetCode), 版本号(version), 成本基准(costBaseline), 管理储备(managementReserve), 总预算(totalBudget), 状态(status, el-tag), 创建人(createdBy), 创建日期(createDate), 操作(actions)
- [x] 15.3 Implement per-row actions: 查看(Detail), 编辑(Edit, DRAFT only), 删除(Delete, DRAFT only), 提交审批(Submit, DRAFT only), 预实对比(Comparison, APPROVED only)
- [x] 15.4 Implement status filter dropdown: DRAFT/PENDING/APPROVED/REJECTED
- [x] 15.5 Data loading with pagination, `onMounted` and `onActivated` for keep-alive refresh

## 16. Frontend — Budget Comparison Page

- [x] 16.1 Create `src/views/pm/budget/comparison.vue` with header: back button + title "预实对比" + version switch dropdown
- [x] 16.2 Implement two-panel layout: left panel = budget item tree, right panel = actual amounts
- [x] 16.3 Left panel: tree table showing category hierarchy (primary → secondary), budget amount, role/BOM details for secondary items
- [x] 16.4 Right panel: for each budget row, show actual amount, ratio (percentage), ratio color coding (green < 80%, yellow 80-95%, orange 95-100%, red > 100%)
- [x] 16.5 Strict 1:1 correspondence: every budget row has a matching actual amount row (0 if no data)
- [x] 16.6 Total row at bottom: sum of all primary category budget amounts and actual amounts
- [x] 16.7 Version switch dropdown: default latest APPROVED, list all APPROVED versions
- [x] 16.8 Click row to drill down to actual cost detail: `router.push({ path: '/pm/actual-costs', query: { budgetItemId: xxx } })`
- [x] 16.9 Load data on mount: `getBudgetComparisonApi(budgetId, { version })`, display totals and ratio

## 17. Frontend — Actual Cost Management

- [x] 17.1 Create `src/views/pm/actual-cost/index.vue` (can be a dialog or standalone page) with table showing: 日期(costDate), 科目(category), 金额(amount), 来源(sourceSystem), 备注(sourceRef), 操作(actions)
- [x] 17.2 Implement create dialog: budget item select (tree picker), cost date, amount (positive/negative), source ref (备注)
- [x] 17.3 Implement delete action: only for sourceSystem="manual", with confirmation dialog
- [x] 17.4 Implement filter by projectId and budgetItemId

## 18. Frontend — Dashboard Warning Card

- [x] 18.1 Modify `src/views/dashboard/index.vue`: add cost warning card component below existing stat cards
- [x] 18.2 Card content: query `getCostWarningListApi({ status: 'ACTIVE' })`, display up to 5 warnings with project name, level (el-tag: INFO=blue, WARN=orange, CRITICAL=red), ratio percentage
- [x] 18.3 "查看更多" link: navigate to warning list page or filter budget list
- [x] 18.4 Auto-refresh: poll every 5 minutes or refresh on page activate (onActivated)

## 19. Frontend — Sidebar Menu Update

- [x] 19.1 Add "预算管理" menu entry in `src/components/layout/SidebarMenu.vue` under 项目管理 group, perm: 'ROLE_PM', route: '/pm/budget'

## 20. Integration & Testing

- [x] 20.1 Verify frontend API calls match backend endpoint paths (baseURL `/api` + path `/pm/budgets/...`)
- [x] 20.2 Verify response parsing: `res.data.records` for list, `res.data.total` for count, `res.data` for single entity
- [x] 20.3 Test budget CRUD: create with all 7 categories, edit (DRAFT only), delete (DRAFT only)
- [x] 20.4 Test approval workflow: submit → sponsor approve → APPROVED (v1.0), submit → sponsor reject → DRAFT
- [x] 20.5 Test version progression: adjust approved budget → v1.1 → submit → v1.7 → approve → v2.0
- [x] 20.6 Test comparison page: all budget rows show actual amounts (0 if no data), ratios correct
- [x] 20.7 Test actual cost creation: manual entry with budget item selection, verify amount appears in comparison
- [x] 20.8 Test warning card: trigger calculation, verify warnings appear on dashboard
- [x] 20.9 Test warning close: WARN/CRITICAL requires admin, INFO auto-closes
- [x] 20.10 Test negative actual cost (red冲): verify ratio decreases accordingly
- [x] 20.11 Test single APPROVED version constraint: create new version while one exists
