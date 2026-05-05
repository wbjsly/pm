## 1. Database DDL & Bootstrap

- [x] 1.1 Create `erp_product` table DDL in `resources/db/sqlite/` migration script (columns: ID VARCHAR PK, PRODUCT_CODE TEXT UNIQUE, PRODUCT_NAME TEXT, STATUS TEXT DEFAULT 'ACTIVE', DESCRIPTION TEXT, REMARKS TEXT, DEL_FLAG, CREATE_BY, CREATE_DATE, UPDATE_BY, UPDATE_DATE, VER_NO, SYS_CODE)
- [x] 1.2 Create `erp_module` table DDL in `resources/db/sqlite/` migration script (columns: ID VARCHAR PK, PRODUCT_ID VARCHAR FK→erp_product, MODULE_CODE TEXT, MODULE_NAME TEXT, DESCRIPTION TEXT, + audit columns)
- [x] 1.3 Create `pm_wbs_version` table DDL in `resources/db/sqlite/` migration script (columns: ID VARCHAR PK, WBS_ID VARCHAR FK→pm_wbs_element, VERSION_NUMBER DECIMAL(4,1), PLANNED_START_DATE DATE, PLANNED_END_DATE DATE, ACTUAL_START_DATE DATE, ACTUAL_END_DATE DATE, CREATE_BY, CREATE_DATE)
- [x] 1.4 Create ALTER TABLE script for `pm_wbs_element` to add: PRODUCT_ID, MODULE_ID, PRIORITY, TECH_DIFFICULTY, PLANNED_OWNER_ID, LATEST_PLANNED_END_DATE, ACTUAL_END_DATE, ACTUAL_COMPLETED_BY (all columns use uppercase naming per SQLite convention)
- [ ] 1.5 Verify `SqliteBootstrap` picks up new scripts on startup (check `wh_bootstrap_marker` table)

## 2. Backend — Entity & DAO (Product Master Data)

- [x] 2.1 Create `ErpProduct` entity: `extends BaseEntity`, `@TableName("erp_product")`, fields with `@TableField("COLUMN_NAME")` (uppercase), `id` as String UUID via `@TableId(type = IdType.ASSIGN_UUID)`, status enum ACTIVE/INACTIVE/PLACEHOLDER
- [x] 2.2 Create `ErpModule` entity: same pattern, `@TableName("erp_module")`, `productId` as String FK
- [x] 2.3 Create `ErpProductDao extends BaseMapper<ErpProduct>` (MyBatis Plus mapper interface, `@Mapper`)
- [x] 2.4 Create `ErpModuleDao extends BaseMapper<ErpModule>`

## 3. Backend — Product Master Data BO & Controller

- [x] 3.1 Create `ErpProductBo` with `@Service` + `@Transactional`: CRUD methods using `LambdaQueryWrapper` and `Page<T>`, placeholder auto-creation (status=PLACEHOLDER, product_name=product_code)
- [x] 3.2 Create `ErpProductController` at `/api/pm/products` with constructor injection: `GET` list (pageNum, pageSize, keyword), `GET /{id}`, `POST`, `PUT /{id}`, `DELETE /{id}`, all returning `R<T>`
- [x] 3.3 Implement unique product_code validation: query by code, return `R.fail(409, "产品编码已存在")` on duplicate

### 3.4 Module Master Data

- [x] 3.4.1 Create `ErpModuleBo` with `@Service` + `@Transactional`: CRUD methods, `listByProductId(String productId)`
- [x] 3.4.2 Create `ErpModuleController` at `/api/pm/modules` with constructor injection: `GET` list, `GET /by-product/{productId}`, `GET /{id}`, `POST`, `PUT /{id}`, `DELETE /{id}`, all returning `R<T>`

## 4. Backend — WBS Entity & DAO

