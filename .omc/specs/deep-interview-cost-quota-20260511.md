# Deep Interview Spec: 交付成本定额管理

## Metadata
- Interview ID: cost-quota-20260511
- Rounds: 11
- Final Ambiguity Score: 6.2%
- Type: brownfield
- Generated: 2026-05-11
- Threshold: 0.2 (20%)
- Initial Context Summarized: no
- Status: PASSED

## Clarity Breakdown
| Dimension | Score | Weight | Weighted |
|-----------|-------|--------|----------|
| Goal Clarity | 0.98 | 35% | 0.343 |
| Constraint Clarity | 0.92 | 25% | 0.230 |
| Success Criteria | 0.92 | 25% | 0.230 |
| Context Clarity | 0.90 | 15% | 0.135 |
| **Total Clarity** | | | **0.938** |
| **Ambiguity** | | | **6.2%** |

## Goal
在 WH PM 系统的「系统管理」菜单下新增**交付成本定额管理**功能。按岗位（Position）和年份（Year）设置人天单价（元/人天），同一岗位在同一年份内可以有多个版本（如调价产生的 v1, v2），但在任意一天只能有一个生效版本。定额用于项目交付成本计算。

**核心概念**：
- **成本定额** = 某个岗位在某个年份版本下的人天单价（元/人天）
- **岗位** = 9 个预设岗位 + 管理员可新增自定义岗位，不可删除有定额记录的岗位
- **年份** = 管理员自定义起止日期的时间段（可跨自然年，支持财年场景），下一年的开始日期默认为上一年的结束日期
- **版本** = 年份内通过「调价」操作产生的定额快照，版本号系统自动递增

## Constraints
- 权限：ROLE_ADMIN 增删改查，ROLE_PM 只读查看
- 岗位：预设 9 个（管理、职能、架构、售前、产品、开发、质量、实施、项目管理），不可删除；管理员可新增自定义岗位
- 有定额记录的岗位（含默认和自定义）不可删除
- 年份起止日期自定义，下一年的开始日期默认取上一年的结束日期
- 同一岗位在同一天只能有一个生效的定额版本
- 版本号系统自动递增（v1, v2, v3...）
- 用户点击「调价」按钮进入编辑，修改单价保存后自动生成新版本
- 遵循现有系统管理模块模式：Controller 直接注入 DAO（无 BO 层）
- 遵循现有前端模式：Vue 3 `<script setup>` + Element Plus，el-card + el-table + el-pagination
- 数据库迁移文件：`db/sqlite/043-sys-cost-quota.sql`，表名 `wh_sys_cost_quota`

## Non-Goals
- 不修改现有的项目预算（WhPmBudget）计算逻辑
- 不在本功能中实现定额与项目预算的自动关联（仅提供数据供查询）
- 不实现审批流（成本定额由管理员直接管理，无需审批）

## Acceptance Criteria
- [ ] 系统管理菜单下出现「交付成本定额」菜单项（ROLE_ADMIN 和 ROLE_PM 可见）
- [ ] 单页面设计：顶部年份选择器 + 下方岗位定额列表
- [ ] 列表列：岗位名称、当前单价（财务格式）、版本号、最近更新时间
- [ ] 可创建新年份（自定义起止日期），创建时自动复制上一年的最新版本定额作为初始值
- [ ] 点击「调价」按钮弹出调价弹窗：新单价（必填）+ 调价原因（选填）+ 生效日期（默认当天）
- [ ] 保存调价后自动生成新版本号（v1, v2, v3...系统自动递增）
- [ ] 可查看某个岗位的历史版本，并排对比两个版本的差异（单价、生效日期）
- [ ] 同一岗位在同一天只有一个生效版本（系统校验）
- [ ] 下一年的开始日期默认为上一年的结束日期（创建新年份时自动填充）
- [ ] 有定额记录的岗位（含默认和自定义）不可删除
- [ ] 有定额记录的年份不可删除
- [ ] ROLE_ADMIN 可增删改查，ROLE_PM 仅可查看（菜单可见但操作按钮置灰/隐藏）
- [ ] 金额格式化为财务格式（千分位，两位小数）

