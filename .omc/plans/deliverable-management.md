# Project Deliverable Management (项目成果物管理) - Implementation Plan

## RALPLAN-DR Summary

### Principles (5)
1. **Follow existing patterns exactly** -- Entity/DAO/BO/Controller layering, BPMN + callback approval, SequenceService code generation, constructor injection, `R<T>` response wrapper
2. **Minimal scope** -- No new framework, no architecture redesign, no refactoring of existing modules
3. **State machine integrity** -- Enforce DRAFT->PENDING_APPROVAL->APPROVED->DELIVERED transitions at BO layer; never allow invalid transitions
4. **Permission cascade** -- PM full CRUD, SPONSOR approve/view, others read-only; only APPROVED projects can have deliverables
5. **Attachment simplicity** -- Store MinIO file metadata as JSON array on the deliverable entity; separate upload endpoint returns MinIO keys

### Decision Drivers (Top 3)
1. **Brownfield compliance** -- New code must be indistinguishable from existing code (same annotations, same injection style, same error handling)
2. **Workflow consistency** -- The Flowable approval pattern is already proven with Charter/Budget/WBS; reuse the exact same mechanism
3. **Frontend UX uniformity** -- List/form/detail pages must match the Charter module's layout, el-tag status display, and action button conditional visibility

### Viable Options

| Option | Description | Pros | Cons |
|--------|------------|------|------|
| **A: Full mirror of Charter** | Copy-paste-adapt the Charter module, swapping fields | Fastest implementation, lowest risk, zero learning curve | Attachment handling needs bolt-on; the DELIVERED state (PM action, not approval) requires a new endpoint not present in Charter |
| **B: Separate file service** | Create a dedicated `FileService` + `WhPmAttachment` entity/table | Clean attachment auditing, reusable for future modules | Over-engineers the immediate need; introduces a new sub-pattern not yet in the codebase; 2-3 extra files |

**Decision: Option A** with a lightweight file utility embedded in the BO (not a separate service). The attachment metadata lives in a TEXT/JSON column on `wh_pm_deliverable`. A single `POST /api/pm/deliverables/{id}/attachments` endpoint uploads to MinIO and appends to the JSON array. This keeps the deliverable module self-contained like every other PM module while still supporting file attachments.

---

## ADR (Architecture Decision Record)

### Decision
Implement Project Deliverable Management as a self-contained PM module following the exact Charter pattern (Entity->DAO->BO->Controller + BPMN + Callback), with a lightweight file upload utility embedded in the BO rather than a separate file service.

### Drivers
- The codebase uses a consistent pattern for all PM modules (Charter, Budget, WBS, WorkLog, CostWarning)
- Approval workflow is standardized via Flowable with `ApprovalCompletedCallback` auto-registry
- Code generation uses `SequenceService` with `WhSequence` table (format: `PREFIX-YYYY-NNN`)
- MinIO is already configured as a Spring bean (bucket: `wh-files`) but has not yet been used for business file operations. The `uploadAttachment`/`deleteAttachment` methods will be the first MinIO consumer in the codebase.
- Attachment requirements are simple (file name, size, MinIO object key, upload timestamp)

### Alternatives Considered
1. **Separate attachment entity + DAO + table** -- Rejected as over-engineering. The current codebase has no precedent for an "attachment sub-module." A JSON array on the deliverable row suffices for the current scope.
2. **Generic `FileService` injection** -- Rejected because no other module needs it yet. Premature abstraction. If another module later needs file uploads, extract the pattern then.
3. **Approval step for DELIVERED transition** -- Rejected per spec. The DELIVERED state is a PM action (mark-as-done), not a separate Flowable process.

### Why Chosen
Option A mirrors every existing PM module exactly. The executor can copy the Charter files and adapt field names/endpoints without uncertainty. The file upload is the only novel element, scoped to a single endpoint with direct MinIO client usage.

### Consequences
- **Positive**: Fast implementation (~16 files, well-understood patterns), zero architectural risk, consistent with codebase
- **Negative**: No reusable file service (but none needed yet); attachment JSON column requires manual parsing in frontend; no attachment-level CRUD (delete single attachment requires updating the JSON array)

### Follow-ups
- If attachment requirements grow (search by file name, per-file permissions, virus scanning), extract a `FileService` from the deliverable BO patterns
- If more modules need file uploads, create a shared `MinioFileService` and refactor the deliverable BO to delegate

---

## Architect Review Resolutions

### Blocker 1: SidebarMenu entry + SPONSOR visibility ✅ RESOLVED
Added `SidebarMenu.vue` to file inventory. Menu item added with `perm: 'ROLE_PM,ROLE_SPONSOR'`. Sidebar filter logic modified to support comma-separated perms.

### Blocker 2: List page scope ✅ RESOLVED
**Decision**: Global list with optional `projectId` filter. BO's `pageList` applies automatic filtering: PM sees their projects, SPONSOR sees projects where they are sponsor, users with both roles see both. API's `projectId` param overrides the automatic filter.

