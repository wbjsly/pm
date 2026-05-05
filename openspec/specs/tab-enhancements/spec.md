## ADDED Requirements

### Requirement: Permanent tab support
The tab system SHALL support a `closable` flag on each tab. When `closable` is `false`, the tab SHALL not display a close icon and SHALL not be closable via tab close button, context menu operations, or "close all" action.

#### Scenario: Permanent tab has no close icon
- **WHEN** a tab has `closable: false`
- **THEN** the tab does not show a close icon

#### Scenario: Close all skips permanent tabs
- **WHEN** the user triggers "关闭所有标签"
- **THEN** permanent tabs remain open and only closable tabs are removed

### Requirement: Dashboard as permanent tab
The `/dashboard` tab SHALL be created with `closable: false`. It SHALL always remain as the first tab.

#### Scenario: Dashboard tab is not closable
- **WHEN** the dashboard tab is active
- **THEN** no close icon is displayed on the dashboard tab

#### Scenario: Close others keeps dashboard
- **WHEN** the user clicks "关闭其他" on any tab
- **THEN** the dashboard tab remains open alongside the clicked tab

### Requirement: Context menu respects permanent tabs
The context menu operations "关闭右侧", "关闭其他", and "全部关闭" SHALL skip tabs with `closable: false`.

#### Scenario: Close right skips permanent tab on left
- **WHEN** the user clicks "关闭右侧" on the dashboard tab
- **THEN** all tabs to the right of dashboard are closed (since they are all closable)

#### Scenario: Close other skips permanent tab
- **WHEN** the user clicks "关闭其他" on a non-dashboard tab
- **THEN** the dashboard tab and the clicked tab remain open
