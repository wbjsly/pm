## ADDED Requirements

### Requirement: 序列模拟表

系统 SHALL 提供 wh_sequence 表用于业务编号生成。表包含：SEQ_NAME（主键）、SEQ_VALUE（当前值）、SEQ_PREFIX（前缀）、SEQ_YEAR（年份）。支持按年自动重置编号。

#### Scenario: 创建序列记录

- **WHEN** 系统初始化 PM_CHARTER 序列，SEQ_PREFIX='CHARTER-'，SEQ_VALUE=0
- **THEN** wh_sequence 表中新增该序列记录

### Requirement: 编号生成服务

SequenceService SHALL 提供 generateCode(String seqName, String prefix) 方法。方法在 SQLite 事务中执行：读取当前 SEQ_VALUE → 判断年份是否需要重置 → SEQ_VALUE + 1 → 返回格式化编号。编号格式为 PREFIX + YYYY + NNN（NNN 为 3 位补零序号）。

#### Scenario: 生成章程编号

- **WHEN** 调用 generateCode("PM_CHARTER", "CHARTER-")，当前年为 2026，SEQ_VALUE=0
- **THEN** 返回 CHARTER-2026-001，SEQ_VALUE 更新为 1

#### Scenario: 同年第 N 次调用

- **WHEN** 同年第 5 次调用 generateCode("PM_CHARTER", "CHARTER-")
- **THEN** 返回 CHARTER-2026-005

#### Scenario: 跨年自动重置

- **WHEN** 2027 年首次调用 generateCode("PM_CHARTER", "CHARTER-")
- **THEN** SEQ_YEAR 检测到变化，SEQ_VALUE 重置为 1，返回 CHARTER-2027-001

### Requirement: 编号生成并发安全

编号生成 SHALL 在 SQLite 事务中执行，利用 SQLite 写锁串行化特性保证并发场景下编号唯一性。

#### Scenario: 并发编号生成

- **WHEN** 两个线程同时调用 generateCode 生成编号
- **THEN** 两个线程获得不同的编号值，不存在重复