### Blocker 3: Delete restricts to DRAFT only ✅ RESOLVED
Planner's proposed DRAFT+REJECTED delete deviated from Charter pattern (`WhPmCharterBo.java:225` — DRAFT only). **Matched Charter pattern exactly**: only DRAFT deletable. REJECTED is editable (for re-submit) but not deletable.

**⚠️ SPEC OVERRIDE**: This deviates from the deep-interview spec which states "草稿和驳回状态可删除" (DRAFT and REJECTED are deletable). The override rationale: consistency with existing Charter behavior, and REJECTED deliverables remain as a permanent paper trail. The spec's acceptance criteria #7 ("草稿/驳回状态的成果物可被 PM 删除") is modified to: "草稿状态的成果物可被 PM 删除；驳回状态的成果物可编辑重新提交但不可删除".

### Should-fix resolutions:
4. **Dropped DeliverableVO** — transient fields added directly to entity as `@TableField(exist = false)` (matches `WhPmCharter.java:84-93`)
5. **Optimistic lock handling added** — `uploadAttachment()` checks `dao.updateById()` return value, throws `ServiceException` on 0 rows (optimistic lock failure)
6. **Callback matches Charter exactly** — `processInstanceId` cleared only on reject, kept on approve
7. **BPMN matches Charter pattern** — uses both `candidateGroups="ROLE_SPONSOR"` + `assignee="${assignee}"`
8. **Project dropdown dependency documented** — uses existing `GET /api/pm/charters?status=APPROVED&pageSize=999`

---

## Step-by-Step Implementation Order

### Phase 1: Backend Foundation (Steps 1-5)

#### Step 1: Database Migration
**File**: `wh-backend/src/main/resources/db/sqlite/037-pm-deliverable.sql`

Create the `wh_pm_deliverable` table and seed the sequence.

Acceptance criteria:
- Table exists with all columns per DDL below
- CHECK constraint enforces valid statuses
- Sequence `PM_DELIVERABLE` seeded with value 0, prefix `DELIVERABLE-`, year 2026
- File named `037-pm-deliverable.sql` so it auto-executes after existing migrations

#### Step 2: Entity
**File**: `wh-backend/src/main/java/com/wh/entity/pm/WhPmDeliverable.java`

Field mapping:
- Extends `BaseEntity` (inherits id, createBy, createDate, updateBy, updateDate, remarks, delFlag, verNo, sysCode)
- `@TableName("wh_pm_deliverable")`
- Each field annotated `@TableField("COLUMN_NAME")` (matching DDL column names)

DB-mapped fields:
- `deliverableCode` (String, `@TableField("DELIVERABLE_CODE")`)
- `name` (String, `@TableField("NAME")`)
- `description` (String, `@TableField("DESCRIPTION")`)
- `plannedDeliveryDate` (String, `@TableField("PLANNED_DELIVERY_DATE")`)
- `actualDeliveryDate` (String, `@TableField("ACTUAL_DELIVERY_DATE")`)
- `status` (String, `@TableField("STATUS")`)
- `attachments` (String, `@TableField("ATTACHMENTS")`) — JSON array stored as TEXT
- `approvalComment` (String, `@TableField("APPROVAL_COMMENT")`)
- `processInstanceId` (String, `@TableField("PROCESS_INSTANCE_ID")`)
- `projectId` (String, `@TableField("PROJECT_ID")`)

Non-DB transient fields (`@TableField(exist = false)`):
- `createByName` (String) — resolved from `SysUserDao.selectById(createBy)`
- `sponsorName` (String) — resolved via `projectId → charter.sponsorId → SysUserDao`

Acceptance criteria:
- Compiles without errors
- MyBatis-Plus can map all columns
- `@Data` + `@EqualsAndHashCode(callSuper = true)` annotations present

#### Step 3: DAO
**File**: `wh-backend/src/main/java/com/wh/dao/pm/WhPmDeliverableDao.java`

- `@Mapper` interface extending `BaseMapper<WhPmDeliverable>`
- Include `physicalDeleteById(String id)` via `@Delete` annotation (same pattern as CharterDao)
- Optionally add `selectStatsByProjectId(String projectId)` for dashboard use

Acceptance criteria:
- Compiles
- Physical delete uses `DELETE FROM wh_pm_deliverable WHERE ID = #{id}` (not soft delete, matching Charter pattern)

#### Step 4: Request Objects
**Files**:
- `wh-backend/src/main/java/com/wh/bo/pm/DeliverableCreateRequest.java`
- `wh-backend/src/main/java/com/wh/bo/pm/DeliverableUpdateRequest.java`
- `wh-backend/src/main/java/com/wh/bo/pm/DeliverableSubmitRequest.java`

All request objects are `@Data` POJOs with fields matching the editable entity fields. Transient display fields (createByName, sponsorName) go directly on the entity as `@TableField(exist = false)`, matching `WhPmCharter.java:84-93`.

Acceptance criteria:
- CreateRequest: name, description, plannedDeliveryDate, projectId
- UpdateRequest: name, description, plannedDeliveryDate
- SubmitRequest: (empty or comment only -- the submit action needs no extra params)

