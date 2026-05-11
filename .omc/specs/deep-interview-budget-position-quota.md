# Deep Interview Spec: 预算管理岗位和成本定额动态化

## Metadata
- Interview ID: b8c9d0e1-2f3a-4b5c-9d0e-1f2a3b4c5d6e
- Rounds: 8
- Final Ambiguity Score: 7.2%
- Type: brownfield
- Generated: 2026-05-11
- Threshold: 0.2
- Status: PASSED

## Clarity Breakdown
| Dimension | Score | Weight | Weighted |
|-----------|-------|--------|----------|
| Goal Clarity | 0.95 | 35% | 0.33 |
| Constraint Clarity | 0.94 | 25% | 0.24 |
| Success Criteria | 0.88 | 25% | 0.22 |
| Context Clarity | 0.94 | 15% | 0.14 |
| **Total Clarity** | | | **0.93** |
| **Ambiguity** | | | **7.2%** |

## Goal
将预算管理的新增/编辑页面中人工项的"岗位"下拉从硬编码改为从交付成本定额的岗位列表动态获取，并将"成本定额"字段从手动输入改为自动计算（dailyRate/8，保留两位小数），不可手动修改。同时详情页同步更新岗位显示方式。

## Constraints
- 岗位下拉选项从 `GET /api/system/cost-quota/positions` 动态加载
- 数据库新增 `position_id` 列（UUID），旧 `role_code` 列保留不动（老数据维持旧值）
- 成本定额（元/时）= dailyRate / 8，保留两位小数，字段只读
- 匹配逻辑：当前日期落在 `wh_sys_cost_year.startDate ~ endDate` 范围内的年份 → 该年份中 `effectiveDate <= 当前日期` 且版本号最大的定额记录
- 若无匹配定额，成本定额显示为 0 且红色标记，不可手动输入
- 仅涉及新增预算和编辑预算页面（form.vue），详情页（detail.vue）同步更新岗位显示
- 新数据存 positionId，老数据 roleCode 保持不变
- **编辑已有预算时**：弹框提示用户选择"使用新定额"还是"保留原值"，展示新旧定额数据对比
- 用户的选择记录在操作日志中（留痕可追溯），不在表格中新增列
- 弹框为**逐行确认**：每个岗位行单独弹出确认框，展示该行的新旧定额对比
- 弹框触发时机：**点击岗位行时触发**（非打开编辑页立即弹出），比较当前日期匹配的定额与已保存的定额，若有差异则弹出确认框

## Non-Goals
- 不涉及预算对比页面（comparison.vue）的修改
- 不删除 roleCode 列（保持向后兼容）
- 不修改审批流程
- 预算列表页（index.vue）不需要显示岗位信息

## Acceptance Criteria
- [ ] 表单页岗位下拉从 `getPositionListApi()` 动态加载（替换硬编码的5个选项）
- [ ] `pm_budget_item_labor` 表新增 `position_id` 列（TEXT）
- [ ] 选择岗位后自动调用后端接口获取对应年度定额的 dailyRate
- [ ] 成本定额自动计算为 `dailyRate / 8`，保留两位小数，字段为只读状态
- [ ] 若当前日期无匹配年份或无匹配定额，成本定额显示红色 `0.00`
- [ ] 提交时 labor 项传 `positionId`（新数据），`roleCode` 仍传但不强制
- [ ] 详情页岗位列改用 `positionId` 查岗位名显示（新数据），旧 roleCode 数据仍用旧映射兼容
- [ ] 编辑已有预算时，点击岗位行触发弹框，展示新旧定额对比，用户选择"使用新定额"或"保留原值"
- [ ] 用户的选择记录到操作日志中（留痕可追溯）
- [ ] 弹框为逐行确认：每个存在定额差异的岗位行单独弹出确认

## Assumptions Exposed & Resolved
| Assumption | Challenge | Resolution |
|------------|-----------|------------|
| 存储 roleCode 字符串即可 | ID关联还是字符串？ | 改为 UUID (positionId)，保留 roleCode 向后兼容 |
| 直接用最新年份定额 | 按什么规则选年份？ | 按编制日期匹配 wh_sys_cost_year 的时间范围 |
| 无定额时用户可手动输入 | 不可手动输入是绝对的？ | 不可编辑，保持为0（红色显示），用户必须先配定额 |
| 只改表单，详情页不动 | 详情页要同步改吗？ | 详情页同步更新，新数据用 positionId 查岗位名 |
| 编辑时直接覆盖原值 | 编辑时要不要提示用户？ | 弹框展示新旧对比，用户选择后记录日志留痕 |
| 弹框一次性处理所有行 | 逐行还是一起确认？ | 逐行确认，每个岗位行单独弹出 |
| 打开页面立刻弹框 | 何时触发弹框？ | 点击岗位行时触发，按当前日期匹配定额与已保存值对比 |

## Technical Context

