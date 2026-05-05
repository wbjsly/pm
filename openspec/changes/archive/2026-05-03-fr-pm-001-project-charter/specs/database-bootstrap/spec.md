## ADDED Requirements

### Requirement: SQL Bootstrap 执行器

系统 SHALL 在 Spring Boot 启动时自动扫描并执行 classpath:db/sqlite/ 目录下的所有 SQL 脚本。脚本按文件名排序执行。执行状态通过 wh_bootstrap_marker 表追踪，确保每个脚本仅执行一次（幂等）。

#### Scenario: 首次启动执行所有 SQL 脚本

- **WHEN** 系统首次启动，wh_bootstrap_marker 表为空
- **THEN** 所有 db/sqlite/ 目录下的 SQL 脚本按文件名排序依次执行

#### Scenario: 重复启动不重复执行

- **WHEN** 系统第二次启动，wh_bootstrap_marker 表中已有脚本执行记录
- **THEN** 已执行过的脚本被跳过，未执行的脚本正常执行

### Requirement: Bootstrap Marker 表

系统 SHALL 创建 wh_bootstrap_marker 表用于追踪已执行的 SQL 脚本。表包含：SCRIPT_NAME（主键）、EXECUTED_AT（执行时间）。该表在 Bootstrap 执行器初始化时自动创建。

#### Scenario: Marker 表自动创建

- **WHEN** Spring Boot 应用启动
- **THEN** 如果 wh_bootstrap_marker 表不存在，则自动创建

### Requirement: Bootstrap 脚本跳过配置

系统 SHALL 支持通过 app.sqlite.bootstrap.skip-scripts 配置项跳过指定的 SQL 脚本。配置值为逗号分隔的文件名列表。

#### Scenario: 跳过指定脚本

- **WHEN** 配置 app.sqlite.bootstrap.skip-scripts 包含 099-pm-charter-seed.sql
- **THEN** 该种子数据脚本被跳过，不执行

### Requirement: SQL 脚本幂等性

所有 Bootstrap SQL 脚本 SHALL 使用幂等语法（CREATE TABLE IF NOT EXISTS、CREATE INDEX IF NOT EXISTS、INSERT ... WHERE NOT EXISTS），确保可重复执行而不产生副作用。

#### Scenario: SQL 脚本重复执行不报错

- **WHEN** 手动重复执行同一 SQL 脚本
- **THEN** 脚本执行成功，不产生重复数据或表已存在错误
