## Open Questions

### deliverable-management - 2026-05-07

- [ ] **List page scope: global or project-scoped?** -- Should the deliverable list page (`/pm/deliverable`) show all deliverables across all projects, or should it be a sub-page nested under a specific project (e.g., `/pm/charter/detail/:id/deliverables`)? The current plan assumes a global list with a projectId filter dropdown. If project-scoped makes more UX sense, the route structure and the create form's project selector need adjustment.

- [ ] **Multi-role route access** -- The router `meta.perm` currently supports a single role value. SPONSOR users need view access to deliverables, but SPONSOR is not ROLE_PM. Options: (a) set `perm: ['ROLE_PM', 'ROLE_SPONSOR']` and update the route guard to check arrays, (b) remove `perm` from the route and enforce permissions only in the BO layer, (c) add a separate SPONSOR-accessible route. Which approach is preferred?

- [ ] **Attachment download URL** -- MinIO object keys stored in the JSON array need to be converted to downloadable URLs. Should the backend provide a dedicated download endpoint that proxies through MinIO (generating a pre-signed URL), or should the frontend construct MinIO URLs directly? The former is more secure; the latter requires exposing MinIO endpoint to the frontend.

- [ ] **DELIVERED status: allow edit of actualDeliveryDate?** -- The spec says DELIVERED is immutable, but the PM might want to backdate the actual delivery date. Should `markDelivered` accept an optional date parameter, or always use `now()`?

- [ ] **User name filling for SPONSOR** -- The BO fills `pmName` and `sponsorName` on the deliverable by joining through the project (via projectId -> project -> sponsorId). The existing `fillUserNames` pattern in Charter BO batches user lookups per entity. Should the deliverable BO do a project join to get the sponsor, or should it store sponsorId directly on the deliverable at create time?

### plans-enum-opt - 2026-05-12

- [ ] **WBS ELEMENT_TYPE 隐含值** -- 当前代码中 `elementType` 只有 `TASK`；实际业务是否需要 `MILESTONE`、`PHASE`、`DELIVERABLE` 等值？需确认种子数据中是否预置这些条目。

- [ ] **CHARTER_STATUS 的 CLOSED 状态** -- 前端多处定义了 `CLOSED` 状态（`charter/index.vue`, `charter/detail.vue`, `wbs/index.vue`），但后端 Bo 中未找到任何 `setStatus("CLOSED")` 的代码。需确认是否需要补全后端关闭项目逻辑，或从前端映射中移除。

- [ ] **BUDGET_STATUS 命名不一致** -- 预算状态使用 `PENDING`（而非 `PENDING_APPROVAL`），与 Charter/Deliverable 的命名风格不一致。是否在字典化过程中统一为 `PENDING_APPROVAL`（需要同步修改数据库已有数据和前端判断逻辑），还是保持历史命名？

- [ ] **字典表与 Flowable 流程变量的关系** -- ApprovalCallback 中 `variables.put("approvalResult", "APPROVED")` 使用的 `APPROVED` 是否纳入字典管理（类型可为 `APPROVAL_RESULT`）？当前设计未覆盖流程变量。
