# Plan: Database-Driven Menu Management

**Created:** 2026-05-13
**Branch:** dev
**Status:** Revised (Architect + Critic review applied)

---

## RALPLAN-DR Summary

### Principles (5)

1. **Minimal Disruption**: Sidebar search, collapse, highlight, and grouping behavior remain identical. Only the data source changes from hardcoded `menuItems[]` to API.
2. **Exact Permission Match**: Admin users see all menus; non-admin users match via `perm` field comma-separated role codes. No `toLowerCase()` or `replace()` hack -- exact `ROLE_*` comparison. Client-side filter retained as defense-in-depth (light assertion).
3. **Backend Convention Alignment**: Follow existing BO pattern (Controller injects `@Service` BO, BO injects DAO), `BaseEntity` with UUID PK, `R<T>` response wrapper, SQLite migration via numbered `.sql` files.
4. **Frontend Convention Alignment**: Follow existing Vue 3 + Element Plus + Pinia patterns, API module structure (`src/api/system/menu.js`), view directory convention (`src/views/system/menu/`).
5. **Route Stability**: All existing route definitions stay in `router/index.js` for component resolution. The sidebar only controls **visibility** -- it does not `addRoute()` or `removeRoute()`.

### Decision Drivers (Top 3)

| # | Driver | Why it matters |
|---|--------|---------------|
| 1 | Sidebar rendering must be identical to current hardcoded state | 10 menu items (9 existing + menu management) must appear with same grouping, icons, and sorting as before migration |
| 2 | Permission filtering must match current logic (admin=all, others=perm-match) | Different roles must see different menus, matching existing behavior |
| 3 | Menu management page must be admin-only (backend AND frontend enforced) | Menu CRUD is a system configuration concern, not a PM concern. SecurityConfig rules + route meta.perm double-gate |

### Options Considered (>=2 required)

**Option A: Sidebar-only dynamic (SELECTED)**
- Routes stay fully defined in `router/index.js` (visible + hidden)
- Sidebar fetches menu list from API, renders only DB entries that match user permissions
- Pros: Zero risk of route registration timing issues; existing navigation works without changes; simplest implementation
- Cons: Adding a new visible route still requires a manual entry in `router/index.js` (acceptable -- this is a system admin task)

**Option B: Full dynamic routing with `router.addRoute()`**
- Visible menu routes NOT defined in `router/index.js`; added at runtime via `router.addRoute()` after fetching from DB
- Pros: Truly dynamic -- adding a menu in admin page automatically registers the route
- Cons: Chicken-and-egg problem (must load menus before navigation completes); component resolution must be configurable in DB; adds complexity for a system-admin-only workflow
- **Invalidated**: The spec says "hidden routes (detail/form pages) stay hardcoded in router/index.js" -- keeping visible routes there too follows the same principle of route stability. Dynamic `addRoute` is over-engineered for an admin-managed menu system.

**Option C: API-driven with static fallback**
- Sidebar fetches from API; if API fails, renders from a hardcoded fallback config
- Pros: Resilient to backend failure
- Cons: Dual source of truth (DB + fallback config) creates maintenance burden; if backend is down, nothing else works anyway
- **Invalidated**: The sidebar is only rendered when the user is authenticated and the app is loaded. If the backend is down, login already failed. A fallback adds complexity without real benefit.

### ADR

- **Decision**: Sidebar fetches menu items from `GET /api/system/menus/user` and renders them. All routes remain statically defined in `router/index.js`. Sidebar only controls visibility. Menu loading triggers after `userStore.getUserInfo()` succeeds (not in beforeEach). Client-side perm filter retained as defense-in-depth.
- **Drivers**: Route stability, minimal disruption, simplest implementation matching existing patterns, correct loading order (user identity must be known before fetching menus).
- **Alternatives considered**: Full dynamic routing (complex, risk of timing issues), API+fallback (dual source of truth), beforeEach loading (too early -- user roles not yet available).
- **Why chosen**: Option A respects the existing route architecture -- hidden routes already co-exist in the same router config as visible routes. The sidebar is purely a UI concern. Loading after getUserInfo() guarantees roles are available for server-side filtering.
- **Consequences**: New visible menu items require BOTH a DB record AND a router entry. The menu management page documents this constraint via an el-alert. Menu management page itself is bootstrapped via MENU010 seed data so admin can reach it after migration.
- **Follow-ups**: If menu count grows significantly, consider a configurable `component` field in `sys_menu` to enable true dynamic routing later.