- [x] 4.1 Extend existing `WhPmWbsElement` entity (if exists) or create new: `extends BaseEntity`, `@TableName("pm_wbs_element")`, all fields with `@TableField("COLUMN_NAME")` uppercase mapping. Key fields: `wbsCode`, `parentId` (String), `level`, `name`, `description`, `elementType`, `effortEstimate`, `budgetEstimate`, `productId` (String), `moduleId` (String), `priority`, `techDifficulty`, `plannedOwnerId` (String), `ownerId` (String), `latestPlannedEndDate`, `actualEndDate`, `actualCompletedBy`, `status`. Add `@TableField(exist = false)` for display fields like `productName`, `moduleName`, `plannedOwnerName`, `ownerName`
- [x] 4.2 Create `WhPmWbsVersion` entity: `extends BaseEntity`, `@TableName("pm_wbs_version")`, fields: `wbsId`, `versionNumber` (BigDecimal), `plannedStartDate`, `plannedEndDate`, `actualStartDate`, `actualEndDate`
- [x] 4.3 Create `WhPmWbsElementDao extends BaseMapper<WhPmWbsElement>`: add custom `@Select` for tree query (recursive CTE or parent-child join), `@Select` for finding children by parentId, `@Delete` for physicalDeleteById
- [x] 4.4 Create `WhPmWbsVersionDao extends BaseMapper<WhPmWbsVersion>`: add `@Select` for versions by wbsId ordered by versionNumber DESC

## 5. Backend — WBS Core CRUD (BO + Controller)

- [x] 5.1 Create `WhPmWbsElementBo` with `@Service` + `@Transactional`: `pageList(pageNum, pageSize, projectId, status, keyword)` using `LambdaQueryWrapper` + `Page<T>`, call `fillUserNames()` to resolve userId → realName for ownerId/plannedOwnerId
- [x] 5.2 Implement `create()`: auto-generate wbsCode (N.0 for root, parentCode.childIndex for children), set status="PLANNED", call `dao.insert()`, create initial version record (0.1)
- [x] 5.3 Implement `update()`: check status constraints (PLANNED editable freely, IN_PROGRESS triggers approval workflow), for PLANNED update + auto-recalculate effort, for IN_PROGRESS store pending changes and trigger Flowable
- [x] 5.4 Implement `delete()`: reject if `hasChildren()` returns true (throw `ServiceException("存在子节点，禁止删除")`), else `physicalDeleteById()`
- [x] 5.5 Implement `getDetail(id)`: return entity + latest version + filled names, throw `ServiceException(404, "WBS节点不存在")` if not found or delFlag="1"
- [x] 5.6 Implement 100% rule: `recalculateEffort(String nodeId)` — query all direct children, sum their `effortEstimate`, update parent, recursively propagate to root
- [x] 5.7 Implement `suspend(id)`: PLANNED→reject, IN_PROGRESS→SUSPENDED, COMPLETED→reject
- [x] 5.8 Implement `resume(id)`: SUSPENDED→IN_PROGRESS, others→reject
- [x] 5.9 Implement `reopen(id)`: COMPLETED→trigger Flowable approval (approval_type=REOPEN), others→reject
- [x] 5.10 Create `WhPmWbsElementController` at `/api/pm/wbs`: constructor injection of `WhPmWbsElementBo`, endpoints: `GET` (list), `GET /{id}` (detail), `POST` (create), `PUT /{id}` (update), `DELETE /{id}` (delete), `POST /{id}/suspend`, `POST /{id}/resume`, `POST /{id}/reopen`, all returning `R<T>`

## 6. Backend — WBS Version Management

- [x] 6.1 Implement version auto-creation: on planned date change, create new `WhPmWbsVersion` with version_number = max(version_number) + 0.1, use `BigDecimal` arithmetic (`new BigDecimal("0.1").add(maxVersion)`)
- [x] 6.2 Implement `latestPlannedEndDate` auto-sync: after version creation, update parent element's `latestPlannedEndDate` from new version's `plannedEndDate`
- [x] 6.3 Implement version history endpoint in Bo: `getVersionHistory(wbsId)` returns list ordered by versionNumber DESC
- [x] 6.4 Implement actual date recording: if only actual dates change (no planned date change), update current version's actual dates without creating new version; set `dateType=1` implied by non-null actual dates
- [x] 6.5 Add `GET /api/pm/wbs/{id}/versions` endpoint in Controller

