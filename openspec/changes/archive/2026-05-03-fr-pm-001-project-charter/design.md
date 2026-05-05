## Context

WH 管理系统是一个统一 ERP+CRM+PM+HR 管理平台，当前处于纯规格文档阶段（`docs/busi/srs_pm.md` 定义了 49 个功能需求，`docs/tech/devspec-db.md` 和 `docs/tech/devspec-code.md` 定义了技术与数据库规范），没有任何实际实现代码。

FR-PM-001 项目立项（项目章程）是 PM 模块的起点。SRS 中的 DDL 使用 `INTEGER AUTOINCREMENT` 主键，与项目规范要求的 UUID 主键 + BaseEntity 审计字段不兼容，需要全面适配。同时项目需要从零搭建完整骨架，包括 Spring Boot 后端、Vue 3 前端、Docker Compose 基础设施、认证链路、Flowable 审批引擎。

**约束条件：**
- 数据库：SQLite 3（唯一运行时数据库，嵌入式文件）
- 后端：Spring Boot 2.7.18 + MyBatis Plus 3.5.5 + Flowable 6.8.0
- 前端：Vue 3 + Vite 5 + Element Plus 2.6
- 容器：OrbStack（Docker CLI 完全兼容）
- 所有业务数据使用逻辑删除（DEL_FLAG），禁止物理删除
- UUID 主键为 32 位无连字符字符串

## Goals / Non-Goals

**Goals:**
- 从零搭建可运行的完整项目骨架（后端 + 前端 + 基础设施）
- 将 SRS 中 50 张 PM 表 DDL 全部适配为项目规范（UUID 主键 + BaseEntity + 索引）
- 实现 FR-PM-001 项目章程的完整生命周期：创建 → 编辑 → 提交 → Flowable 审批 → 通过/驳回
- 建立可复用的基础设施：认证链路、审批引擎回调框架、Bootstrap 机制、编号生成服务
- 初始化种子数据：10 用户 + 8 角色 + 权限矩阵 + 10 条章程

**Non-Goals:**
- FR-PM-002 ~ FR-PM-049 其他 PM 功能（DDL 会创建表，但无后端/前端代码）
- CRM/HR/FIN/PUR 等其他业务模块
- 审批通过后自动创建管理计划（仅改状态，管理计划创建留到 FR-PM-002）
- 前端 E2E 测试 / 后端单元测试
- CI/CD 流水线 / 生产 Docker 构建

## Decisions

### D1: DDL 一次性全部转换，拆分为 14 个模块文件

SRS 中 50 张表使用 `INTEGER AUTOINCREMENT`，需适配为 `TEXT UUID` + BaseEntity 审计字段。一次性转换保证表间外键引用的一致性，避免分批转换时的中间态问题。

拆分为 14 个文件（`001-system` ~ `099-seed`），按文件名排序自动执行。优点是清晰、可按模块独立维护；Bootstrap 执行器通过 `wh_bootstrap_marker` 表保证幂等。

**替代方案**：一个大文件 —— 可读性和可维护性差，调试困难。

### D2: 自定义 SQL Bootstrap 执行器（轻量级）

`SqliteBootstrap` 组件（`ApplicationRunner`）在 Spring Boot 启动时：
1. 创建 `wh_bootstrap_marker` 表（若不存在）
2. 扫描 `classpath:db/sqlite/*.sql`，按文件名排序
3. 对每个未执行过的脚本：读取 → 执行 → 写 marker 记录
4. 通过 `app.sqlite.bootstrap.skip-scripts` 配置支持跳过特定脚本

**替代方案**：引入 Flyway —— Flyway 对 SQLite 支持有限，且引入额外依赖与项目"轻量单体"定位不符。

### D3: wh_sequence 表实现编号生成

```sql
CREATE TABLE wh_sequence (
    SEQ_NAME    TEXT NOT NULL PRIMARY KEY,
    SEQ_VALUE   INTEGER NOT NULL DEFAULT 0,
    SEQ_PREFIX  TEXT,
    SEQ_YEAR    INTEGER
);
```

