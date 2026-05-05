## ADDED Requirements

### Requirement: User profile page
The system SHALL provide a user profile page at `/system/user/profile` showing the logged-in user's own information (username, nickName, realName, email, phone, roles) in a read-only format. The page SHALL be accessible to all authenticated users.

#### Scenario: View own profile
- **WHEN** the user navigates to `/system/user/profile`
- **THEN** their own user information is displayed in read-only format

### Requirement: User account dropdown in header
The tab header bar SHALL display the logged-in user's nickName (or username if nickName is empty) on the right side, next to the global close button. The user name SHALL be wrapped in an el-dropdown component. Clicking the dropdown SHALL reveal two options: "用户信息" and "退出/切换账号".

#### Scenario: Display user account dropdown
- **WHEN** a user is logged in
- **THEN** the header right side shows their nickName with a dropdown arrow

#### Scenario: Navigate to profile
- **WHEN** the user clicks "用户信息" in the dropdown
- **THEN** the router navigates to `/system/user/profile` and a new tab is created

### Requirement: Logout and switch account
Clicking "退出/切换账号" in the user dropdown SHALL call `userStore.logout()`, clear the local token, and redirect to the login page with `redirect=/dashboard` query parameter.

#### Scenario: Logout successfully
- **WHEN** the user clicks "退出/切换账号"
- **THEN** the token is cleared and the user is redirected to `/login?redirect=/dashboard`
