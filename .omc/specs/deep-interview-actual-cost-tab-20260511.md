# Deep Interview Spec: 项目实际成本Tab

## Metadata
- Interview ID: actual-cost-tab-20260511
- Rounds: 10
- Final Ambiguity Score: 8.3%
- Type: brownfield
- Generated: 2026-05-11
- Threshold: 0.2 (20%)
- Initial Context Summarized: no
- Status: PASSED

## Clarity Breakdown
| Dimension | Score | Weight | Weighted |
|-----------|-------|--------|----------|
| Goal Clarity | 0.97 | 35% | 0.340 |
| Constraint Clarity | 0.93 | 25% | 0.233 |
| Success Criteria | 0.87 | 25% | 0.218 |
| Context Clarity | 0.85 | 15% | 0.128 |
| **Total Clarity** | | | **0.917** |
| **Ambiguity** | | | **8.3%** |

## Goal
在项目详情页（`wh-frontend/src/views/pm/charter/detail.vue`）的项目预算tab后新增**「项目实际成本」tab**。页面顶部为按月堆叠柱状图（按人工/采购/其他三色堆叠），下方为可过滤的实际成本列表。

## Constraints

### 图表
- 堆叠柱状图，按成本分类三色堆叠（人工/采购/其他）
- 标题显示汇总成本额
- 时间范围：全部有数据的月份
- 数据来源：新增后端聚合接口，返回 `[{yearMonth, laborAmount, procurementAmount, otherAmount}]`

### 分类过滤器
- 四个复选框：全部（默认选中）、人工、采购、其他
- 支持多选，选中即过滤
- 复选框下方单行显示选中分类的汇总金额（如 `¥ 123,456.78`）
- 分类映射**在前端**做：人工=LABOR，采购=PROCUREMENT，其他=TRAVEL,BUSINESS,ENTERTAINMENT,ACTIVITY,OTHER
- 勾选「其他」时，前端展开为 5 个 costType 传给后端

### 列表
- 列：成本日期、成本分类、金额、描述
- 分页：后端分页，100行/页
- 数据来源：`pm_actual_cost` 表
- 过滤参数：`costTypes`（逗号分隔多值）、`yearMonth`（可选，如 "2026-05"）

### 交互
- 点击柱状图某月柱子 → 列表过滤为该月数据（传 yearMonth 参数）
- 点击柱状图标题 → 清除年月过滤，列表恢复全部数据
- 年月过滤 + 分类过滤同时生效（AND 逻辑）

### 技术约束
- 遵循现有 detail.vue 的 lazy-load 模式（loadedTabs + tabLoading）
- 遵循现有前端技术栈：Vue 3 `<script setup>` + Element Plus + ECharts
- 后端遵循 BO 模式
- 列表分页使用后端分页

## Non-Goals
- 不实现按人员展示工时明细（人员姓名、累计工时、成本定额、定额成本等列）
- 不实现采购tab、其他tab的独立页面
- 不修改 pm_actual_cost 表结构
- 不实现工时→实际成本的自动同步逻辑

## Acceptance Criteria
- [ ] 项目详情页「项目预算」tab后出现「项目实际成本」tab
- [ ] 点击tab后：顶部显示堆叠柱状图（按月×三类堆叠），标题显示汇总金额
- [ ] 图下方四个复选框（全部/人工/采购/其他），默认全部选中，下方单行显示选中分类汇总金额
- [ ] 复选框下方实际成本列表（列：成本日期、成本分类、金额、描述），后端分页100行/页
- [ ] 点击柱状图某月柱子 → 列表过滤为该月数据
- [ ] 点击柱状图标题 → 列表恢复全部数据（清除年月过滤）
- [ ] 切换复选框 → 列表按选中分类过滤，汇总金额同步更新
- [ ] 年月过滤 + 分类过滤可同时生效（AND 逻辑）
- [ ] 切换项目时正确重置 tab 状态

## Technical Context

### 前端改动清单

**1. detail.vue（主要改动）**
- 新增 `<el-tab-pane name="actualCost">` 在 budget tab 后
- 扩展 `loadedTabs`、`tabLoading` 增加 `actualCost: false`
- 扩展 `handleTabClick` 增加 `loadActualCost` 加载器
- 扩展 `watch(route.params.id)` 重置逻辑
- 新增 ECharts 堆叠柱状图（参考现有 `renderChart` 模式）
- 新增复选框组 + 汇总金额行
- 新增 `el-table` + `el-pagination` 列表

**2. API 文件** — 扩展 `wh-frontend/src/api/pm/actualCost.js`：
- `getActualCostListApi({ projectId, costTypes, yearMonth, pageNum, pageSize })` → 列表查询（扩展现有）
- `getActualCostAggregationApi(projectId)` → 图表聚合数据

### 后端改动清单

**1. WhPmActualCostController.list()** — 扩展参数：
- 新增 `costTypes`（逗号分隔，如 "LABOR,PROCUREMENT"）
- 新增 `yearMonth`（可选，如 "2026-05"，转换为 `costDate LIKE '2026-05%'`）

**2. WhPmActualCostBo.pageList()** — 扩展 LambdaQueryWrapper：
- `costTypes` → `wrapper.in(WhPmActualCost::getCostType, costTypeList)`
- `yearMonth` → `wrapper.likeRight(WhPmActualCost::getCostDate, yearMonth)`

**3. 新增聚合接口**：
- `GET /api/pm/actual-costs/aggregation?projectId={id}`
- 返回：`[{yearMonth: "2026-05", laborAmount: 10000, procurementAmount: 5000, otherAmount: 3000}]`
- 在 WhPmActualCostDao 新增 SQL：按月 + 三类归并 GROUP BY