## 7. Backend — WBS Import/Export

- [x] 7.1 Create CSV template file (`resources/templates/wbs_import_template.csv`) with headers: 序号,名称,父节点名称,产品类型,产品编码,模块编码,优先级,技术难度,计划责任人,估算工时(小时),估算成本(元),描述
- [x] 7.2 Implement template download endpoint: `GET /api/pm/wbs/template` returns CSV file response with `Content-Disposition: attachment`
- [x] 7.3 Implement CSV import in Bo: two-pass processing — pass 1: create root nodes (parentName empty), store name→id mapping; pass 2: create child nodes resolving parentName via (a) in-batch map, (b) existing nodes by name, (c) degrade to root if not found
- [x] 7.4 Implement import result: return `{total, success, degraded, failed, details[]}` where details contain `{row, name, status, reason}` for degraded/failed rows
- [x] 7.5 Implement placeholder creation during import: if product_code not found in `erp_product`, create PLACEHOLDER record; same for module_code under resolved product
- [x] 7.6 Implement Excel export: query all WBS for project in tree order, populate latest version dates, generate .xlsx file response
- [x] 7.7 Add Controller endpoints: `POST /api/pm/wbs/import` (MultipartFile upload), `GET /api/pm/wbs/export` (projectId param), `GET /api/pm/wbs/template`

## 8. Backend — Approval Workflow

- [x] 8.1 Create BPMN file at `resources/bpmn/pm-wbs-modify-approval.bpmn20.xml`: process id=`PM_WBS_MODIFY_APPROVAL`, two userTask nodes (BA审批 → PM审批), exclusiveGateway for approval decision, endEvent with `flowable:executionListener delegateExpression="${flowableProcessEndListener}"`, BA node with conditional skip via `${skipBaApproval == true || approvalResult == 'SKIPPED'}`
- [x] 8.2 Implement `WbsApprovalCallback` component: `@Component` implements `ApprovalCompletedCallback`, `getFlowCode()` returns `"PM_WBS_MODIFY_APPROVAL"`, registered automatically via `ApprovalCallbackRegistry`
- [x] 8.3 Implement `onApproved(bizId, params)`: apply pending changes (stored before workflow start), create new version record (version_number + 0.1), sync `latestPlannedEndDate`, if approval_type=REOPEN set status=IN_PROGRESS
- [x] 8.4 Implement `onRejected(bizId, rejectReason)`: discard pending changes, notify submitter (log for now), if approval_type=REOPEN keep status=COMPLETED
- [x] 8.5 Implement `startApproval(wbsId, approvalType, content)`: store pending changes (use a `pm_wbs_pending_change` temp table or JSON field in version record), start Flowable process with variables `{flowCode: "PM_WBS_MODIFY_APPROVAL", bizId: wbsId, approvalType, submitterId, skipBaApproval: (submitterIsBA)}`
- [ ] 8.6 Add sequence record in `wh_sequence` table for WBS code generation (seqName: "PM_WBS", prefix: "WBS-" or use project-based coding)

## 9. Frontend — Setup & API Layer

- [x] 9.1 Create `src/api/pm/wbs.js` with all API functions following naming convention `[action][Resource]Api`: `getWbsListApi`, `getWbsDetailApi`, `createWbsApi`, `updateWbsApi`, `deleteWbsApi`, `suspendWbsApi`, `resumeWbsApi`, `reopenWbsApi`, `getWbsVersionsApi`, `importWbsApi`, `exportWbsApi`, `downloadWbsTemplateApi`, `getProjectsForWbsApi`
- [x] 9.2 Request pattern: `request.get('/pm/wbs', { params })`, `request.post('/pm/wbs', data)`, etc. (baseURL `/api` already configured in `request.js`)
- [x] 9.3 Add routes in `src/router/index.js`: `/pm/wbs` (WbsList, group: '项目管理', perm: 'ROLE_PM'), `/pm/wbs/detail/:id` (WbsDetail, hidden), `/pm/wbs/form` (WbsForm, hidden), `/pm/wbs/form/:id` (WbsEdit, hidden), `/pm/wbs/history/:id` (WbsHistory, hidden)
- [ ] 9.4 Add sidebar menu entry in `src/components/layout/SidebarMenu.vue` (if applicable)