---

## Context

Currently, sidebar menus are hardcoded in `SidebarMenu.vue:81-91` as a static array of 9 objects with `path`, `title`, `group`, `icon`, `perm` fields. Permission filtering is done in-browser via `userStore.userInfo.roles`. This makes menu changes require code deploys.

The goal is to replace this with a database-driven system: `sys_menu` table, backend CRUD API, admin management page (bootstrapped as 10th seed item), and a refactored SidebarMenu that fetches menus from the API.

## Work Objectives

1. Create `sys_menu` table via SQLite migration with seed data for all 10 menu items (9 existing + menu management)
2. Build backend CRUD stack (entity, DAO, BO, controller) following existing patterns
3. Add explicit SecurityConfig authorization rules for `/api/system/menus/**`
4. Build frontend menu management page under system management (admin-only, with cache invalidation)
5. Refactor SidebarMenu.vue to fetch from API instead of hardcoded array
6. Add menu management route, menu store init hook, and E2E tests

## Guardrails

**Must Have:**
- Sidebar rendering identical to current (grouping, icons, sorting, search, collapse, active-highlight)
- Admin users (`ROLE_ADMIN`) see all enabled menus
- Non-admin users see only menus whose `perm` field contains at least one of their role codes (exact match)
- Menu management page only accessible to `ROLE_ADMIN`
- Menu store loads after `userStore.getUserInfo()` completes (role data available)
- Menu store cleared/refreshed after management page CRUD operations
- 10 seed items (9 existing + MENU010 for menu management itself) producing identical rendering

**Must NOT:**
- Remove or modify existing route definitions in `router/index.js` (keep for component resolution)
- Touch `sys_permission` table
- Change the existing permission model (roles-based, no permission codes)
- Break Dashboard (hardcoded `/dashboard` menu item in SidebarMenu template)

---

## Task Flow

### Step 1: SQL Migration `048-sys-menu.sql`

**File to create:** `wh-backend/src/main/resources/db/sqlite/048-sys-menu.sql`

- Create `sys_menu` table with fields: ID, PARENT_ID, TITLE, PATH, COMPONENT, ICON, GROUP_NAME, SORT_ORDER, PERM, STATUS, MENU_TYPE, plus BaseEntity audit columns (CREATE_BY, CREATE_DATE, UPDATE_BY, UPDATE_DATE, REMARKS, DEL_FLAG, VER_NO, SYS_CODE)
- Insert **10** seed data rows (9 existing + MENU010 for menu management itself)
- `GROUP_NAME` determines sidebar grouping; `SORT_ORDER` controls group & item ordering
- `PERM` stores comma-separated role codes (e.g., `ROLE_PM,ROLE_SPONSOR`)
- `STATUS` = '1' (enabled) or '0' (disabled)
- MENU010 bootstraps the menu management page in the sidebar so admin can reach it

**Seed Data (10 items):**

| ID | TITLE | PATH | GROUP_NAME | ICON | SORT | PERM |
|----|-------|------|-----------|------|------|------|
| MENU001 | 项目立项 | /pm/charter | 项目管理 | Document | 1 | ROLE_PM |
| MENU002 | 项目任务 | /pm/wbs | 项目管理 | List | 2 | ROLE_PM |
| MENU003 | 预算管理 | /pm/budget | 项目管理 | Money | 3 | ROLE_PM |
| MENU004 | 工时管理 | /pm/work-hours | 项目管理 | Clock | 4 | ROLE_PM |
| MENU005 | 成果管理 | /pm/deliverable | 项目管理 | Folder | 5 | ROLE_PM,ROLE_SPONSOR |
| MENU006 | 产品清单 | /pm/product | 系统管理 | Tickets | 1 | ROLE_PM |
| MENU007 | 用户管理 | /system/user | 系统管理 | User | 2 | ROLE_ADMIN |
| MENU008 | 工作日历 | /system/calendar | 系统管理 | Calendar | 3 | ROLE_ADMIN |
| MENU009 | 成本定额 | /system/cost-quota | 系统管理 | Money | 4 | ROLE_ADMIN,ROLE_PM |
| MENU010 | 菜单管理 | /system/menu | 系统管理 | Menu | 5 | ROLE_ADMIN |