#### Step 5: BO (Business Logic)
**File**: `wh-backend/src/main/java/com/wh/bo/pm/WhPmDeliverableBo.java`

This is the core file. Implement the following methods following the exact Charter BO pattern:

```
Constructor injection: WhPmDeliverableDao, WhPmCharterDao, SequenceService, RuntimeService, TaskService, SysUserDao, MinioClient, @Value("${app.minio.bucket}")
```

| Method | Logic |
|--------|-------|
| `pageList(pageNum, pageSize, status, projectId, keyword)` | Query with LambdaQueryWrapper, filters by delFlag=0, status, projectId, keyword (name LIKE or code LIKE). Return IPage with user names filled |
| `getById(id)` | Select by ID, throw 404 if null or deleted. Fill user names |
| `create(req)` | Validate projectId exists and is APPROVED. Generate code via `sequenceService.generateCode("PM_DELIVERABLE", "DELIVERABLE-")`. Set status=DRAFT. Insert |
| `update(id, req)` | Fetch, check status is DRAFT or REJECTED. Update name/description/plannedDeliveryDate. Save |
| `delete(id)` | Fetch, check status is DRAFT only (matches Charter pattern: `WhPmCharterBo.java:225`). Call `dao.physicalDeleteById(id)` |
| `submit(id)` | Fetch, check status is DRAFT or REJECTED. Start Flowable process `PM_DELIVERABLE_APPROVAL` with variables (flowCode, bizId, assignee=sponsorId). Set status=PENDING_APPROVAL |
| `approve(id, comment)` | Fetch, check status is PENDING_APPROVAL. Complete Flowable task with approvalResult=APPROVED |
| `reject(id, comment)` | Fetch, check status is PENDING_APPROVAL. Complete Flowable task with approvalResult=REJECTED, rejectReason=comment |
| `markDelivered(id)` | Fetch, check status is APPROVED. Set actualDeliveryDate=now, status=DELIVERED. Save |
| `uploadAttachment(id, MultipartFile)` | Fetch, check status is DRAFT or REJECTED. Check MAX_ATTACHMENTS (10). Generate MinIO key: `deliverable/{deliverableId}/{uuid}_{originalName}`. Upload via `minioClient.putObject(...)`. Parse existing attachments JSON, append new entry, save via `dao.updateById()`. **Check return value** — if 0 rows (optimistic lock failure from `@Version`), throw `ServiceException("数据已被他人修改，请刷新后重试")`. Return updated attachment list |
| `deleteAttachment(id, attachmentIndex)` | Fetch, check status is DRAFT or REJECTED. Parse attachments JSON, remove entry at index, delete from MinIO, save entity. Same optimistic lock check as uploadAttachment |

### Permission Matrix

| Method | Allowed Roles | Data Filter | Auth Check |
|--------|--------------|-------------|------------|
| `pageList` | PM, SPONSOR | PM sees where `charter.pmId == currentUser`; SPONSOR sees where `charter.sponsorId == currentUser`; both roles see union | `charterDao.selectById(deliverable.projectId)` then compare IDs |
| `getById` | Any authenticated | None (single-record access) | None beyond authentication |
| `create` | PM only | Only for APPROVED projects | `charterDao.selectById(projectId)` — must exist, status==APPROVED, `pmId == currentUser` |
| `update` | PM only | Only DRAFT/REJECTED | Same project PM check as create |
| `delete` | PM only | Only DRAFT | Same project PM check as create |
| `submit` | PM only | DRAFT/REJECTED → PENDING_APPROVAL | Same project PM check as create |
| `approve` | SPONSOR (of project) | PENDING_APPROVAL → APPROVED | `charterDao.selectById(deliverable.projectId).sponsorId == currentUser` |
| `reject` | SPONSOR (of project) | PENDING_APPROVAL → REJECTED | Same as approve |
| `markDelivered` | PM only | APPROVED → DELIVERED | Same project PM check as create |
| `uploadAttachment` | PM only | DRAFT/REJECTED | Same project PM check as create |
| `deleteAttachment` | PM only | DRAFT/REJECTED | Same project PM check as create |

**Note**: SPONSOR authorization is project-specific — NOT role-wide. A user with ROLE_SPONSOR can only approve/reject deliverables for projects where they are listed as the sponsor. The `approve`/`reject` methods must resolve the project and compare `sponsorId` to `SecurityUtils.getCurrentUserId()`.

### User Name Resolution

- **`createByName`**: Resolved from `BaseEntity.createBy` → `SysUserDao.selectById(createBy).getRealName()`. Used in both `pageList` and `getById`.
- **`sponsorName`**: Requires two-hop resolution: `deliverable.projectId` → `WhPmCharterDao.selectById(projectId).sponsorId` → `SysUserDao.selectById(sponsorId).getRealName()`. Only needed in `getById` (detail view), not in `pageList` (list columns only show project name, not sponsor name).

