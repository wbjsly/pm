## Context

当前系统登录后直接进入 `/pm/charter` 项目列表页，缺少全局概览。左侧菜单仅有「项目管理」一个模块。Header 区域没有用户信息展示。标签系统不支持永久标签（所有标签均可关闭）。系统管理员无法在系统内管理用户。

技术栈：Spring Boot 2.7.18 + MyBatis Plus 3.5.5 + SQLite / Vue 3 + Element Plus 2.6.1 + Pinia + Vue Router 4。

## Goals / Non-Goals

**Goals:**
- 登录后展示 Dashboard 主页，包含项目统计、最近项目、快捷操作
- 管理员可在系统内管理用户（CRUD + 重置密码 + 角色分配）
- Header 右侧展示用户信息，支持下拉操作
- 标签系统支持永久标签（Dashboard 不可关闭）

**Non-Goals:**
- 不包含趋势图表（延期至后续迭代）
- 不包含角色管理、权限管理（系统管理的其他子模块）
- 不包含密码复杂度校验规则
- 不包含多账号切换记忆功能

## Decisions

### D1: Stats API — 独立聚合接口
**Decision**: 新增 `GET /api/pm/charters/stats?pmId={userId}` 返回 counts only，不重用现有的分页列表接口做前端统计。

**Rationale**: 统计只需要 5 个数字，分页接口返回完整实体列表会造成不必要的网络和序列化开销。

**Alternatives considered**:
- A: 前端调用分页接口后计数 — 浪费带宽，当项目多时分页可能不准
- B: 在现有列表接口的 response 中增加 stats 字段 — 耦合度高，且列表接口可能不带 pmId

### D2: User list API — 分页替换现有的 /list
**Decision**: 将现有的 `GET /api/system/users/list` 改为 `GET /api/system/users` 支持分页，保留 keyword 参数。

**Rationale**: 用户数量增长后全量加载不可持续，分页是标准做法。现有 `/list` 接口无分页，前端直接使用下拉选择场景不受影响（该场景改用新的全量接口或在用户管理内做搜索选择）。

**Alternatives considered**:
- A: 保留 /list，新增 /page — 两个接口语义重叠
- B: 保持 /list 全量，前端做分页 — 数据量大时性能差

### D3: Role assignment — 全量替换模式
**Decision**: `PUT /api/system/users/{id}` 接受 `roleIds` 数组，后端先 `deleteByUserId` 再批量插入新关联。

**Rationale**: 全量替换逻辑简单，不需要计算 diff。用户管理是低频操作，性能无问题。

**Alternatives considered**:
- A: 分别提供 addRole/removeRole 接口 — 前端需要维护 diff 逻辑

### D4: Tab closable — meta 驱动
**Decision**: 路由 `meta.closable` 控制标签是否可关闭。Tab store 的 `addTab` 读取该值设置到 tab 对象。所有关闭操作遍历 `tabs` 数组时跳过 `closable: false` 的标签。

**Rationale**: 路由配置声明式，与现有 `meta.hidden` 模式一致，不需要额外 API。

### D5: Dashboard tab position — 始终第一
**Decision**: Dashboard tab 在 tabs 数组中始终排在首位。"关闭右侧"从 dashboard 之后开始关闭。新增标签插入在 dashboard 之后、其他标签之前。

**Rationale**: Dashboard 是主页，视觉上应该在第一个。

## Risks / Trade-offs

| Risk | Impact | Mitigation |
|------|--------|------------|
| Stats 接口按 pmId 过滤，若用户无项目则全 0 | 低 — 这是预期行为 | 空状态友好展示即可 |
| 用户删除后历史数据关联断裂（如作为 PM 的项目） | 中 — 已有项目的 pmId 指向已删除用户 | 使用软删除，保留 ID 引用；列表页 join 时用户不存在显示占位 |
| 重置密码无二次验证（如输入旧密码） | 低 — 仅管理员可操作，管理员本身有高权限 | 依赖 RBAC 保护接口 |
| Dashboard 永久 tab 逻辑与现有 closeAll/closeOther 冲突 | 低 — 已通过过滤逻辑处理 | 单元测试覆盖 |
