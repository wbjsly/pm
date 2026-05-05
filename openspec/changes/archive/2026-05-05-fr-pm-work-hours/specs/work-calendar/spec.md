## ADDED Requirements

### Requirement: 选择年份
管理员 SHALL 能选择目标年份（如2026年），并在该年份下管理工作日历。

#### Scenario: 选择年份
- **WHEN** 管理员进入工作日历管理页面
- **THEN** 系统默认展示当前年份的日历

### Requirement: 一键生成全年日历
管理员 SHALL 能通过点击"一键生成"按钮，自动为所选年份的每一天创建日历记录。默认规则：周一至周五设为工作日（标准工时8h），周六和周日设为周末。

#### Scenario: 生成全年日历
- **WHEN** 管理员选择2026年并点击"一键生成"
- **THEN** 系统为2026年365天的每一天创建日历记录，周一到周五为WORKDAY(8h)，周六日为WEEKEND

#### Scenario: 重复生成覆盖
- **WHEN** 管理员已生成2026年日历后再次点击"一键生成"
- **THEN** 系统覆盖已有的日历记录，重新按默认规则生成

### Requirement: 手动调整日期类型
管理员 SHALL 能点击日历中任意格子，将其日期类型在工作日、周末、节假日之间切换，并可设置节假日名称（如"春节"、"国庆节"）。

#### Scenario: 设置法定节假日
- **WHEN** 管理员点击5月1日的格子，将类型设为"节假日"，名称设为"劳动节"
- **THEN** 该日的 DAY_TYPE 变为 HOLIDAY，HOLIDAY_NAME 为"劳动节"

#### Scenario: 设置调休补班
- **WHEN** 管理员点击某个周六的格子，将类型设为"工作日"
- **THEN** 该日的 DAY_TYPE 变为 WORKDAY，标准工时为8h

### Requirement: 配置标准工时
系统 SHALL 允许管理员为每个工作日配置不同的标准工作时长，默认为8小时。

#### Scenario: 调整标准工时
- **WHEN** 管理员将某工作日的标准工时从8h改为6h
- **THEN** 该日的 STANDARD_HOURS 更新为"6"

### Requirement: 批量设置
管理员 SHALL 能通过快捷按钮批量设置工作日或周末。

#### Scenario: 批量设置周末
- **WHEN** 管理员选择日期范围后点击"批量设为周末"
- **THEN** 范围内所有日期的 DAY_TYPE 更新为 WEEKEND
