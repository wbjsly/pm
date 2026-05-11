# Deep Interview Spec: 用户管理增加成本岗位字段

## Metadata
- Interview ID: d7e8f9a1-2b3c-4d5e-8f9a-0b1c2d3e4f5a
- Rounds: 6
- Final Ambiguity Score: 6.25%
- Type: brownfield
- Generated: 2026-05-11
- Threshold: 0.2
- Initial Context Summarized: no
- Status: PASSED

## Clarity Breakdown
| Dimension | Score | Weight | Weighted |
|-----------|-------|--------|----------|
| Goal Clarity | 0.95 | 35% | 0.33 |
| Constraint Clarity | 0.95 | 25% | 0.24 |
| Success Criteria | 0.90 | 25% | 0.23 |
| Context Clarity | 0.95 | 15% | 0.14 |
| **Total Clarity** | | | **0.94** |
| **Ambiguity** | | | **6.25%** |

## Goal
在用户管理模块（列表页、编辑页、详情页）中增加"成本岗位"字段。该字段为必填单选，枚举值来自交付成本定额中的岗位列表（`wh_sys_position` 表）。一个用户只属于一个成本岗位（一对一关系）。列表页支持按成本岗位筛选。

## Constraints
- 成本岗位为**必填**字段（数据库 NOT NULL）
- 已有用户通过 SQL 迁移脚本统一设置默认值为"开发"岗位
- 岗位枚举值动态从 `GET /system/cost-quota/positions` 接口获取（与交付成本定额共享同一岗位数据源）
- 列表页搜索区增加岗位下拉筛选框
- 仅涉及列表页(index.vue)、编辑页(edit.vue)、详情页(detail.vue)，不需要独立的新增用户页面
- 权限不变：用户管理仍为 `ROLE_ADMIN` 可操作
- 删除岗位时需检查是否有关联用户，有则禁止删除（与定额记录保护逻辑一致）
- 编辑页岗位下拉框支持 filterable 搜索

## Non-Goals
- 不需要创建独立的用户新增页面
- 不涉及个人资料页(profile.vue)的修改
- 不改变现有的角色管理系统
- 不在用户管理中增加岗位的 CRUD 操作（岗位管理仍在交付成本定额页面）

## Acceptance Criteria
- [ ] `wh_sys_user` 表新增 `position_id` 列（TEXT, NOT NULL），并创建索引
- [ ] SQL 迁移脚本：为已有用户设置默认岗位为"开发"（通过 position name 查找对应的 position id）
- [ ] `SysUser` 实体新增 `positionId` 字段
- [ ] 后端用户列表接口返回数据中包含 `positionId` 和 `positionName`
- [ ] 后端用户详情接口返回数据中包含 `positionId` 和 `positionName`
- [ ] 后端用户更新接口支持接收和保存 `positionId`
- [ ] 列表页(index.vue)新增"成本岗位"列，显示岗位名称
- [ ] 列表页搜索区新增岗位下拉筛选框（选项从岗位 API 动态加载）
- [ ] 编辑页(edit.vue)新增"成本岗位"表单项（el-select，必选，filterable，选项从 API 加载）
- [ ] 详情页(detail.vue)新增"成本岗位"展示字段
- [ ] 编辑提交时 `positionId` 随表单一起提交
- [ ] 后端岗位删除接口增加用户关联检查，有关联用户时禁止删除并提示

## Assumptions Exposed & Resolved
| Assumption | Challenge | Resolution |
|------------|-----------|------------|
| 用户与岗位是多对多关系 | 一对一还是多对多？ | 一对一，每个用户只属于一个成本岗位 |
| 岗位字段可选 | 必填还是选填？ | 必填，已有用户通过迁移脚本统一设置默认值"开发" |
| 只在列表和表单中显示 | 是否需要独立新增页面？ | 不需要，编辑页可兼做新增用途 |
| 已有用户可以保持空值 | 老用户怎么处理？ | 通过 SQL 迁移统一设为"开发"岗位 |
| 列表不需要筛选功能 | 是否要加岗位筛选？ | 列表搜索区增加岗位下拉筛选 |
| 岗位可以随意删除 | 删除岗位时已关联用户怎么办？ | 禁止删除有关联用户的岗位 |
| 下拉框不需要搜索 | 岗位多了怎么快速选择？ | filterable 支持搜索 |

## Technical Context