**Acceptance Criteria:**
- Table created with correct columns and constraints (PK, NOT NULL on TITLE/PATH/GROUP_NAME)
- 10 seed rows inserted with same title, path, group_name, icon as specified
- Migration file named `048-sys-menu.sql` (fills gap between 047 and 099)

**Recovery:**
- If migration fails (bad SQL), SqliteBootstrap logs the error and skips the file. Fix the SQL and restart the application. No data loss since this is a new table.
- To re-run: delete the SQLite database file (`data/wh.sqlite`) and restart (all migrations re-execute).
- If seed IDs conflict with future inserts: seed IDs use `MENU0XX` prefix; new items get UUIDs from MyBatis-Plus. No conflict possible.

---

### Step 2: Backend CRUD Stack

**Files to create:**

| File | Package/Path | Role |
|------|-------------|------|
| `SysMenu.java` | `com.wh.entity.system` | Entity extending BaseEntity, @TableName("sys_menu") |
| `SysMenuDao.java` | `com.wh.dao.system` | MyBatis-Plus BaseMapper<SysMenu> |
| `WhPmMenuBo.java` | `com.wh.bo.system` | @Service, business logic + DAO calls |
| `SysMenuController.java` | `com.wh.controller.system` | REST controller, returns R<T> |

**API Design:**

| Method | Path | Auth (SecurityConfig) | Description |
|--------|------|----------------------|-------------|
| GET | `/api/system/menus/user` | `authenticated()` | Returns menus visible to current user (admin=all, others=perm-filtered). Sorted by GROUP_NAME, then SORT_ORDER. |
| GET | `/api/system/menus` | `hasRole("ADMIN")` | List all menus (for management page) |
| GET | `/api/system/menus/{id}` | `hasRole("ADMIN")` | Get single menu |
| POST | `/api/system/menus` | `hasRole("ADMIN")` | Create menu |
| PUT | `/api/system/menus` | `hasRole("ADMIN")` | Update menu |
| DELETE | `/api/system/menus/{id}` | `hasRole("ADMIN")` | Delete menu (logic delete via delFlag). Returns `R.ok()` without data body (consistent with SysDictController). |

**`GET /api/system/menus/user` Response Shape:**
```json
{
  "code": 200,
  "data": [
    {
      "id": "MENU001",
      "title": "项目立项",
      "path": "/pm/charter",
      "groupName": "项目管理",
      "icon": "Document",
      "sortOrder": 1,
      "perm": "ROLE_PM"
    }
  ]
}
```

**BO Logic (`WhPmMenuBo`):**
- `getUserMenus(List<String> roles)`: If `roles` contains `ROLE_ADMIN`, return all with STATUS='1' and delFlag='0'. Otherwise, return menus where `perm` is not null AND at least one of the user's roles appears in `perm` after splitting by comma and trimming. Sorted by GROUP_NAME, SORT_ORDER.
- `getAllMenus()`: List all (including disabled), for admin management page. Uses `selectList(null)`.
- Standard CRUD methods using `SysMenuDao` (insert/updateById/deleteById).

**Substep 2a: SecurityConfig Authorization Rules**

**File to modify:** `wh-backend/src/main/java/com/wh/security/SecurityConfig.java`

Insert the following rules BEFORE the catch-all `.antMatchers("/api/**").authenticated()` line, and AFTER the existing dict rules (follow the same pattern):

```java
// Menu: user-facing endpoint accessible to all authenticated users
.antMatchers(HttpMethod.GET, "/api/system/menus/user").authenticated()
// Menu management: admin-only
.antMatchers(HttpMethod.GET, "/api/system/menus/**").hasRole("ADMIN")
.antMatchers(HttpMethod.POST, "/api/system/menus/**").hasRole("ADMIN")
.antMatchers(HttpMethod.PUT, "/api/system/menus/**").hasRole("ADMIN")
.antMatchers(HttpMethod.DELETE, "/api/system/menus/**").hasRole("ADMIN")
```