### 成本分类归并规则（前端执行）
```
人工 ← LABOR
采购 ← PROCUREMENT
其他 ← TRAVEL, BUSINESS, ENTERTAINMENT, ACTIVITY, OTHER
```

### 数据流
```
Aggregation API: pm_actual_cost → GROUP BY yearMonth + mergedCategory → [{yearMonth, laborAmount, procurementAmount, otherAmount}]
List API: pm_actual_cost → WHERE projectId + costTypes[] + yearMonth → paginated records
```

## Assumptions Exposed & Resolved
| Assumption | Challenge | Resolution |
|------------|-----------|------------|
| 人工tab需要按人员展示工时明细 | pm_actual_cost 无人员字段 | 去掉人员明细，统一列表展示 pm_actual_cost 记录 |
| 三个独立子tab | Contrarian: 是否需要三个tab？ | 改为统一列表 + 复选框过滤（全部/人工/采购/其他） |
| 图表需要堆叠 | Contrarian: 简单柱状图也行？ | 按成本分类三色堆叠 |
| 七种 costType 全部展示 | Simplifier: 归并简化？ | 归并为三类，前端映射 |
| 定额成本=实际成本 | 追问区别 | 定额成本是预算编制用（工时×定额），实际成本来自 pm_actual_cost |
| 图表数据前端聚合 | 数据量大时性能问题 | 新增后端聚合接口 |
| 全部月份 vs 最近12个月 | 数据可能稀疏 | 全部有数据的月份 |
| 汇总金额多卡片展示 | 空间占用 | 单行展示选中分类汇总金额 |
| 分页前端 vs 后端 | 数据量 | 后端分页 100行/页 |
| costType 单值 vs 多值 | 复选框多选需求 | 逗号分隔多值 |
| yearMonth vs 日期范围 | 简化参数 | yearMonth 字符串，后端 likeRight |
| 分类映射位置 | 前后端职责 | 前端展开「其他」为 5 个 costType |

## Ontology (Key Entities)
| Entity | Type | Fields | Relationships |
|--------|------|--------|---------------|
| 实际成本Tab (ActualCostTab) | core domain | chartData, selectedCategories, yearMonthFilter, listData, summaryAmount | 属于 ProjectDetail |
| 堆叠柱状图 (StackedBarChart) | supporting | monthlyData[{yearMonth,laborAmount,procurementAmount,otherAmount}], totalAmount | 点击柱子→过滤列表；点击标题→清除过滤 |
| 分类过滤器 (CostFilter) | supporting | checkboxes[全部,人工,采购,其他], checkedCategories, summaryAmount | 过滤列表 |
| 成本列表 (CostList) | supporting | columns[成本日期,成本分类,金额,描述], pageSize=100, backendPagination | 受年月过滤+分类过滤 AND 控制 |
| 实际成本记录 (WhPmActualCost) | core domain | projectId, costDate, costType, amount, description | 属于项目 |
| 分类映射 (CategoryMapping) | supporting | LABOR→人工, PROCUREMENT→采购, 其余5种→其他 | 前端执行 |

## Ontology Convergence
| Round | Entity Count | Stable | Stability Ratio |
|-------|-------------|--------|----------------|
| 1 | 7 | - | - |
| 2 | 7 | 6 | 86% |
| 3 | 6 | 5 | 83% |
| 4 | 6 | 6 | 100% |
| 5-10 | 6 | 6 | 100% |

领域模型从 Round 4 起完全收敛，持续 6 轮稳定。

## Interview Transcript
<details>
<summary>Full Q&A (10 rounds)</summary>

### Round 1
**Q:** 人工tab中「定额成本」和「实际成本金额」有什么区别？
**A:** 定额成本是预算编制用，实际成本从 pm_actual_cost 取
**Ambiguity:** 51.8%

### Round 2
**Q:** pm_actual_cost 没有人员字段，如何匹配到人？
**A:** 改为统一列表 + 全部/人工/采购/其他四个复选框过滤，去掉三个子tab
**Ambiguity:** 45.0%

### Round 3
**Q:** 统一列表中展示什么列？人员工时明细是否还需要？
**A:** 只展示实际成本记录
**Ambiguity:** 37.3%

### Round 4 (Contrarian)
**Q:** 堆叠维度？列表列？
**A:** 按成本分类堆叠；列=成本日期、金额、成本分类、描述
**Ambiguity:** 30.0%

### Round 5
**Q:** 验收验证步骤？
**A:** 完整交互流程验证
**Ambiguity:** 22.0%

### Round 6 (Simplifier)
**Q:** 「其他」是否包含 TRAVEL+BUSINESS+ENTERTAINMENT+ACTIVITY+OTHER？
**A:** 是，三类归并
**Ambiguity:** 18.8%

### Round 7
**Q:** 图表聚合数据是后端接口还是前端聚合？时间范围？
**A:** 新增聚合接口；全部有数据的月份
**Ambiguity:** 15.1%

### Round 8
**Q:** 汇总金额展示方式？分页方式？
**A:** 单行汇总金额；后端分页
**Ambiguity:** 11.5%

### Round 9
**Q:** 聚合API返回格式？costType过滤参数格式？
**A:** 按月+三类汇总 [{yearMonth, laborAmount, procurementAmount, otherAmount}]；逗号分隔多值
**Ambiguity:** 10.3%

### Round 10
**Q:** yearMonth过滤参数格式？分类映射在前端还是后端？
**A:** yearMonth 字符串（如 "2026-05"）；前端展开「其他」为5个 costType
**Ambiguity:** 8.3%

</details>