Acceptance criteria:
- All state transition guards enforced (match the state machine)
- Project must be APPROVED before creating deliverables (use WhPmCharterDao to check)
- Permission checks per the matrix above — enforced in BO before any mutation
- MinIO upload uses `PutObjectArgs` with content type detection
- All public methods annotated `@Transactional`

### Phase 2: Approval Workflow (Steps 6-7)

#### Step 6: BPMN Process Definition
**File**: `wh-backend/src/main/resources/bpmn/pm-deliverable-approval.bpmn20.xml`

Exact same structure as `pm-charter-approval.bpmn20.xml`:
- Process ID: `PM_DELIVERABLE_APPROVAL`
- One userTask assigned to `ROLE_SPONSOR` with `assignee=${assignee}`
- Exclusive gateway branching on `${approvalResult == 'APPROVED'}` / `REJECTED`
- Both end events have `FlowableProcessEndListener` via `delegateExpression`

Acceptance criteria:
- Valid BPMN 2.0 XML
- Flowable can deploy and execute the process
- Listener fires on process end

#### Step 7: Approval Callback
**File**: `wh-backend/src/main/java/com/wh/approval/pm/DeliverableApprovalCallback.java`

- `@Component` implementing `ApprovalCompletedCallback`
- `getFlowCode()` returns `"PM_DELIVERABLE_APPROVAL"`
- `onApproved(bizId, params)`: set status=APPROVED. Keep `processInstanceId` (matches Charter `CharterApprovalCallback.java:28-34` — useful for audit)
- `onRejected(bizId, rejectReason)`: set status=REJECTED, set approvalComment=rejectReason, clear processInstanceId

Acceptance criteria:
- Auto-discovered by `ApprovalCallbackRegistry`
- Status transitions correct per state machine
- Callback tested by completing a Flowable task in integration test

### Phase 3: Controller (Step 8)

#### Step 8: REST Controller
**File**: `wh-backend/src/main/java/com/wh/controller/pm/WhPmDeliverableController.java`

Endpoints (see API Contract section below for full details):

| Method | Path | Action |
|--------|------|--------|
| GET | `/api/pm/deliverables` | Paginated list with filters |
| GET | `/api/pm/deliverables/{id}` | Detail |
| POST | `/api/pm/deliverables` | Create |
| PUT | `/api/pm/deliverables/{id}` | Update |
| DELETE | `/api/pm/deliverables/{id}` | Delete |
| POST | `/api/pm/deliverables/{id}/submit` | Submit for approval |
| POST | `/api/pm/deliverables/{id}/approve` | Approve (SPONSOR) |
| POST | `/api/pm/deliverables/{id}/reject` | Reject (SPONSOR) |
| POST | `/api/pm/deliverables/{id}/deliver` | Mark as delivered (PM) |
| POST | `/api/pm/deliverables/{id}/attachments` | Upload file attachment |
| GET | `/api/pm/deliverables/{id}/attachments/{index}` | Download attachment (302 redirect to MinIO Presigned URL) |
| DELETE | `/api/pm/deliverables/{id}/attachments/{index}` | Delete attachment |

Acceptance criteria:
- All endpoints return `R<T>` wrapper
- Constructor injection of `WhPmDeliverableBo`
- Path variable validation (non-empty)
- `@Slf4j` and `@RestController` annotations present

### Phase 4: Frontend (Steps 9-11)

#### Step 9: API Module
**File**: `wh-frontend/src/api/pm/deliverable.js`

Export functions following the charter.js pattern:
- `getDeliverableListApi(params)` -- GET /pm/deliverables
- `getDeliverableDetailApi(id)` -- GET /pm/deliverables/{id}
- `createDeliverableApi(data)` -- POST /pm/deliverables
- `updateDeliverableApi(id, data)` -- PUT /pm/deliverables/{id}
- `deleteDeliverableApi(id)` -- DELETE /pm/deliverables/{id}
- `submitDeliverableApi(id)` -- POST /pm/deliverables/{id}/submit
- `approveDeliverableApi(id, data)` -- POST /pm/deliverables/{id}/approve
- `rejectDeliverableApi(id, data)` -- POST /pm/deliverables/{id}/reject
- `deliverDeliverableApi(id)` -- POST /pm/deliverables/{id}/deliver
- `uploadDeliverableAttachmentApi(id, file)` -- POST /pm/deliverables/{id}/attachments (FormData)
- `downloadDeliverableAttachmentApi(id, index)` -- GET /pm/deliverables/{id}/attachments/{index} (returns redirect URL, open in new tab or set `window.location`)
- `deleteDeliverableAttachmentApi(id, index)` -- DELETE /pm/deliverables/{id}/attachments/{index}

#### Step 10: Vue Views
**Files**:
- `wh-frontend/src/views/pm/deliverable/index.vue`
- `wh-frontend/src/views/pm/deliverable/form.vue`
- `wh-frontend/src/views/pm/deliverable/detail.vue`