Rule ordering is critical: `/api/system/menus/user` (more specific) must appear before `/api/system/menus/**` (less specific) so that the user endpoint is not caught by the admin-only rule.

**Acceptance Criteria:**
- Entity fields mapped correctly to column names (upper case with underscores per existing convention, @TableField annotations)
- `GET /api/system/menus/user` with ROLE_ADMIN returns all 10 enabled seed items
- `GET /api/system/menus/user` with ROLE_PM returns only PM-visible items (6-7 items, no admin-only menus)
- `GET /api/system/menus/user` with ROLE_SPONSOR returns only SPONSOR-visible items (1 item: 成果管理)
- `GET /api/system/menus` returns all 10 items for admin; returns 401/403 for non-admin
- `DELETE /api/system/menus/{id}` returns `R.ok()` with no data body (code=200, data=null)
- All CRUD endpoints return proper `R<T>` responses
- SecurityConfig rules tested: PM user cannot POST/PUT/DELETE to `/api/system/menus`

**Recovery:**
- If `@TableField` column name mismatch: check SQL DDL against entity field names. Column names in SQL are upper case with underscores (e.g., `GROUP_NAME`). Entity fields are camelCase with `@TableField("GROUP_NAME")`.
- If BO authorization fails: verify the Controller extracts roles from SecurityContextHolder or request attributes (pattern must match how existing controllers get roles).
- If SecurityConfig rule ordering is wrong: the most specific rule (`/api/system/menus/user`) must come before the wildcard (`/api/system/menus/**`). Spring Security evaluates rules in declaration order.

---

### Step 3: Frontend API Module + Pinia Store

**Files to create:**

| File | Path |
|------|------|
| Menu API | `wh-frontend/src/api/system/menu.js` |
| Menu Store | `wh-frontend/src/store/menu.js` |

**Menu API (`src/api/system/menu.js`):**
```js
import request from '@/utils/request'

export function getUserMenusApi() { return request.get('/system/menus/user') }
export function getMenuListApi() { return request.get('/system/menus') }
export function getMenuApi(id) { return request.get(`/system/menus/${id}`) }
export function createMenuApi(data) { return request.post('/system/menus', data) }
export function updateMenuApi(data) { return request.put('/system/menus', data) }
export function deleteMenuApi(id) { return request.delete(`/system/menus/${id}`) }
```

**Menu Store (`src/store/menu.js`):**
- Pinia store with state: `menuItems` (array), `loaded` (boolean), `loading` (boolean) -- follows the dict store's `loaded`/`loading` double-check pattern (`store/dict.js:37-41`)
- `fetchMenus()` action:
  - Guard: `if (this.loaded || this.loading) return`
  - Set `loading = true`
  - Call `getUserMenusApi()`, store result in `menuItems`
  - Set `loaded = true`
  - Catch errors silently (sidebar will be empty, not crash)
  - Finally: set `loading = false`
- `reset()` action: set `menuItems = []`, `loaded = false`, `loading = false` -- called by menu management page after CRUD to force re-fetch
- Used by SidebarMenu for reactive menu data

**Menu loading trigger (Step 6 covers the hook location):**
- NOT loaded in `router.beforeEach` -- too early, user roles may not be available yet
- Instead, loaded in `App.vue` (or Layout component) after `userStore.getUserInfo()` successfully resolves
- The user store's `getUserInfo()` is called in `beforeEach` guard (lines 221-228 of router/index.js). The menu store fetch should be triggered AFTER that resolves.

**Acceptance Criteria:**
- `fetchMenus()` returns menu items in correct order from API
- `loaded`/`loading` guards prevent concurrent/redundant API calls
- `reset()` clears state to force re-fetch on next access
- Store is reactive -- sidebar re-renders when `menuItems` changes

**Recovery:**
- If `fetchMenus()` fails silently: sidebar renders empty. User sees Dashboard only. Cause is likely backend down or auth token expired. Check browser console for network errors.
- If `loaded` remains false due to exception before `finally` block: the `loading` flag in `finally` ensures the store is not stuck. But if API returns a non-error empty response, `loaded` will be set to true with empty `menuItems`. This is expected behavior for users with no roles.