## Assumptions Exposed & Resolved
| Assumption | Challenge | Resolution |
|------------|-----------|------------|
| 定额是"人天单价" | Contrarian: 如果是月薪或年度总成本？ | 确认是人天单价（元/人天），用于交付成本计算 |
| 岗位列表固定 9 个 | Contrarian: 如果需要动态增减？ | 预设 9 个 + 管理员可新增，不可删除默认岗位 |
| 年份是自然年 | 什么是"年份"？ | 自定义起止日期，支持财年场景 |
| 版本号手动输入 | 版本号如何生成？ | 系统自动递增 v1, v2, v3... |
| 权限同其他系统管理功能 | Simplifier: 谁需要访问？ | ROLE_ADMIN 编辑 + ROLE_PM 只读 |
| 编辑即产生新版本 | 版本如何触发？ | 明确的「调价」按钮触发，保存后自动生成新版本 |
| 岗位可随时删除 | 历史数据怎么办？ | 有定额记录的岗位（含自定义）不可删除 |
| 多个页面拆分 | 页面结构如何？ | 单页面 + 弹窗：年份选择器 + 定额列表，调价/历史用弹窗 |
| 新年份定额从零开始 | 初始值如何定？ | 创建新年份时自动复制上一年最新版本定额 |
| 年份可随时删除 | 年份删除规则？ | 有定额记录的年份不可删除（和岗位规则一致） |
| 历史版本简单列表 | 历史版本如何展示？ | 支持并排对比两个版本的差异（单价、生效日期高亮） |
| 列表字段不确定 | 主列表展示什么？ | 岗位名称、当前单价、版本号、最近更新时间 |
| 调价只需单价 | 调价弹窗字段？ | 新单价（必填）+ 调价原因（选填）+ 生效日期（默认当天） |

## Technical Context

### 前端
- **菜单注册**：`wh-frontend/src/components/layout/SidebarMenu.vue` — 在「系统管理」group 下新增菜单项
- **路由**：`wh-frontend/src/router/index.js` — 新增路由，meta: `{ group: '系统管理', title: '交付成本定额', perm: 'ROLE_ADMIN' }`
- **页面文件**：`src/views/system/cost-quota/index.vue`（列表页）、`src/views/system/cost-quota/edit.vue`（编辑/调价页）
- **API 文件**：`src/api/system/costQuota.js`
- **参考模式**：`src/views/system/user/index.vue`（列表 CRUD 模式）、`src/api/system/user.js`（API 模式）

### 后端
- **Controller**：`com.wh.controller.system.SysCostQuotaController` — 直接注入 DAO，无 BO 层
- **Entity**：`com.wh.entity.system.SysCostQuota` — 继承 BaseEntity
- **DAO**：`com.wh.dao.system.SysCostQuotaDao` — MyBatis-Plus BaseMapper
- **参考模式**：`SysUserController.java`（直接 DAO 注入模式）

### 数据库
- **迁移文件**：`db/sqlite/043-sys-cost-quota.sql`
- **表名**：`wh_sys_cost_quota`
- **关键字段**：position（岗位名称）、year_id（关联年份）、version（版本号）、daily_rate（人天单价）、effective_date（生效日期）
- 额外需要岗位表 `wh_sys_position` 和年份表 `wh_sys_cost_year`

## Ontology (Key Entities)

| Entity | Type | Fields | Relationships |
|--------|------|--------|---------------|
| 岗位 (Position) | supporting | name, is_default, sort_order | 有多个 CostQuota |
| 年份 (CostYear) | supporting | name, start_date, end_date | 包含多个 Version；下一年的 start_date 默认 = 上一年的 end_date |
| 版本 (QuotaVersion) | supporting | version_no (auto v1, v2...) | 属于某个 CostYear；包含多个 CostQuota |
| 成本定额 (CostQuota) | core domain | position_id, year_id, version_id, daily_rate, effective_date | 属于某个 Position + CostYear + QuotaVersion；同岗位同一天只有一个生效 |

## Ontology Convergence
{Show how entities stabilized across interview rounds using data from ontology_snapshots in state}