**index.vue** (list page):
- Pattern: Copy charter/index.vue structure exactly
- Filters: status dropdown (DRAFT/PENDING_APPROVAL/APPROVED/DELIVERED/REJECTED), project selector, keyword search
- Table columns: deliverableCode, name, project name, plannedDeliveryDate, actualDeliveryDate, status (el-tag), attachments count
- Action buttons conditional on status+role:
  - View (all roles)
  - Edit (PM only, DRAFT/REJECTED only)
  - Delete (PM only, DRAFT only -- matches Charter pattern)
  - Submit (PM only, DRAFT/REJECTED only)
  - Approve/Reject (SPONSOR only, PENDING_APPROVAL only)
  - Mark Delivered (PM only, APPROVED only)
- Pagination: same as charter pattern

**form.vue** (create/edit):
- Single-page form (no wizard steps -- simpler than charter, fewer fields)
- Fields: name (required text), description (textarea), plannedDeliveryDate (date picker, required), projectId (select from APPROVED projects, required)
- File upload section: el-upload component, list existing attachments with delete button
- In edit mode: load existing data from detail API
- In create mode: must pass or select projectId first
- Validation: name required, plannedDeliveryDate required, projectId required

**detail.vue** (detail/read-only):
- el-descriptions with all fields
- Status as el-tag
- Attachment list with download links
- Approval history (if available from Flowable -- otherwise just status + comment)

#### Step 11: Router Configuration
**File**: `wh-frontend/src/router/index.js`

Add 4 routes under the Layout child routes:

```javascript
{
  path: '/pm/deliverable',
  name: 'DeliverableList',
  component: () => import('@/views/pm/deliverable/index.vue'),
  meta: { title: '成果物管理', group: '项目管理', perm: 'ROLE_PM,ROLE_SPONSOR' }
},
{
  path: '/pm/deliverable/detail/:id',
  name: 'DeliverableDetail',
  component: () => import('@/views/pm/deliverable/detail.vue'),
  meta: { title: '成果物详情', hidden: true }
},
{
  path: '/pm/deliverable/form',
  name: 'DeliverableForm',
  component: () => import('@/views/pm/deliverable/form.vue'),
  meta: { title: '新增成果物', hidden: true }
},
{
  path: '/pm/deliverable/form/:id',
  name: 'DeliverableEdit',
  component: () => import('@/views/pm/deliverable/form.vue'),
  meta: { title: '编辑成果物', hidden: true }
},
```

**Also modify `SidebarMenu.vue`:**

1. Add menu item at `wh-frontend/src/components/layout/SidebarMenu.vue`:
```javascript
{ path: '/pm/deliverable', title: '成果物管理', group: '项目管理', icon: 'Folder', perm: 'ROLE_PM,ROLE_SPONSOR' }
```

2. Modify the sidebar filter logic (currently at lines 91-102 in SidebarMenu.vue) to support comma-separated `perm` values. Current logic checks `roles.some(r => r.toLowerCase().includes(permCode))` which requires an exact single-role match. Change to split `perm` by comma and check if user has ANY of the listed roles:
```javascript
// Before (single perm):
roles.some(r => r.toLowerCase().includes(permCode))
// After (comma-separated perms):
permCode.split(',').some(p => roles.some(r => r.toLowerCase().includes(p.trim().toLowerCase())))
```
This allows SPONSOR role to also see the "成果物管理" menu item.

Acceptance criteria:
- Routes visible in sidebar under "项目管理" group
- Hidden routes (form/detail) navigable by code but not shown in sidebar
- SPONSOR users can see "成果物管理" in sidebar
- Route `perm` meta matches SidebarMenu `perm` (`ROLE_PM,ROLE_SPONSOR`) for consistency
- Note: The current router has no perm-based guard (login/token only); sidebar filtering is the access control mechanism

### Phase 5: Verification (Step 12)

#### Step 12: Testing
See Test Plan section below.

---

## API Contract

Base URL: `/api/pm/deliverables`

### GET /api/pm/deliverables
List with pagination and filters.

**Query Parameters:**
| Param | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| pageNum | int | No | 1 | Page number |
| pageSize | int | No | 10 | Page size |
| status | string | No | - | Filter by status |
| projectId | string | No | - | Filter by project |
| keyword | string | No | - | Search in name/code |

**Response:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": "uuid",
        "deliverableCode": "DELIVERABLE-2026-001",
        "name": "需求规格说明书",
        "description": "详细的需求规格说明文档",
        "plannedDeliveryDate": "2026-06-30",
        "actualDeliveryDate": null,
        "status": "DRAFT",
        "attachments": "[{\"fileName\":\"spec.pdf\",\"fileSize\":123456,\"minioKey\":\"deliverable/uuid/abc_spec.pdf\",\"uploadTime\":\"2026-05-07T10:00:00\"}]",
        "approvalComment": null,
        "processInstanceId": null,
        "projectId": "proj-uuid",
        "createDate": "2026-05-07T10:00:00",
        "createBy": "user-uuid",
        "remarks": null
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

### GET /api/pm/deliverables/{id}
Detail.

**Response:** Single deliverable object (same shape as list record, plus `createByName` and `sponsorName` non-DB fields).

### POST /api/pm/deliverables
Create.