---

### Step 4: Menu Management Page (Admin Only)

**File to create:** `wh-frontend/src/views/system/menu/index.vue`

- **Top banner**: `<el-alert type="info" :closable="false">` with message: "注意：新建菜单项需要同时在 `router/index.js` 中定义对应的路由记录，否则侧边栏点击后将无法正常导航。"
- Single-page table with columns: Title, Path, Group, Icon, Sort Order, Permissions, Status, Actions
- Inline or dialog-based CRUD (follow existing `dict/index.vue` pattern with `el-dialog` forms)
- Form fields:
  - TITLE (required, text input)
  - PATH (required, text input)
  - GROUP_NAME (required, text input)
  - ICON (optional, text input with placeholder hint: "Element Plus 图标组件名，如 Document, User")
  - SORT_ORDER (number input)
  - PERM (text input, comma-separated role codes, placeholder: "ROLE_ADMIN,ROLE_PM")
  - STATUS (el-switch: 启用/禁用)
  - MENU_TYPE (hidden field, default 'MENU')
- Delete confirmation dialog (`ElMessageBox.confirm`)
- El-tag display for STATUS (启用=success, 禁用=info) and PERM (split by comma, each role gets a tag)
- Table sorted by GROUP_NAME, then SORT_ORDER

**Cache Invalidation After CRUD (CRITICAL for sidebar sync):**
- After successful create/update/delete, call `useMenuStore().reset()` to force sidebar to re-fetch on next access
- This ensures the sidebar reflects management page changes immediately

**Acceptance Criteria:**
- Page accessible at `/system/menu` route (admin only)
- el-alert warning about router entries visible at top of page
- All 10 seed items visible in table on load
- Can add new menu item; appears in table after save; menu store is reset
- Can edit existing item; changes reflected in table; menu store is reset
- Can delete item (logic delete); item disappears from table; menu store is reset
- Form validation: TITLE, PATH, GROUP_NAME are required (el-form rules)
- Status toggle works (enabled/disabled)
- Route is only accessible to ROLE_ADMIN users
- Icon field shows placeholder hint for Element Plus icon names

**Recovery:**
- If management page fails to load: check that MENU010 is in the seed data and STATUS='1'. Without it, admin has no sidebar link to the page (can still navigate directly via URL).
- If CRUD operations fail: check SecurityConfig rules. Admin user must have ROLE_ADMIN role.
- If menu store is not reset after CRUD: sidebar continues showing stale data until next full page reload. Verify `useMenuStore().reset()` is called in the success handler of each CRUD operation.

---

### Step 5: Refactor SidebarMenu.vue

**File to modify:** `wh-frontend/src/components/layout/SidebarMenu.vue`

Changes:
1. Remove hardcoded `menuItems` array (lines 81-91)
2. Import and use `useMenuStore` from `@/store/menu`
3. Keep `useUserStore` import -- needed for client-side defense-in-depth perm filtering (see item 4)
4. Replace `visibleItems` computed:
   - Use `menuStore.menuItems` as the data source (already filtered server-side)
   - **Add client-side defense-in-depth filter**: apply the same perm-matching logic as a light assertion. If the client-side filter disagrees with the server, log a warning to console but still use the server result. This catches misconfiguration without breaking the UI.
   - Keep `hidden` filter support (for future use)
5. Keep Dashboard hardcoded `<el-menu-item index="/dashboard">` (not in DB, always visible)
6. `menuGroups` computed: group by `groupName` field (was `group`), group icon from first item's `icon`
7. Keep search, collapse, active-menu highlighting unchanged

**Key Changes -- computed properties:**
```
Before: visibleItems filters hardcoded menuItems by userStore roles
After:  
  - Data source: menuStore.menuItems (server-filtered)
  - Defense-in-depth: re-apply client perm filter; warn on mismatch
  - Group by item.groupName (was item.group)
```

**SidebarMenu.vue template change:**
- `<template v-for="group in filteredGroups">` and `:key="group.name"` remain unchanged
- `<el-sub-menu v-if="group.children.length" :index="group.name">` unchanged
- The only behavioral change is the data source (store vs hardcoded)

