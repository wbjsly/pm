## ADDED Requirements

### Requirement: User list page
The system SHALL provide a user list page at `/system/user` accessible only to users with ROLE_ADMIN. The page SHALL display a paginated table with columns: 账号 (username), 昵称 (nickName), 真实姓名 (realName), 邮箱 (email), 手机 (phone), 角色 (role names), 状态 (status), 操作 (actions). The page SHALL support keyword search across username and realName.

#### Scenario: Admin views user list
- **WHEN** a user with ROLE_ADMIN navigates to `/system/user`
- **THEN** a paginated table displays all users with their information

#### Scenario: Non-admin cannot access user list
- **WHEN** a user without ROLE_ADMIN navigates to `/system/user`
- **THEN** the sidebar does not show the "用户管理" menu item

#### Scenario: Search users by keyword
- **WHEN** the user enters a keyword and clicks "查询"
- **THEN** the table filters to users whose username or realName contains the keyword

### Requirement: User detail page
The system SHALL provide a read-only user detail page at `/system/user/detail/:id`. The page SHALL display all user fields (username, nickName, realName, email, phone, avatar, status, roles) in a description list format. The page SHALL show a "返回" button to navigate back to the user list.

#### Scenario: View user details
- **WHEN** the user clicks "查看" icon on a user row
- **THEN** the router navigates to `/system/user/detail/{id}` showing read-only user information

### Requirement: User edit page
The system SHALL provide a user edit page at `/system/user/edit/:id`. The page SHALL allow editing of user fields (nickName, realName, email, phone, status) and role assignment via a multi-select dropdown listing all available roles. Submitting the form SHALL call `PUT /api/system/users/{id}` with updated user data and role assignments.

#### Scenario: Edit user information
- **WHEN** the user clicks "编辑" icon and modifies fields then submits
- **THEN** the user's information is updated and the page navigates back to the user list

#### Scenario: Assign roles to user
- **WHEN** the admin selects roles in the role multi-select and submits
- **THEN** the user's role assignments are updated to match the selected roles

### Requirement: User delete
The system SHALL allow administrators to soft-delete users. Clicking the delete icon SHALL show a confirmation dialog. Upon confirmation, the system SHALL call `DELETE /api/system/users/{id}` to set delFlag = '1'.

#### Scenario: Soft delete a user
- **WHEN** the admin clicks the delete icon and confirms
- **THEN** the user's delFlag is set to '1' and they no longer appear in the active user list

### Requirement: Reset user password
The system SHALL allow administrators to reset a user's password via a dialog. The dialog SHALL require entering a new password twice (with confirmation). Upon submission, the system SHALL call `PUT /api/system/users/{id}/password` with the new password, which SHALL be BCrypt-hashed before storage.

#### Scenario: Reset password successfully
- **WHEN** the admin clicks the reset password icon, enters a new password twice, and submits
- **THEN** the user's password is updated with BCrypt hash and a success message is shown

#### Scenario: Password confirmation mismatch
- **WHEN** the admin enters two different passwords in the reset dialog
- **THEN** a validation error message is displayed and the form is not submitted

### Requirement: User list paginated API
The backend SHALL provide `GET /api/system/users?pageNum=&pageSize=&keyword=&status=` returning a paginated result. The response SHALL include total count, page number, page size, and list of users. Each user entry SHALL include role codes. The query SHALL exclude soft-deleted users by default (delFlag = '0').

#### Scenario: Get paginated user list
- **WHEN** a GET request is made to `/api/system/users?pageNum=1&pageSize=10`
- **THEN** the response contains paginated user data with total count

#### Scenario: Get user with keyword filter
- **WHEN** a GET request is made with `keyword=张`
- **THEN** the response includes only users whose username or realName contains "张"

### Requirement: Get single user API
The backend SHALL provide `GET /api/system/users/{id}` returning a single user's full information including assigned role codes.

#### Scenario: Get user detail
- **WHEN** a GET request is made to `/api/system/users/user001`
- **THEN** the response contains the user's fields and an array of assigned role codes

### Requirement: Update user API
The backend SHALL provide `PUT /api/system/users/{id}` accepting user fields and an optional `roleIds` array. When `roleIds` is provided, the system SHALL replace all existing role assignments for the user with the new set.

#### Scenario: Update user and roles
- **WHEN** a PUT request is made to `/api/system/users/user001` with updated fields and roleIds
- **THEN** the user's information is updated and role assignments are replaced with the new roleIds

### Requirement: Delete user API
The backend SHALL provide `DELETE /api/system/users/{id}` that performs a soft delete by setting delFlag = '1'.

#### Scenario: Soft delete user
- **WHEN** a DELETE request is made to `/api/system/users/user001`
- **THEN** the user's delFlag is set to '1'

### Requirement: Reset password API
The backend SHALL provide `PUT /api/system/users/{id}/password` accepting `{ password }`. The password SHALL be BCrypt-hashed before storage.

#### Scenario: Reset user password
- **WHEN** a PUT request is made to `/api/system/users/user001/password` with `{ "password": "newPass123" }`
- **THEN** the user's password is updated with the BCrypt hash of "newPass123"

### Requirement: System management menu
The sidebar menu SHALL include a "系统管理" group with a "用户管理" child menu item. The group SHALL only be visible to users with ROLE_ADMIN. The menu item SHALL navigate to `/system/user`.

#### Scenario: Admin sees system management menu
- **WHEN** a user with ROLE_ADMIN logs in
- **THEN** the sidebar displays "系统管理" with "用户管理" child