### 涉及文件
**后端：**
- `wh-backend/src/main/java/com/wh/entity/system/SysUser.java` — 新增 `positionId` 字段
- `wh-backend/src/main/java/com/wh/controller/system/SysUserController.java` — 列表/详情接口返回 positionName，更新接口接收 positionId
- `wh-backend/src/main/java/com/wh/dao/system/SysPositionDao.java` — 已存在，用于查询岗位名称
- `wh-backend/src/main/resources/db/sqlite/044-user-position.sql` — 新建迁移脚本

**前端：**
- `wh-frontend/src/views/system/user/index.vue` — 列表新增列 + 搜索区新增岗位筛选
- `wh-frontend/src/views/system/user/edit.vue` — 表单新增岗位选择
- `wh-frontend/src/views/system/user/detail.vue` — 详情新增岗位展示
- `wh-frontend/src/api/system/user.js` — API 无需变更（现有接口即可）
- `wh-frontend/src/api/system/costQuota.js` — 引入 `getPositionListApi` 获取岗位列表

### 关键技术决策
- 用户表用 `position_id`（TEXT UUID）关联 `wh_sys_position.id`，而非存储岗位名称
- 列表/详情接口在 Controller 的 `userToMap` 方法中通过 `SysPositionDao` 查询岗位名称附加到返回结果
- 前端编辑页加载岗位列表使用已有的 `getPositionListApi`

## Ontology (Key Entities)
| Entity | Type | Fields | Relationships |
|--------|------|--------|---------------|
| SysUser | core domain | id, username, password, nickName, realName, email, phone, avatar, status, **positionId** | SysUser belongs to one SysPosition |
| SysPosition | supporting | id, name, isDefault, sortOrder | SysPosition has many SysUser |
| 成本岗位 | UI concept | (maps to SysPosition.name) | dropdown options for user form |

## Ontology Convergence
| Round | Entity Count | New | Changed | Stable | Stability Ratio |
|-------|-------------|-----|---------|--------|----------------|
| 1 | 3 | 3 | - | - | N/A |
| 2 | 3 | 0 | 0 | 3 | 100% |
| 3 | 3 | 0 | 0 | 3 | 100% |
| 4 | 3 | 0 | 0 | 3 | 100% |
| 5 | 3 | 0 | 0 | 3 | 100% |
| 6 | 3 | 0 | 0 | 3 | 100% |

## Interview Transcript
<details>
<summary>Full Q&A (4 rounds)</summary>

### Round 1
**Q:** 一个用户应该关联几个成本岗位？
**A:** 一对一（每个用户只属于一个成本岗位）
**Ambiguity:** 59% (Goal: 0.45, Constraints: 0.25, Criteria: 0.30, Context: 0.75)

### Round 2
**Q:** 成本岗位字段是否必填？已有的老用户没有岗位数据，该如何处理？
**A:** 必填，老用户给默认值（SQL脚本统一迁移）
**Ambiguity:** 42% (Goal: 0.65, Constraints: 0.55, Criteria: 0.40, Context: 0.80)

### Round 3
**Q:** 当前用户管理只有列表页、编辑页、详情页，没有独立的"新增用户"页面。你说的"新增/修改/查看"具体指哪些页面？
**A:** 编辑页 + 详情页（编辑页作为新增和修改共用）
**Ambiguity:** 30% (Goal: 0.85, Constraints: 0.60, Criteria: 0.50, Context: 0.85)

### Round 4
**Q:** 已有用户迁移时默认分配哪个岗位？列表页是否需要增加按岗位筛选的下拉框？
**A:** 默认"开发"岗位，列表加筛选
**Ambiguity:** 17.5% (Goal: 0.90, Constraints: 0.85, Criteria: 0.65, Context: 0.90) — THRESHOLD MET

### Round 5
**Q:** 如果一个岗位被删除了，但已有用户分配了该岗位，应该怎么处理？
**A:** 禁止删除有关联用户的岗位
**Ambiguity:** 11.8% (Goal: 0.92, Constraints: 0.90, Criteria: 0.80, Context: 0.90)

### Round 6
**Q:** 编辑页的岗位下拉框是否需要像角色选择一样支持搜索（filterable）？还有其他细节吗？
**A:** 支持搜索，没有其他问题了
**Ambiguity:** 6.25% (Goal: 0.95, Constraints: 0.95, Criteria: 0.90, Context: 0.95) — ALL CLEAR

</details>