**Acceptance Criteria:**
- Sidebar renders 10 menu items with same grouping, icons, titles, order as current
- Admin user logs in -> sees all items in 系统管理 group (including new 菜单管理)
- PM user logs in -> sees only PM-permitted items (no 用户管理, 工作日历, 菜单管理)
- Sponsor user logs in -> sees only SPONSOR-permitted items (成果管理)
- Search filtering still works (searches across group children)
- Dashboard remains always visible (hardcoded)
- Active route highlighting works correctly
- Sidebar collapse/expand works
- Client-side defense-in-depth filter emits console.warn on mismatch (not console.error -- non-blocking)

**Recovery:**
- If sidebar renders empty (only Dashboard visible): check that `menuStore.fetchMenus()` was called and succeeded. Check browser console for API errors. Verify `GET /api/system/menus/user` returns data.
- If sidebar shows wrong items for a role: check `WhPmMenuBo.getUserMenus()` logic -- verify the `perm` field split/trim/exact-match works correctly for edge cases (trailing commas, whitespace).
- If sidebar does not re-render after management page CRUD: verify `menuStore.reset()` is called, and that `fetchMenus()` re-fetches when `loaded` is false.

---

### Step 6: Router + Menu Init Hook + E2E Tests

**File to modify:** `wh-frontend/src/router/index.js`

Changes:
1. Add route for menu management page (insert before the closing `]` of children array):
```js
{
  path: '/system/menu',
  name: 'MenuManagement',
  component: () => import('@/views/system/menu/index.vue'),
  meta: { title: '菜单管理', group: '系统管理', perm: 'ROLE_ADMIN' }
}
```

2. Menu store initialization: DO NOT add to `beforeEach`. Instead, trigger after `userStore.getUserInfo()` succeeds. The `beforeEach` guard already calls `userStore.getUserInfo()` at lines 221-228. Add menu loading after that block:

In the `beforeEach` guard, after the `if (!userStore.userInfo)` block (line 228), add:
```js
// Load menu data after user info is available
const menuStore = (await import('@/store/menu')).useMenuStore()
if (!menuStore.loaded && !menuStore.loading) {
  menuStore.fetchMenus().catch(() => {})
}
```

This placement ensures:
- User identity and roles are loaded before menus
- Menu fetch happens once per session (guarded by `loaded`/`loading`)
- Failure does not block navigation (`.catch(() => {})`)

**File to create:** `wh-frontend/e2e/menu-management.spec.js`

Note: Test path is `wh-frontend/e2e/` (not `tests/e2e/`), matching the existing `playwright.config.js` which has `testDir: './e2e'`. Existing E2E specs are at `wh-frontend/e2e/*.spec.js`.

Scenarios (minimum 7):
1. Admin user logs in -> sidebar shows 系统管理 group with 菜单管理 item
2. Admin user navigates to `/system/menu` -> sees management table with 10 items
3. Admin user creates a new menu item -> item appears in table
4. Admin user edits an existing menu item -> changes saved
5. Admin user deletes a menu item -> item removed
6. PM user logs in -> 系统管理 group shows only non-admin items (产品清单, 成本定额); 用户管理 and 菜单管理 NOT visible
7. PM user navigates to `/system/menu` -> redirected to `/dashboard` or shown access denied

**Acceptance Criteria:**
- `/system/menu` route accessible and renders management page for admin
- Menu store loads on first navigation after user info resolves (not before, not on every route change)
- `loaded`/`loading` double-check prevents redundant fetches
- E2E tests pass (at least 7 scenarios)
- No regression: existing E2E tests in `wh-frontend/e2e/` still pass

**Recovery:**
- If menu route doesn't work: verify the route was added inside the `children` array under the `'/'` Layout route (not at the top level).
- If menu store doesn't load: check that the `.catch(() => {})` handler isn't swallowing a critical error silently. Add `console.warn` inside the catch for debugging.
- If E2E tests fail after migration: the sidebar no longer uses hardcoded `menuItems`. Auth setup in `e2e/auth.setup.js` may need to ensure `getUserMenusApi()` is also called/stubbed.
- Menu route component is lazy-loaded (`() => import(...)`) -- if the file path is wrong, the route will resolve but the component will fail to load at navigation time.

