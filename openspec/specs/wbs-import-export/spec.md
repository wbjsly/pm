## ADDED Requirements

### Requirement: CSV Import
The system SHALL allow users to import WBS data from a CSV file. The CSV template SHALL NOT include a WBS code column. Instead, hierarchy SHALL be built using a parent node name column. The import process SHALL first process rows with empty parent_name (root nodes), then process rows with parent references.

#### Scenario: Import root nodes first
- **WHEN** a CSV file is uploaded with mixed root and child rows
- **THEN** the system processes all root nodes (empty parent_name) before processing child rows

#### Scenario: Parent node resolution
- **WHEN** a child row references a parent_name
- **THEN** the system first searches in the current import batch, then in existing WBS nodes, then falls back to treating it as a root node (degraded)

#### Scenario: Import with degraded parent
- **WHEN** a child row's parent_name is not found in batch or existing nodes
- **THEN** the row is imported as a root node and marked as "degraded" in the import result

### Requirement: Import Result Report
The system SHALL return an import result containing: total count, success count, degraded count, failed count, and a details array with per-row status and reason for degraded/failed rows.

#### Scenario: Successful import with degradation
- **WHEN** a CSV with 20 rows is imported (15 success, 3 degraded, 2 failed)
- **THEN** the response contains {total: 20, success: 15, degraded: 3, failed: 2, details: [...]}

### Requirement: Excel Export
The system SHALL allow users to export all WBS elements for a given project to an Excel file, including all main table fields and the latest version's planned/actual dates.

#### Scenario: Export project WBS
- **WHEN** a user requests export for project PRJ-2026-001
- **THEN** the system generates an Excel file with all WBS elements in tree order

### Requirement: Template Download
The system SHALL provide a downloadable CSV template file with the correct column headers for WBS import: 序号, 名称, 父节点名称, 产品类型, 产品编码, 模块编码, 优先级, 技术难度, 计划责任人, 估算工时(小时), 估算成本(元), 描述.

#### Scenario: Download import template
- **WHEN** a user requests the import template
- **THEN** the system returns a CSV file with the correct headers and sample row