## 10. Frontend — WBS List Page (Multi-Project Accordion)

- [x] 10.1 Create `src/views/pm/wbs/index.vue` with standard pattern: `<div class="wbs-list">` → `<el-card>` → header with title "WBS管理" + pagination info + search
- [x] 10.2 Implement project-level pagination: call `getProjectsForWbsApi({ pageNum, pageSize: 5 })`, store project list, display as accordion headers with `▶/▼` icon, project name, code, PM name, status
- [x] 10.3 Implement mutual exclusion: `expandedProjectId` ref, when setting to a new value first collapse previous, then `loadData(newProjectId)` for that project's WBS tree
- [x] 10.4 Implement WBS tree table inside expanded project: `<el-table :data="wbsTreeData" row-key="id" :tree-props="{children: 'children'}" v-loading="loading" stripe>`, lazy load via `loadData(projectId)` on expand
- [x] 10.5 Implement table columns: 编码(wbsCode, width 100), 名称(name, min-width 120), 产品(productName, width 100), 模块(moduleName, width 80), 优先级(priority, width 80, tag), 技术难度(techDifficulty, width 80, tag), 状态(status, width 90, tag), 计划完成(latestPlannedEndDate, width 110), 实际完成(actualEndDate, width 110), 操作(width 200, fixed right)
- [x] 10.6 Implement per-row action buttons with `el-tooltip` + `el-button link` + icons from `@element-plus/icons-vue`: 查看(View), 编辑(Edit, PLANNED only), 删除(Delete, PLANNED/SUSPENDED only), 暂停(Pause, IN_PROGRESS), 恢复(Resume, SUSPENDED), 重新打开(RefreshRight, COMPLETED) — all conditionally rendered by `row.status`
- [x] 10.7 Implement project header action buttons: 新增WBS (el-button primary + Plus), 导入 (el-button + Upload), 导出 (el-button + Download) — visible only for expanded project
- [x] 10.8 Use `onMounted(loadData)` and `onActivated(loadData)` for keep-alive refresh pattern
- [x] 10.9 Data loading: `loading.value = true`, `try { const res = await getWbsListApi(queryParams); tableData.value = res.data.records || []; total.value = res.data.total || 0 } catch { ElMessage.error('加载数据失败') } finally { loading.value = false }`

## 11. Frontend — WBS Create/Edit Form

- [x] 11.1 Create `src/views/pm/wbs/form.vue` with standard pattern: header with back button (`<el-button @click="$router.back()">返回</el-button>`) + dynamic title (`isEdit ? '编辑WBS' : '新增WBS'`)
- [x] 11.2 Use `<el-form :model="form" :rules="rules" ref="formRef" label-width="120px">` with fields: name, product (el-select cascading with module), module (el-select, filtered by product), priority (el-select: MUST/SHOULD/COULD/WONT with Chinese labels), techDifficulty (el-select: LOW/MEDIUM/HIGH), plannedOwner (el-select users), owner (el-select users), effortEstimate (el-input-number), budgetEstimate (el-input-number), plannedStartDate (el-date-picker), plannedEndDate (el-date-picker), description (el-input textarea)
- [x] 11.3 Implement product→module cascading: watch productId, filter module options
- [x] 11.4 Form validation rules: name required, effortEstimate > 0, budgetEstimate >= 0, plannedEndDate >= plannedStartDate
- [x] 11.5 Submit: `await formRef.value.validate()`, call `createWbsApi(form)` or `updateWbsApi(id, form)`, `ElMessage.success('保存成功')`, `router.push('/pm/wbs')`
- [x] 11.6 WBS code display: read-only el-input showing "系统自动生成" on edit mode (isEdit=true)

## 12. Frontend — WBS Detail Page

