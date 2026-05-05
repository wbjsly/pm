## ADDED Requirements

### Requirement: Dashboard page as default landing
After successful login, the system SHALL redirect the user to `/dashboard` instead of `/pm/charter`. The `/dashboard` route SHALL be the redirect target in router configuration.

#### Scenario: Login redirect to dashboard
- **WHEN** a user successfully logs in
- **THEN** the router redirects to `/dashboard`

#### Scenario: Default route redirect
- **WHEN** a user navigates to `/`
- **THEN** the router redirects to `/dashboard`

### Requirement: Project statistics cards
The dashboard SHALL display four stat cards showing counts filtered by the logged-in user's ID as PM (project manager): 项目总数 (Total), 草稿 (Draft), 审批中 (Pending), 已通过 (Approved). Each card SHALL display the count and be clickable. Clicking a card SHALL navigate to `/pm/charter` with the corresponding status filter query parameter.

#### Scenario: Display stats counts
- **WHEN** the dashboard page loads
- **THEN** four stat cards display counts fetched from `GET /api/pm/charters/stats?pmId={currentUserId}`

#### Scenario: Click stat card navigates to filtered list
- **WHEN** the user clicks the "草稿" stat card
- **THEN** the router navigates to `/pm/charter?status=DRAFT` and a new tab is created

### Requirement: Recent projects table
The dashboard SHALL display a table showing the 5 most recently created projects for the logged-in user as PM. The table SHALL show columns: 章程编号, 项目名称, 状态, 创建日期. Each row SHALL have a "查看" action that navigates to the project detail page.

#### Scenario: Display recent projects
- **WHEN** the dashboard page loads
- **THEN** a table displays up to 5 recent projects sorted by creation date descending

#### Scenario: Click view in recent projects
- **WHEN** the user clicks "查看" on a project row
- **THEN** the router navigates to `/pm/charter/detail/{id}` and a new tab is created

### Requirement: Quick actions panel
The dashboard SHALL display a quick actions panel with a "新增项目" button. Clicking the button SHALL navigate to `/pm/charter/form`.

#### Scenario: Navigate to new project form
- **WHEN** the user clicks "新增项目" in quick actions
- **THEN** the router navigates to `/pm/charter/form` and a new tab is created

### Requirement: Dashboard stats API endpoint
The backend SHALL provide `GET /api/pm/charters/stats?pmId={userId}` returning `{ total, draft, pending, approved, rejected }` counts of non-deleted charters where `pm_id` matches the given user ID, grouped by status.

#### Scenario: Get stats for a user
- **WHEN** a GET request is made to `/api/pm/charters/stats?pmId=user001`
- **THEN** the response contains counts grouped by status for charters where pm_id = 'user001' and del_flag = '0'
