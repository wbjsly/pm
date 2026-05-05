## ADDED Requirements

### Requirement: In-Progress Modification Approval
When a user modifies an IN_PROGRESS WBS node, the system SHALL trigger the `PM_WBS_MODIFY_APPROVAL` Flowable workflow. The workflow SHALL route to BA (product manager) for first approval, then to PM (project manager) for second approval. If the submitter IS the BA role, the BA approval step SHALL be automatically skipped.

#### Scenario: Non-BA user modifies IN_PROGRESS node
- **WHEN** a developer modifies an IN_PROGRESS WBS node
- **THEN** the workflow routes to BA for approval first

#### Scenario: BA user modifies IN_PROGRESS node
- **WHEN** a BA modifies an IN_PROGRESS WBS node
- **THEN** the BA approval step is skipped and the workflow goes directly to PM

#### Scenario: Approval passes
- **WHEN** both BA and PM approve the modification
- **THEN** the changes are applied and a new version (version_number + 0.1) is created

#### Scenario: Approval rejected
- **WHEN** either BA or PM rejects the modification
- **THEN** the changes are discarded, the original node remains unchanged, and the submitter is notified

### Requirement: Reopen Approval
When a user initiates a Reopen on a COMPLETED WBS node, the system SHALL trigger the `PM_WBS_MODIFY_APPROVAL` Flowable workflow with approval_type=REOPEN. The workflow SHALL route to BA then PM. Upon approval, the node status SHALL change to IN_PROGRESS and a new version record SHALL be created.

#### Scenario: Reopen approved
- **WHEN** both BA and PM approve the Reopen request
- **THEN** the node status changes to IN_PROGRESS and a new version is created

#### Scenario: Reopen rejected
- **WHEN** the Reopen is rejected by BA or PM
- **THEN** the node remains in COMPLETED status

### Requirement: Approval Workflow Variables
The `PM_WBS_MODIFY_APPROVAL` workflow SHALL accept the following variables: approval_type (MODIFY or REOPEN), wbs_id, modify_content (description of changes or reopen reason), submitter_id.

#### Scenario: Workflow distinguishes MODIFY vs REOPEN
- **WHEN** the workflow is started with approval_type=REOPEN
- **THEN** the approval form displays reopen-specific fields (reopen reason, impact description)