| Round | Entity Count | New | Changed | Stable | Stability Ratio |
|-------|-------------|-----|---------|--------|----------------|
| 1 | 4 | 4 | - | - | - |
| 2 | 4 | 0 | 0 | 4 | 100% |
| 3 | 4 | 0 | 0 | 4 | 100% |
| 4 | 4 | 0 | 0 | 4 | 100% |
| 5 | 4 | 0 | 0 | 4 | 100% |
| 6 | 4 | 0 | 0 | 4 | 100% |
| 7 | 4 | 0 | 0 | 4 | 100% |
| 8 | 4 | 0 | 0 | 4 | 100% |
| 9 | 4 | 0 | 0 | 4 | 100% |
| 10 | 4 | 0 | 0 | 4 | 100% |
| 11 | 4 | 0 | 0 | 4 | 100% |

领域模型从 Round 2 起完全收敛，4 个实体在 10 轮中保持完全稳定。

## Interview Transcript
<details>
<summary>Full Q&A (11 rounds)</summary>

### Round 1
**Q:** 「成本定额」具体是什么？是指人天单价、月薪、还是年度总成本？最终用在哪个业务场景？
**A:** 人天单价（元/人天），用于项目交付成本计算
**Ambiguity:** 77.0% (Goal: 0.25, Constraints: 0.15, Criteria: 0.00, Context: 0.70)

### Round 2
**Q:** 「版本」代表什么？年内多版本调整、每年一个版本、还是年份+版本双层结构？
**A:** 年内多版本调整 — 同一年内定额可调整多次，每次产生新版本，但某一天只有一个生效版本
**Ambiguity:** 68.8% (Goal: 0.45, Constraints: 0.20, Criteria: 0.00, Context: 0.70)

### Round 3
**Q:** 验收场景是什么？核心的 2-3 个验证步骤？
**A:** 完整 CRUD + 版本追溯 — 创建年份→设置定额→调价产生新版本→查看历史→确认生效唯一性
**Ambiguity:** 51.0% (Goal: 0.60, Constraints: 0.25, Criteria: 0.45, Context: 0.70)

### Round 4 (Contrarian)
**Q:** 岗位列表是固定的还是可配置的？
**A:** 预设 + 可扩展 — 9 个默认岗位写死，管理员可新增自定义岗位，不可删除默认岗位
**Ambiguity:** 44.3% (Goal: 0.65, Constraints: 0.45, Criteria: 0.45, Context: 0.70)

### Round 5
**Q:** 年份是自然年还是自定义？版本号手动还是自动？
**A:** 自定义年份起止日期 + 系统自动递增版本号
**Ambiguity:** 36.5% (Goal: 0.80, Constraints: 0.50, Criteria: 0.50, Context: 0.70)

### Round 6 (Simplifier)
**Q:** 谁可以操作？仅管理员还是 PM 也可以？
**A:** ROLE_ADMIN 增删改查，ROLE_PM 只读查看
**Ambiguity:** 27.8% (Goal: 0.85, Constraints: 0.70, Criteria: 0.55, Context: 0.75)

### Round 7
**Q:** 新版本如何触发？有定额记录的岗位能否删除？
**A:** 「调价」按钮触发新版本；有定额记录的岗位（含默认和自定义）不可删除
**Ambiguity:** 17.8% (Goal: 0.90, Constraints: 0.80, Criteria: 0.75, Context: 0.80)

### Round 8
**Q:** 页面结构？单页面还是多页面？
**A:** 单页面 + 弹窗 — 顶部年份选择器切换年份，调价用弹窗，历史版本用弹窗/抽屉
**Ambiguity:** 15.0% (Goal: 0.92, Constraints: 0.82, Criteria: 0.80, Context: 0.82)

### Round 9
**Q:** 创建新年份时各岗位定额如何处理？
**A:** 自动复制上一年最新版本定额作为初始值
**Ambiguity:** 11.5% (Goal: 0.95, Constraints: 0.85, Criteria: 0.85, Context: 0.85)

### Round 10
**Q:** 年份删除规则？历史版本展示方式？
**A:** 有定额记录的年份不可删除；历史版本支持并排对比两个版本差异（单价、生效日期高亮）
**Ambiguity:** 8.5% (Goal: 0.97, Constraints: 0.90, Criteria: 0.88, Context: 0.87)

### Round 11
**Q:** 主列表字段？调价弹窗字段？
**A:** 列表列：岗位名称、当前单价、版本号、最近更新时间。调价弹窗：新单价（必填）+ 调价原因（选填）+ 生效日期（默认当天）
**Ambiguity:** 6.2% (Goal: 0.98, Constraints: 0.92, Criteria: 0.92, Context: 0.90)

</details>