- [x] 12.1 Create `src/views/pm/wbs/detail.vue` with standard pattern: header with back button + title "WBS详情"
- [x] 12.2 Use `<el-descriptions :column="2" border>` to display all fields: 编码, 名称, 产品, 模块, 优先级(el-tag), 技术难度(el-tag), 计划责任人, 实际负责人, 估算工时, 估算成本, 计划开始, 计划完成, 实际完成, 实际完成人, 状态(el-tag), 描述
- [x] 12.3 Display latest version number prominently: `版本 {{ latestVersion }}` as clickable `<el-link type="primary" @click="$router.push('/pm/wbs/history/' + id)">版本 0.5 →</el-link>`
- [x] 12.4 Conditional action buttons based on status: Edit (PLANNED), Pause (IN_PROGRESS), Resume (SUSPENDED), Reopen (COMPLETED) — use `el-button` with icons, call respective API on click with `ElMessageBox.confirm`
- [x] 12.5 Load detail in `onMounted`: `const res = await getWbsDetailApi(route.params.id); detail.value = res.data`

## 13. Frontend — Version History Page

- [x] 13.1 Create `src/views/pm/wbs/history.vue` with header: back button + title "版本历史: {{ wbsName }} ({{ wbsCode }})"
- [x] 13.2 Implement version table: `<el-table :data="versions" stripe>`, columns: 版本号(versionNumber, width 80), 计划开始(plannedStartDate, width 120), 计划结束(plannedEndDate, width 120), 实际开始(actualStartDate, width 120, show "-" if null), 实际结束(actualEndDate, width 120, show "-" if null), 创建时间(createDate, width 160), 创建人(createBy, width 100)
- [x] 13.3 Rows with non-null actual dates should display a small indicator (e.g., el-tag type="success" size="small" "含实际数据") in the version number column
- [x] 13.4 Load data: `const res = await getWbsVersionsApi(route.params.id); versions.value = res.data`

## 14. Frontend — Import Dialog

- [x] 14.1 Create import dialog within `index.vue` or as separate component: `<el-dialog v-model="importDialogVisible" title="批量导入WBS" width="600px">`
- [x] 14.2 Include file upload: `<el-upload drag :auto-upload="false" :on-change="handleFileSelect" accept=".csv">` with drag area and "点击或拖拽文件到此区域"
- [x] 14.3 Template download link: `<el-link type="primary" @click="handleDownloadTemplate">下载导入模板</el-link>`
- [x] 14.4 On file select: call `importWbsApi(file)` with FormData, show loading spinner
- [x] 14.5 Import result display: three stat cards (成功/降级/失败) with color coding (green/yellow/red), detail table showing row number, name, status, reason for degraded/failed rows
- [x] 14.6 After import: `ElMessage.success('导入完成')`, `loadData()` to refresh WBS tree

## 15. Integration & Testing

- [ ] 15.1 Verify frontend API calls match backend endpoint paths exactly (baseURL `/api` + path `/pm/wbs/...`)
- [ ] 15.2 Verify response parsing: `res.data.records` for list, `res.data.total` for count, `res.data` for single entity (per `R<T>` wrapper pattern)
- [ ] 15.3 Test WBS CRUD with 100% rule: create 4-level deep tree, update leaf effort, verify all ancestors recalculated
- [ ] 15.4 Test import: CSV with mixed root/child rows, verify degraded parent fallback, verify placeholder product creation
- [ ] 15.5 Test approval workflow: non-BA user modifies IN_PROGRESS WBS → BA approval → PM approval → changes applied + new version created
- [ ] 15.6 Test approval workflow: BA user modifies IN_PROGRESS WBS → BA step auto-skipped → PM approval only
- [ ] 15.7 Test Reopen: COMPLETED WBS → Reopen → BA approval → PM approval → status → IN_PROGRESS + new version
- [ ] 15.8 Test rejection: any approval rejected → original data preserved, user notified
- [ ] 15.9 Test export: verify Excel file contains all WBS in tree order with latest version dates
- [ ] 15.10 Test status constraints via UI: delete button hidden for COMPLETED, pause hidden for PLANNED, etc.
- [ ] 15.11 Test accordion mutual exclusion: expand project B → project A auto-collapses
- [ ] 15.12 Test pagination: verify >5 projects show pagination controls, 5 per page
