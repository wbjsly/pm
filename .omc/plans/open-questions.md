## Open Questions

### deliverable-management - 2026-05-07

- [ ] **List page scope: global or project-scoped?** -- Should the deliverable list page (`/pm/deliverable`) show all deliverables across all projects, or should it be a sub-page nested under a specific project (e.g., `/pm/charter/detail/:id/deliverables`)? The current plan assumes a global list with a projectId filter dropdown. If project-scoped makes more UX sense, the route structure and the create form's project selector need adjustment.

- [ ] **Multi-role route access** -- The router `meta.perm` currently supports a single role value. SPONSOR users need view access to deliverables, but SPONSOR is not ROLE_PM. Options: (a) set `perm: ['ROLE_PM', 'ROLE_SPONSOR']` and update the route guard to check arrays, (b) remove `perm` from the route and enforce permissions only in the BO layer, (c) add a separate SPONSOR-accessible route. Which approach is preferred?

- [ ] **Attachment download URL** -- MinIO object keys stored in the JSON array need to be converted to downloadable URLs. Should the backend provide a dedicated download endpoint that proxies through MinIO (generating a pre-signed URL), or should the frontend construct MinIO URLs directly? The former is more secure; the latter requires exposing MinIO endpoint to the frontend.

- [ ] **DELIVERED status: allow edit of actualDeliveryDate?** -- The spec says DELIVERED is immutable, but the PM might want to backdate the actual delivery date. Should `markDelivered` accept an optional date parameter, or always use `now()`?

- [ ] **User name filling for SPONSOR** -- The BO fills `pmName` and `sponsorName` on the deliverable by joining through the project (via projectId -> project -> sponsorId). The existing `fillUserNames` pattern in Charter BO batches user lookups per entity. Should the deliverable BO do a project join to get the sponsor, or should it store sponsorId directly on the deliverable at create time?