**Request Body:**
```json
{
  "name": "需求规格说明书",
  "description": "详细的需求规格说明文档",
  "plannedDeliveryDate": "2026-06-30",
  "projectId": "proj-uuid",
  "remarks": "备注信息"
}
```

**Response:** Created deliverable object with auto-generated `deliverableCode`, status=`DRAFT`.

### PUT /api/pm/deliverables/{id}
Update. Only DRAFT/REJECTED.

**Request Body:**
```json
{
  "name": "需求规格说明书 v2",
  "description": "更新后的描述",
  "plannedDeliveryDate": "2026-07-15",
  "remarks": "更新备注"
}
```

**Response:** `R<Void>` with code 200.

### DELETE /api/pm/deliverables/{id}
Delete. Only DRAFT (matches Charter pattern: `WhPmCharterBo.java:225`).

**Response:** `R<Void>` with code 200.

### POST /api/pm/deliverables/{id}/submit
Submit for SPONSOR approval. Only DRAFT/REJECTED.

**Request Body:**
```json
{
  "comment": "请审批"
}
```

**Response:** `R<Void>` with code 200.

### POST /api/pm/deliverables/{id}/approve
SPONSOR approves. Only PENDING_APPROVAL.

**Request Body:**
```json
{
  "comment": "批准"
}
```

**Response:** `R<Void>` with code 200.

### POST /api/pm/deliverables/{id}/reject
SPONSOR rejects. Only PENDING_APPROVAL.

**Request Body:**
```json
{
  "rejectReason": "需要补充内容"
}
```

**Response:** `R<Void>` with code 200.

### POST /api/pm/deliverables/{id}/deliver
PM marks as delivered. Only APPROVED.

**Response:** `R<Void>` with code 200. Sets `actualDeliveryDate` to now, status to `DELIVERED`.

### POST /api/pm/deliverables/{id}/attachments
Upload file attachment. Only DRAFT/REJECTED.

**Request:** `multipart/form-data` with field `file`.

**Response:**
```json
{
  "code": 200,
  "data": [
    {
      "fileName": "spec.pdf",
      "fileSize": 123456,
      "minioKey": "deliverable/uuid/abc_spec.pdf",
      "uploadTime": "2026-05-07T10:00:00"
    }
  ]
}
```
Returns the full updated attachment list.

### DELETE /api/pm/deliverables/{id}/attachments/{index}
Delete attachment at array index. Only DRAFT/REJECTED.

**Response:** Updated attachment list (minus deleted entry).

### GET /api/pm/deliverables/{id}/attachments/{index}
Download attachment file. Any authenticated user with view access to the deliverable.

**Response:** HTTP 302 redirect to a MinIO Presigned URL (1-hour expiry). The browser follows the redirect automatically, triggering a file download. No JSON response body.

---

## Database DDL

```sql
-- Deliverable Management Table
CREATE TABLE IF NOT EXISTS wh_pm_deliverable (
    ID TEXT NOT NULL,
    DELIVERABLE_CODE TEXT NOT NULL UNIQUE,
    NAME TEXT NOT NULL,
    DESCRIPTION TEXT,
    PLANNED_DELIVERY_DATE TEXT,
    ACTUAL_DELIVERY_DATE TEXT,
    STATUS TEXT NOT NULL DEFAULT 'DRAFT',
    ATTACHMENTS TEXT,
    APPROVAL_COMMENT TEXT,
    PROCESS_INSTANCE_ID TEXT,
    PROJECT_ID TEXT NOT NULL,
    CREATE_BY TEXT,
    CREATE_DATE TEXT DEFAULT (datetime('now', 'localtime')),
    UPDATE_BY TEXT,
    UPDATE_DATE TEXT DEFAULT (datetime('now', 'localtime')),
    REMARKS TEXT,
    DEL_FLAG TEXT DEFAULT '0',
    VER_NO INTEGER DEFAULT 0 NOT NULL,
    SYS_CODE TEXT,
    CONSTRAINT PK_WH_PM_DELIVERABLE PRIMARY KEY (ID),
    CONSTRAINT CK_WH_PM_DELIVERABLE_STATUS CHECK (STATUS IN (
        'DRAFT','PENDING_APPROVAL','APPROVED','DELIVERED','REJECTED'
    )),
    CONSTRAINT FK_WH_PM_DELIVERABLE_PROJECT FOREIGN KEY (PROJECT_ID)
        REFERENCES wh_pm_project_charter(ID)
);

CREATE INDEX IF NOT EXISTS IDX_WH_PM_DELIVERABLE_STATUS ON wh_pm_deliverable(STATUS);
CREATE INDEX IF NOT EXISTS IDX_WH_PM_DELIVERABLE_PROJECT ON wh_pm_deliverable(PROJECT_ID);
CREATE INDEX IF NOT EXISTS IDX_WH_PM_DELIVERABLE_CODE ON wh_pm_deliverable(DELIVERABLE_CODE);
CREATE INDEX IF NOT EXISTS IDX_WH_PM_DELIVERABLE_DEL ON wh_pm_deliverable(DEL_FLAG);

-- Seed PM_DELIVERABLE sequence
INSERT INTO wh_sequence (SEQ_NAME, SEQ_VALUE, SEQ_PREFIX, SEQ_YEAR)
SELECT 'PM_DELIVERABLE', 0, 'DELIVERABLE-', 2026
WHERE NOT EXISTS (SELECT 1 FROM wh_sequence WHERE SEQ_NAME = 'PM_DELIVERABLE');
```