### 涉及文件
**后端：**
- SQL 迁移：`pm_budget_item_labor` 表新增 `position_id` 列
- `WhPmBudgetItemLabor.java` — 实体新增 `positionId` 字段
- `WhPmBudgetController.java` 或 `WhPmBudgetBo.java` — 可能需要 new endpoint：根据 positionId 返回当前 costRate(dailyRate/8)
- 可复用：`SysPositionDao`、`SysCostYearDao`、`SysCostQuotaDao`

**前端：**
- `src/views/pm/budget/form.vue` — 岗位下拉动态化 + 成本定额自动计算 + 只读
- `src/views/pm/budget/detail.vue` — 岗位显示改用 positionId 或保留兼容逻辑
- `src/api/system/costQuota.js` — 引入 `getPositionListApi`

### 关键技术决策
- 新增 API 端点（建议）：`GET /api/system/cost-quota/quotas/current-rate?positionId=` 返回当前日期对应的 costRate=每日价/8
- 表单加载时先拉取岗位列表，用户选择岗位后立即计算 costRate
- 成本定额用 `el-input` 只读或 disabled 状态显示，highlight 为红色当值为0

## Ontology (Key Entities)
| Entity | Type | Fields | Relationships |
|--------|------|--------|---------------|
| BudgetItemLabor | core domain | id, budgetItemId, roleCode(old), **positionId(new)**, hours, costRate, amount | belongs to Budget |
| SysPosition | supporting | id, name, isDefault, sortOrder | has many BudgetItemLabor |
| SysCostQuota | supporting | id, positionId, yearId, versionNo, dailyRate, effectiveDate | scoped by (position, year) |
| SysCostYear | supporting | id, name, startDate, endDate | contains many quotas |
| 成本定额(元/时) | UI concept | (dailyRate/8, read-only) | computed from SysCostQuota.dailyRate |

## Ontology Convergence
| Round | Entity Count | New | Changed | Stable | Stability Ratio |
|-------|-------------|-----|---------|--------|----------------|
| 1 | 4 | 4 | - | - | N/A |
| 2 | 5 | 1 | 0 | 4 | 80% |
| 3 | 5 | 0 | 0 | 5 | 100% |
| 4 | 5 | 0 | 0 | 5 | 100% |
| 5 | 5 | 0 | 0 | 5 | 100% |
| 6 | 5 | 0 | 0 | 5 | 100% |
| 7 | 5 | 0 | 0 | 5 | 100% |
| 8 | 5 | 0 | 0 | 5 | 100% |

## Interview Transcript
<details>
<summary>Full Q&A (4 rounds)</summary>

### Round 1
**Q:** 当前人工项存储的是 roleCode（如 DEV/QA），成本定额体系用的是 positionId（UUID）。改为关联岗位后，数据库应存储什么？
**A:** 改为存储 positionId (UUID)，roleCode 列可保留
**Ambiguity:** 61% (Goal: 0.45, Constraints: 0.25, Criteria: 0.25, Context: 0.75)

### Round 2
**Q:** 成本定额的 dailyRate 是按年份划分的。计算成本定额时应使用哪个年份的单价？
**A:** 按编制时的日期匹配 wh_sys_cost_year 时间范围，取有效记录；无匹配时红色显示0
**Ambiguity:** 41% (Goal: 0.70, Constraints: 0.55, Criteria: 0.35, Context: 0.80)

### Round 3
**Q:** 当岗位没有匹配的定额记录时，成本定额显示红色0，用户能否手动输入兜底值？
**A:** 不可编辑，保持为0
**Ambiguity:** 25% (Goal: 0.85, Constraints: 0.75, Criteria: 0.55, Context: 0.85)

### Round 4
**Q:** 已有老预算数据存储旧 roleCode，改为 positionId 后旧数据如何处理？详情页是否同步更新？
**A:** 旧数据保持不变（保留 roleCode），详情页同步更新
**Ambiguity:** 14.8% (Goal: 0.92, Constraints: 0.88, Criteria: 0.70, Context: 0.90) — THRESHOLD MET

### Round 5
**Q:** 编辑已有预算时，成本定额应保留原值还是按当前日期重新计算？
**A:** 弹框提醒用户选择使用新定额还是保留原值，用户选择需留痕可追溯
**Ambiguity:** 20% (Goal: 0.90, Constraints: 0.80, Criteria: 0.60, Context: 0.90)

### Round 6
**Q:** "留痕可追溯"如何实现？在表格新增一列还是操作日志记录？
**A:** 仅在操作日志中记录
**Ambiguity:** 12.4% (Goal: 0.93, Constraints: 0.90, Criteria: 0.75, Context: 0.92) — ALL CLEAR

### Round 7
**Q:** 一个预算可以有多个岗位行。编辑时发现定额变化，弹框应该如何提示？
**A:** 逐行确认，每个岗位行单独弹出新旧对比
**Ambiguity:** 9.7% (Goal: 0.94, Constraints: 0.92, Criteria: 0.82, Context: 0.93)

### Round 8
**Q:** 逐行确认弹框在什么时候触发？打开编辑页立即弹出还是点击岗位行时触发？
**A:** 点击岗位行时触发
**Ambiguity:** 7.2% (Goal: 0.95, Constraints: 0.94, Criteria: 0.88, Context: 0.94) — ALL CLEAR

</details>
