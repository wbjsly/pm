## ADDED Requirements

### Requirement: Multi-Project WBS List
The system SHALL provide a page that displays WBS elements grouped by project. Projects SHALL be paginated at 5 per page. Only one project's WBS details SHALL be expanded at a time; expanding a different project SHALL automatically collapse the previously expanded project.

#### Scenario: Default collapsed state
- **WHEN** a user opens the WBS management page
- **THEN** all projects are shown in collapsed state (project header row only)

#### Scenario: Expand a project
- **WHEN** a user clicks to expand a project
- **THEN** that project's WBS elements are loaded and displayed, and any previously expanded project is collapsed

#### Scenario: Pagination
- **WHEN** there are more than 5 projects
- **THEN** the page shows pagination controls, displaying 5 projects per page

### Requirement: WBS Detail Page
The detail page SHALL display all fields of a single WBS element. It SHALL show only the latest version number as a clickable link that navigates to the version history page. The page SHALL include action buttons: Edit, Pause (if IN_PROGRESS), Resume (if SUSPENDED), Reopen (if COMPLETED).

#### Scenario: Display latest version
- **WHEN** a user views a WBS detail page
- **THEN** only the latest version number (e.g., "版本 0.5") is shown prominently

#### Scenario: Navigate to version history
- **WHEN** a user clicks the version number link
- **THEN** the system navigates to the version history table page for that WBS node

### Requirement: Version History Table Page
The version history page SHALL display all versions in a table with columns: version_number, planned_start_date, planned_end_date, actual_start_date, actual_end_date, created_at. Rows with actual dates populated SHALL be expandable to show additional context.

#### Scenario: View version history
- **WHEN** a user opens the version history page for a WBS node
- **THEN** all versions are displayed in a table, newest first

### Requirement: WBS Create/Edit Form
The create/edit form SHALL include fields for: name, product, module, priority, tech_difficulty, planned_owner, owner, effort_estimate, budget_estimate, planned_start_date, planned_end_date, description. The WBS code SHALL be auto-assigned and not editable.

#### Scenario: Create new WBS node
- **WHEN** a PM fills out and submits the WBS create form
- **THEN** the system creates the node with an auto-generated WBS code