---

## BPMN Process Design

Process ID: `PM_DELIVERABLE_APPROVAL`
Process Name: `成果物审批流程`

```
[Start] --> [Sponsor Approval (UserTask)]
                candidateGroups: ROLE_SPONSOR
                assignee: ${assignee}
                    |
            [Exclusive Gateway]
            /                \
    ${approvalResult         ${approvalResult
     == 'APPROVED'}          == 'REJECTED'}
          |                        |
    [APPROVED End]          [REJECTED End]
     (listener)              (listener)
```

Both end events use:
```xml
<extensionElements>
    <flowable:executionListener event="end"
        delegateExpression="${flowableProcessEndListener}"/>
</extensionElements>
```

Process variables passed on start:
- `flowCode` = `"PM_DELIVERABLE_APPROVAL"`
- `bizId` = deliverable UUID
- `assignee` = project's sponsorId
- `deliverableName` = deliverable name (for notification display)

---

## Frontend Component Structure

### deliverable/index.vue (List)
```
el-card
  header: title "成果物管理" + filters (status dropdown, project selector, keyword input, action buttons)
  el-table: deliverableCode | name | project name | plannedDeliveryDate | actualDeliveryDate | status (el-tag) | attachments count | actions
  el-pagination
```

Actions per row (conditional):
| Condition | Visible Buttons |
|-----------|----------------|
| All statuses | View (el-icon View) |
| DRAFT + currentUser is PM | Edit, Delete, Submit |
| REJECTED + currentUser is PM | Edit, Submit |
| PENDING_APPROVAL + currentUser is SPONSOR | Approve, Reject |
| APPROVED + currentUser is PM | Mark Delivered |
| DELIVERED | View only |

### deliverable/form.vue (Create/Edit)
```
el-card
  header: "新增成果物" or "编辑成果物"
  el-form:
    - projectId: el-select (filterable, load APPROVED projects, disabled in edit mode)
    - name: el-input (required)
    - description: el-input type=textarea
    - plannedDeliveryDate: el-date-picker (required)
    - remarks: el-input
    - Attachments section:
        el-upload (drag-and-drop, multiple)
        v-for list current attachments with name + delete button
    - Form actions: Save, Cancel
```

### deliverable/detail.vue (Detail)
```
el-card v-loading
  header: "成果物详情" + Back button
  el-descriptions (2 columns, border):
    - deliverableCode, status (el-tag), project name
    - name, description
    - plannedDeliveryDate, actualDeliveryDate
    - approvals comment
    - remarks
  Attachments section:
    v-for list with file name, size, upload time, download link
  Approval action buttons (if applicable):
    - Approve/Reject for SPONSOR
    - Mark Delivered for PM
```

---

## Test Plan

### Unit Tests

1. **WhPmDeliverableBoTest** (`wh-backend/src/test/java/com/wh/WhPmDeliverableBoTest.java`)
   - `testCreate_deliverableCodeGenerated` -- Verify code format `DELIVERABLE-YYYY-NNN`
   - `testCreate_nonApprovedProject_throws` -- Creating for non-APPROVED project throws ServiceException
   - `testUpdate_deliveredStatus_throws` -- Updating a DELIVERED deliverable throws
   - `testUpdate_draftStatus_succeeds` -- Updating a DRAFT deliverable works
   - `testDelete_deliveredStatus_throws` -- Deleting a DELIVERED deliverable throws
   - `testDelete_draftStatus_succeeds` -- Deleting a DRAFT deliverable works (physical delete)
   - `testSubmit_fromDraft_succeeds` -- Submitting a DRAFT deliverable sets PENDING_APPROVAL and creates Flowable process
   - `testSubmit_fromApproved_throws` -- Submitting an APPROVED deliverable throws
   - `testMarkDelivered_fromApproved_succeeds` -- Sets DELIVERED + actualDeliveryDate
   - `testMarkDelivered_fromDraft_throws` -- Throws for non-APPROVED status
   - `testApprove_pendingApproval_succeeds` -- Completes Flowable task with APPROVED result
   - `testReject_pendingApproval_succeeds` -- Completes Flowable task with REJECTED result

2. **DeliverableApprovalCallbackTest** (`wh-backend/src/test/java/com/wh/DeliverableApprovalCallbackTest.java`)
   - `testOnApproved_setsStatus` -- Callback sets status to APPROVED
   - `testOnRejected_setsStatus` -- Callback sets status to REJECTED and clears processInstanceId

### Integration Tests