BO 层在 SQLite 事务中执行 `SELECT → UPDATE`，利用 SQLite 写锁天然串行化的特性保证编号唯一性。支持按年自动重置（比较 `SEQ_YEAR` 与当前年份）。

**替代方案**：BO 层 `MAX(序号) + 1` —— 并发不安全；ShedLock —— 更重，不如利用 SQLite 自带锁特性。

### D4: ApprovalCompletedCallback 接口 + Spring 自动注入

```java
public interface ApprovalCompletedCallback {
    String getFlowCode();
    void onApproved(String bizId, Map<String, Object> params);
    void onRejected(String bizId, String rejectReason);
}
```

每个业务回调（如 `CharterApprovalCallback`）实现接口并标注 `@Component`。`ApprovalCallbackRegistry` 在启动时通过 `Map<String, ApprovalCompletedCallback>` 自动收集所有回调，`FlowableProcessEndListener` 根据 `flowCode` 查找并调用。

**替代方案**：数据库注册表（反射实例化）—— 运行时可动态增删但复杂度更高；手动注册（`@PostConstruct`）—— 每个模块需手动注册，易遗漏。

### D5: 固定角色分配审批任务

Flowable BPMN 中 userTask 使用 `candidateGroups="ROLE_SPONSOR"`，所有具有 SPONSOR 角色的用户均可看到审批任务并操作。审批人从 Flowable 流程变量中动态传入 `assignee`。

**替代方案**：动态指定审批人 —— 需在 charter 表中硬编码 `sponsorId` 并在流程变量中传递，灵活性更高但增加耦合。固定角色更通用，适合多项目场景。

### D6: 章程审批通过仅改状态

`CharterApprovalCallback.onApproved()` 仅将章程状态从 `PENDING_APPROVAL` 改为 `APPROVED`，不触发管理计划创建。管理计划创建留到 FR-PM-002 实现。

### D7: Bootstrap SQL 中种子数据流程实例 ID 为 NULL

种子数据的 `PROCESS_INSTANCE_ID` 字段设为 NULL，仅用于 UI 展示和列表筛选。Flowable 流程实例仅在运行时动态创建。

### D8: 种子用户统一密码 Admin@123

所有种子用户使用相同初始密码 `Admin@123`，BCrypt 哈希硬编码到 SQL 中。用户首次登录后可自行修改。

### D9: docker-compose 仅包含 Redis + MinIO

Spring Boot App 和前端在宿主机直接运行（`npm run dev`），不容器化。Docker Compose 仅提供基础设施服务。SQLite 为宿主机文件（`data/wh.sqlite`），不走容器 volume 挂载。MinIO bucket 在 Spring Boot `@PostConstruct` 中幂等创建。

### D10: MinIO bucket 自动创建

`MinioConfig` 在 `@PostConstruct` 中检查 bucket 是否存在，不存在则创建。幂等操作，重复执行无副作用。

## Risks / Trade-offs

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| SQLite 写锁串行化，高并发下性能瓶颈 | 多用户同时创建章程时编号生成可能排队 | 开发阶段可接受；生产环境可评估迁移到 PostgreSQL |
| 50 张表一次性 DDL 文件大（~3000 行 SQL） | 调试和审查困难 | 拆分为 14 个模块文件，每个文件 ~200 行 |
| Flowable 6.8.0 内嵌表（ACT_*）与业务表共存同一 SQLite | Flowable 自身写入可能影响性能 | Flowable `async-executor-activate: false`，同步执行 |
| 种子数据中章程状态与真实 Flowable 实例不一致 | 审批操作可能失败（无对应流程实例） | 种子数据仅用于 UI 展示，审批功能仅在运行时创建的章程上操作 |
| SRS DDL 到项目规范的适配可能存在字段遗漏 | 运行时缺失必要字段 | 按 BaseEntity 标准字段清单逐项对照，人工复核 |
| 整个项目从 0 到 1，变更范围大（~93 文件） | 实施周期长，出错概率高 | 按依赖顺序分 8 个阶段逐步推进，每阶段可独立验证 |
