## ADDED Requirements

### Requirement: WBS Tree CRUD
The system SHALL allow users to create, read, update, and delete WBS elements in a tree structure under a project. Each element SHALL have fields: project_id, wbs_code (auto-generated), parent_id, level (1-4), name, description, element_type (DELIVERABLE/WORK_PACKAGE/PLANNING_PACKAGE), effort_estimate, budget_estimate, product_id, module_id, priority (MUST/SHOULD/COULD/WONT), tech_difficulty (LOW/MEDIUM/HIGH), planned_owner_id, owner_id, latest_planned_end_date, actual_end_date, actual_completed_by, status (PLANNED/IN_PROGRESS/COMPLETED/SUSPENDED), sort_order.

#### Scenario: Create root-level WBS node
- **WHEN** a PM creates a WBS node with no parent
- **THEN** the system assigns wbs_code as N.0 where N is the next available root sequence number

#### Scenario: Create child WBS node
- **WHEN** a PM creates a WBS node under an existing parent
- **THEN** the system assigns wbs_code as parent_code.child_index (e.g., 1.1, 1.2)

#### Scenario: View WBS tree
- **WHEN** a user requests WBS for a project
- **THEN** the system returns the full tree structure ordered by sort_order

#### Scenario: Delete node with children
- **WHEN** a user attempts to delete a WBS node that has child nodes
- **THEN** the system rejects the request with an error indicating child nodes must be removed first

### Requirement: 100% Rule Enforcement
The system SHALL automatically recalculate a parent node's effort_estimate to equal the sum of all its direct children's effort_estimate whenever any child is created, updated, or deleted. This recalculation SHALL propagate recursively to all ancestor nodes.

#### Scenario: Child effort update triggers parent recalculation
- **WHEN** a child WBS node's effort_estimate is changed from 40 to 50
- **THEN** the parent node's effort_estimate is updated to the sum of all its children

#### Scenario: Recursive propagation to root
- **WHEN** a leaf node's effort_estimate is changed
- **THEN** all ancestor nodes up to the root are recalculated

### Requirement: WBS Status State Machine
The system SHALL enforce status-based operation constraints:
- PLANNED: editable, deletable, suspendable
- IN_PROGRESS: editable only via approval workflow, deletable=false, suspendable
- COMPLETED: locked (no edit/delete/pause), reopenable via approval workflow
- SUSPENDED: editable only via approval workflow, resumable to IN_PROGRESS

#### Scenario: Edit IN_PROGRESS node
- **WHEN** a user modifies an IN_PROGRESS WBS node
- **THEN** the system triggers the approval workflow (PM_WBS_MODIFY_APPROVAL)

#### Scenario: Cannot delete COMPLETED node
- **WHEN** a user attempts to delete a COMPLETED WBS node
- **THEN** the system returns 403 Forbidden

#### Scenario: Reopen COMPLETED node
- **WHEN** a user initiates Reopen on a COMPLETED node and approval passes
- **THEN** the node status changes to IN_PROGRESS

### Requirement: Pause and Resume
The system SHALL allow pausing an IN_PROGRESS WBS node (status → SUSPENDED) and resuming a SUSPENDED node (status → IN_PROGRESS). COMPLETED and PLANNED nodes SHALL NOT be pausable.

#### Scenario: Pause IN_PROGRESS node
- **WHEN** a user clicks pause on an IN_PROGRESS WBS node
- **THEN** the node status changes to SUSPENDED

#### Scenario: Cannot pause PLANNED node
- **WHEN** a user attempts to pause a PLANNED node
- **THEN** the system returns 400 Bad Request