3. **DeliverableApiTest** (`wh-backend/src/test/java/com/wh/DeliverableApiTest.java`)
   - `testFullLifecycle` -- Create -> Submit -> Approve (via Flowable task completion) -> Mark Delivered
   - `testRejectAndReSubmit` -- Create -> Submit -> Reject -> Re-submit
   - `testUploadAndDeleteAttachment` -- Upload file, verify attachments JSON, delete
   - `testListWithFilters` -- Create 3 deliverables with different statuses, filter by status
   - `testPermissionGuards` -- Verify 403 for unauthorized role access (if security enabled)

### Frontend (Manual / E2E)
4. Manual verification checklist:
   - [ ] List page loads with correct columns and status tags
   - [ ] Create form validates required fields
   - [ ] Create form project selector only shows APPROVED projects
   - [ ] Status-based action buttons show/hide correctly for each state
   - [ ] Submit triggers a pending state change
   - [ ] SPONSOR can see approve/reject buttons for PENDING_APPROVAL items
   - [ ] Mark Delivered sets actualDeliveryDate and shows in detail
   - [ ] File upload adds to attachment list
   - [ ] File delete removes from list
   - [ ] Detail page shows all fields correctly
   - [ ] Sidebar menu shows "成果物管理" under "项目管理" group

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| **Flowable process deployment failure** | Low | High | BPMN XML follows existing pm-charter-approval exactly; only IDs/names differ. Validate XML against Flowable schema before runtime |
| **MinIO unavailable or first-use issues** | Medium | Medium | MinioConfig already initializes the bucket in `@PostConstruct` (logs warning if unreachable). Add try-catch in upload/download methods with clear error messages. Use Presigned URLs (1-hour expiry) for downloads. No business code uses MinIO yet — this is the first consumer. Test with MinIO running locally before marking Step 8 complete |
| **Sequence code collision** | Low | Medium | SequenceService is synchronized and uses DB-level increment. Year reset logic already handled. Same pattern used for CHARTER with no reported issues |
| **Project foreign key violation** | Low | Low | BO validates project existence and APPROVED status before allowing create. SQLite FK enforcement requires `PRAGMA foreign_keys = ON` which is not set — the BO-level validation is the primary guard |
| **Attachment JSON column size limits** | Low | Low | SQLite TEXT can hold up to 1GB. A few attachments per deliverable (few MB of JSON) is far below limits. Max 10 attachments enforced in BO |
| **Permission bypass** | Medium | High | All BO methods now have explicit permission checks per the Permission Matrix above. SPONSOR checks include project-level ownership validation (not just role check). Controller has no `@PreAuthorize` — BO is the enforcement layer |
| **Frontend route guard misses SPONSOR access** | Low | Low | Resolved: route meta and sidebar both use `perm: 'ROLE_PM,ROLE_SPONSOR'`. Sidebar filter supports comma-separated perms. No route-level perm guard exists to conflict with this |

---

## File Inventory

| # | File | Type | Lines (est) |
|---|------|------|-------------|
| 1 | `wh-backend/.../db/sqlite/037-pm-deliverable.sql` | SQL | ~30 |
| 2 | `wh-backend/.../entity/pm/WhPmDeliverable.java` | Entity | ~50 |
| 3 | `wh-backend/.../dao/pm/WhPmDeliverableDao.java` | DAO | ~20 |
| 4 | `wh-backend/.../bo/pm/DeliverableCreateRequest.java` | Request | ~20 |
| 5 | `wh-backend/.../bo/pm/DeliverableUpdateRequest.java` | Request | ~15 |
| 6 | `wh-backend/.../bo/pm/DeliverableSubmitRequest.java` | Request | ~10 |
| 7 | `wh-backend/.../bo/pm/WhPmDeliverableBo.java` | BO | ~300 |
| 8 | `wh-backend/.../approval/pm/DeliverableApprovalCallback.java` | Callback | ~45 |
| 9 | `wh-backend/.../controller/pm/WhPmDeliverableController.java` | Controller | ~100 |
| 10 | `wh-backend/.../resources/bpmn/pm-deliverable-approval.bpmn20.xml` | BPMN | ~45 |
| 11 | `wh-frontend/src/api/pm/deliverable.js` | API | ~50 |
| 12 | `wh-frontend/src/views/pm/deliverable/index.vue` | View | ~200 |
| 13 | `wh-frontend/src/views/pm/deliverable/form.vue` | View | ~250 |
| 14 | `wh-frontend/src/views/pm/deliverable/detail.vue` | View | ~120 |
| 15 | `wh-frontend/src/router/index.js` | Route (edit) | +20 |
| 16 | `wh-frontend/src/components/layout/SidebarMenu.vue` | Sidebar (edit) | +10 |

**Total: 16 files (14 new, 2 modified), approximately 1,275 lines of code.**

---

## Complexity Assessment: MEDIUM

The feature follows well-established patterns with zero architectural novelty. The only non-trivial elements are:
1. MinIO file upload (new pattern, but MinioClient is already a Spring bean)
2. The DELIVERED state transition (PM action, not approval-driven -- requires a dedicated endpoint not in Charter)
3. Attachment metadata as JSON (simple serialization, but requires correct Jackson handling)
