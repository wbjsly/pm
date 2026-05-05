## 1. Backend — Stats API

- [x] 1.1 Add `getStatsByPmId(pmId)` method to `WhPmCharterBo` that queries `SELECT status, COUNT(*) FROM wh_pm_project_charter WHERE pm_id = ? AND del_flag = '0' GROUP BY status` and returns `{ total, draft, pending, approved, rejected }`
- [x] 1.2 Add `GET /api/pm/charters/stats?pmId={userId}` endpoint to `WhPmCharterController`

## 2. Backend — User CRUD API

- [x] 2.1 Add `deleteByUserId(String userId)` method to `SysUserRoleDao`
- [x] 2.2 Refactor `SysUserController`: replace `/list` with paginated `GET /api/system/users?pageNum=&pageSize=&keyword=&status=` returning `IPage<SysUser>` with role codes for each user
- [x] 2.3 Add `GET /api/system/users/{id}` endpoint returning single user detail with role codes
- [x] 2.4 Add `PUT /api/system/users/{id}` endpoint accepting user fields + optional `roleIds` array; when roleIds provided, delete existing roles and insert new ones
- [x] 2.5 Add `DELETE /api/system/users/{id}` endpoint performing soft delete (set delFlag = '1')
- [x] 2.6 Add `PUT /api/system/users/{id}/password` endpoint accepting `{ password }`, BCrypt-encode and update

## 3. Frontend — API layer

- [x] 3.1 Create `src/api/pm/stats.js` with `getStatsApi(pmId)` function
- [x] 3.2 Update `src/api/system/user.js`: replace `getUserListApi` with `getUserListApi({ pageNum, pageSize, keyword, status })`, add `getUserDetailApi(id)`, `updateUserApi(id, data)`, `deleteUserApi(id)`, `resetPasswordApi(id, password)`

## 4. Frontend — Router & Tab Store

- [x] 4.1 Update `src/router/index.js`: change redirect from `/pm/charter` to `/dashboard`; add routes for `/dashboard`, `/system/user`, `/system/user/detail/:id`, `/system/user/edit/:id`, `/system/user/profile` with appropriate meta (title, group, perm, hidden, closable)
- [x] 4.2 Update `src/store/tab.js`: add `closable` field support in `addTab` from `route.meta.closable`; update `removeTab`, `closeRight`, `closeOther`, `closeAll` to skip tabs with `closable: false`; ensure dashboard tab is always first in array
- [x] 4.3 Update `src/store/user.js`: add `realName` to userInfo stored from login response; ensure logout clears all state

## 5. Frontend — Sidebar Menu

- [x] 5.1 Update `src/components/layout/SidebarMenu.vue`: add "系统管理" group with "用户管理" child to `menuItems`, with `group: '系统管理'`, `perm: 'ROLE_ADMIN'`

## 6. Frontend — MainLayout Header

- [x] 6.1 Update `src/components/layout/MainLayout.vue`: add user dropdown (el-dropdown) to the right of the global close button, showing nickName; add "用户信息" and "退出/切换账号" dropdown items with handlers

## 7. Frontend — Dashboard Page

- [x] 7.1 Create `src/views/dashboard/index.vue` with: 4 stat cards (total/draft/pending/approved) fetching stats on mount, recent projects table (top 5), quick actions panel with "新增项目" button
- [x] 7.2 Wire stat card clicks to navigate to `/pm/charter?status=XXX`
- [x] 7.3 Wire recent project "查看" to navigate to `/pm/charter/detail/{id}`

## 8. Frontend — User Management Pages

- [x] 8.1 Create `src/views/system/user/index.vue`: user list with paginated table, keyword search, status filter; action column with 查看/编辑/删除/重置密码 icons
- [x] 8.2 Create `src/views/system/user/detail.vue`: read-only user detail showing all fields and role tags
- [x] 8.3 Create `src/views/system/user/edit.vue`: form for editing user fields + multi-select for role assignment
- [x] 8.4 Create `src/views/system/user/profile.vue`: own profile page showing current user's info in read-only format
- [x] 8.5 Implement reset password dialog in user list page (password input + confirm, call API)
- [x] 8.6 Implement delete confirmation in user list page (ElMessageBox.confirm then call API)
