## ADDED Requirements

### Requirement: Version History Tracking
The system SHALL automatically create a new version record in `pm_wbs_version` whenever a WBS node's planned or actual dates are modified. Version numbers SHALL start at 0.1 and increment by 0.1 for each new version.

#### Scenario: New version on date change
- **WHEN** a user modifies the planned_end_date of a WBS node
- **THEN** the system creates a new version record with version_number incremented by 0.1

#### Scenario: First version
- **WHEN** a WBS node is created with planned dates
- **THEN** the initial version is created with version_number 0.1

### Requirement: Version Data Capture
Each version record SHALL capture: wbs_id, version_number, planned_start_date, planned_end_date, actual_start_date (nullable), actual_end_date (nullable), created_by, created_at. When actual dates are first recorded, they SHALL be stored in the current version record.

#### Scenario: Record actual dates
- **WHEN** a user records actual_start_date and actual_end_date on a WBS node
- **THEN** the current version's actual dates are populated (no new version unless planned dates also change)

#### Scenario: Null actual dates indicate planned-only
- **WHEN** a version has null actual_start_date and null actual_end_date
- **THEN** it represents a pure planned version

### Requirement: Latest Planned Date Sync
The `latest_planned_end_date` field on `pm_wbs_element` SHALL be automatically synchronized from the most recent version's `planned_end_date` whenever a new version is created.

#### Scenario: Auto-sync latest_planned_end_date
- **WHEN** a new version 0.3 is created with planned_end_date = 2026-07-15
- **THEN** the parent element's latest_planned_end_date is updated to 2026-07-15

### Requirement: Version History Retrieval
The system SHALL provide an endpoint to retrieve all versions for a given WBS node, ordered by version_number descending (newest first).

#### Scenario: Get version history
- **WHEN** a user requests version history for a WBS node
- **THEN** the system returns all versions sorted newest first