---

## Database Migration SQL (Reference)

### Table: `sys_menu`

| Column | Type | Constraint | Description |
|--------|------|-----------|-------------|
| ID | TEXT | PK | Seed items use MENU0XX; new items use UUID |
| PARENT_ID | TEXT | DEFAULT '0' | Parent menu ID (reserved for future nesting) |
| TITLE | TEXT | NOT NULL | Menu display name |
| PATH | TEXT | NOT NULL | Vue router path |
| COMPONENT | TEXT | NULL | Component path (reserved for dynamic routing) |
| ICON | TEXT | NULL | Element Plus icon component name |
| GROUP_NAME | TEXT | NOT NULL | Sidebar group label |
| SORT_ORDER | INTEGER | DEFAULT 0 | Display order within group |
| PERM | TEXT | NULL | Comma-separated role codes |
| STATUS | TEXT | DEFAULT '1' | 1=enabled, 0=disabled |
| MENU_TYPE | TEXT | DEFAULT 'MENU' | MENU/BUTTON (reserved) |

Plus BaseEntity audit columns: CREATE_BY, CREATE_DATE, UPDATE_BY, UPDATE_DATE, REMARKS, DEL_FLAG, VER_NO, SYS_CODE.

### Seed Data (10 items)

| ID | TITLE | PATH | GROUP_NAME | ICON | SORT | PERM |
|----|-------|------|-----------|------|------|------|
| MENU001 | 项目立项 | /pm/charter | 项目管理 | Document | 1 | ROLE_PM |
| MENU002 | 项目任务 | /pm/wbs | 项目管理 | List | 2 | ROLE_PM |
| MENU003 | 预算管理 | /pm/budget | 项目管理 | Money | 3 | ROLE_PM |
| MENU004 | 工时管理 | /pm/work-hours | 项目管理 | Clock | 4 | ROLE_PM |
| MENU005 | 成果管理 | /pm/deliverable | 项目管理 | Folder | 5 | ROLE_PM,ROLE_SPONSOR |
| MENU006 | 产品清单 | /pm/product | 系统管理 | Tickets | 1 | ROLE_PM |
| MENU007 | 用户管理 | /system/user | 系统管理 | User | 2 | ROLE_ADMIN |
| MENU008 | 工作日历 | /system/calendar | 系统管理 | Calendar | 3 | ROLE_ADMIN |
| MENU009 | 成本定额 | /system/cost-quota | 系统管理 | Money | 4 | ROLE_ADMIN,ROLE_PM |
| MENU010 | 菜单管理 | /system/menu | 系统管理 | Menu | 5 | ROLE_ADMIN |

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Migration file number 048 conflicts with future numbered files | Low | Low | Gap between 047 and 099 is wide. If contested, use 049 or 050. |
| Menu store fetch fails on first load | Low | Medium | Sidebar renders empty (Dashboard still visible). `.catch(() => {})` prevents navigation failure. |
| Icon name typos in DB | Medium | Low | Element Plus renders unknown icon components as empty space. No crash. Admin page has icon name hints. |
| DB seed data and router meta diverge over time | Medium | Medium | Management page shows el-alert warning. Adding a visible menu requires BOTH a DB record AND a router entry. |
| SecurityConfig rule ordering error (user endpoint caught by admin wildcard) | Medium | High | `GET /api/system/menus/user` (specific) MUST appear before `GET /api/system/menus/**` (wildcard). Documented in Step 2a. |
| Sidebar renders nothing for admin after migration (menu management not bootstrapped) | High | High | **Mitigated**: MENU010 seed data ensures 菜单管理 appears in sidebar. Admin can reach the page to manage menus. |
| Client and server permission filters disagree | Low | Medium | Client-side defense-in-depth filter logs `console.warn` on mismatch but uses server result. Non-blocking. |

---

## Open Questions

- [ ] Should the 10 seed menu items be locked (non-deletable) or freely editable by admin? -- Planned: freely editable (consistent with dict seed data pattern)
- [ ] Should disabled menus (STATUS='0') still have their routes accessible via direct URL? -- Planned: yes, route access is controlled by router guard (existing behavior), not by menu visibility.
