# Deep Interview Spec: 预算升级界面瀑布布局改造

## Metadata
- Interview ID: budget-upgrade-layout-20260514
- Rounds: 9
- Final Ambiguity Score: 18.0%
- Type: brownfield
- Generated: 2026-05-14
- Updated: 2026-05-14 (extended to round 11)
- Final: 2026-05-14 (round 14, ambiguity 11.0%)
- Threshold: 0.2
- Status: PASSED

## Clarity Breakdown
| Dimension | Score | Weight | Weighted |
|-----------|-------|--------|----------|
| Goal Clarity | 0.95 | 35% | 0.333 |
| Constraint Clarity | 0.7 | 25% | 0.175 |
| Success Criteria | 0.8 | 25% | 0.200 |
| Context Clarity | 0.75 | 15% | 0.113 |
| **Total Clarity** | | | **0.820** |
| **Ambiguity** | | | **0.180** |

## Goal
将 `upgrade.vue` 从左右全页分栏布局改为**垂直瀑布流布局**：页面从上到下依次为 **项目信息区 → 人工 → 采购 → 其他客户 → 预算汇总**。每个科目区域内左栏展示原预算（只读）、右栏为编辑表单，各科目右上角显示编辑前后差异合计（增红减绿），页面顶部固定全局面板汇总所有科目差异。

## Constraints
- 只修改 `wh-frontend/src/views/pm/budget/upgrade.vue`（前端改动）
- 后端 `upgradeBudgetApi` 接口不动，差异计算由前端基于原数据和新编辑数据自行完成
- 保留现有数据加载流程（`getBudgetDetailWithItemsApi` 加载原预算、`upgradeBudgetApi` 提交升级）
- 配色方案：预算增加用红色(#F56C6C)，预算减少用绿色(#67C23A)

## Non-Goals
- 不修改后端接口
- 不改动数据模型
- 不修改审批流程
- 不改动其他预算页面（index/form/detail/comparison）

## Acceptance Criteria
- [ ] 页面布局为垂直瀑布流：项目信息 → 人工 → 采购 → 其他客户 → 预算汇总
- [ ] 每个科目区域左右分栏：左侧原预算（只读）、右侧编辑表单
- [ ] 每个科目区域右上角显示该科目新旧合计差异（金额 + 百分比），红色增/绿色减
- [ ] 页面顶部固定全局差异汇总面板，汇总所有科目差异
- [ ] 预算汇总区域同样左右分栏展示原汇总 vs 新汇总
- [ ] 编辑后点击"升级"按钮正常提交，跳回列表页
- [ ] 岗位定额变更时弹出确认对话框（保留 form.vue 的交互逻辑）
- [ ] 未保存修改时离开页面弹出确认提示
- [ ] 返回按钮正常工作
- [ ] 现有 upgrade.test.js 测试用例同步更新

## Technical Context
- **文件**: `wh-frontend/src/views/pm/budget/upgrade.vue`
- **现有布局**: `split-panels` flex 容器，左右两栏（`.left-panel` + `.right-panel`）
- **数据源**: `originalData` (ref) — 左栏原数据；`form` (ref) — 右栏编辑数据
- **API**: `getBudgetDetailWithItemsApi(id)` 加载原预算；`upgradeBudgetApi(id, data)` 提交升级
- **差异计算**: 前端 `computed` 属性，基于 `originalData.items` 与 `form.items` 按类别对比
- **全局差异面板**: 页面顶部 `position: sticky` 固定，汇总人工+采购+其他+汇总四项差异

## Layout Structure (New)
```
┌─────────────────────────────────────────────┐
│ 页面标题: 升级预算          [全局差异面板]  │ ← sticky top
│ 项目名称 | 预算编码 | 版本号 | 状态        │ ← 只读
├─────────────────────────────────────────────┤
│ ┌──────────────┐ ┌──────────────────────┐   │
│ │  人工(原)     │ │  人工(编辑)    [差异]│   │
│ │  岗位 工时... │ │  下拉 输入框...     │   │
│ │  合计:¥100   │ │  合计:¥120  +20% ▲  │   │
│ └──────────────┘ └──────────────────────┘   │
├─────────────────────────────────────────────┤
│ ┌──────────────┐ ┌──────────────────────┐   │
│ │  采购(原)     │ │  采购(编辑)    [差异]│   │
│ └──────────────┘ └──────────────────────┘   │
├─────────────────────────────────────────────┤
│ ┌──────────────┐ ┌──────────────────────┐   │
│ │ 其他客户(原)  │ │ 其他客户(编辑) [差异]│   │
│ └──────────────┘ └──────────────────────┘   │
├─────────────────────────────────────────────┤
│ ┌──────────────┐ ┌──────────────────────┐   │
│ │ 预算汇总(原)  │ │ 预算汇总(新)  [差异] │   │
│ │ 总预算=...   │ │ 总预算=...   +15% ▲  │   │
│ └──────────────┘ └──────────────────────┘   │
├─────────────────────────────────────────────┤
│            [升级]  [返回]                    │
└─────────────────────────────────────────────┘
```

## Ontology (Key Entities)

| Entity | Type | Fields | Relationships |
|--------|------|--------|---------------|
| BudgetUpgrade | core domain | projectName, budgetCode, version, status | has many BudgetItems |
| BudgetItem | core domain | category, positionName, hours, costRate, amount, bomItem, qty, unitPrice | belongs to BudgetUpgrade |
| CategorySummary | supporting | categoryLabel, originalTotal, newTotal, diffAmount, diffPercent | aggregates BudgetItems by category |
| GlobalDiffPanel | supporting | laborDiff, procurementDiff, otherDiff, summaryDiff | summarizes all CategorySummary |

## Interview Transcript
<details>
<summary>Full Q&A (9 rounds)</summary>

### Round 1
**Q:** 瀑布方式布局是指所有科目在同一页面从上到下排列，每科目内部左右分栏？
**A:** 是，垂直瀑布流
**Ambiguity:** 59.5%

### Round 2
**Q:** 差异合计是每科目各自显示还是全局汇总？
**A:** 两者都要
**Ambiguity:** 52.5%

### Round 3
**Q:** 差异比较粒度（汇总vs逐项）？配色确认（红增绿减）？
**A:** 科目汇总比较 + 确认配色
**Ambiguity:** 41.5%

### Round 4 (Contrarian)
**Q:** 如果不提交后端，纯前端模拟对比工具有没有价值？
**A:** 必须提交后端
**Ambiguity:** 34.8%

### Round 5
**Q:** 验收最看重什么？
**A:** 布局+配色+计算都重要
**Ambiguity:** 28.5%

### Round 6 (Simplifier)
**Q:** 只改前端布局，后端不动，够用吗？
**A:** 不够，后端也要调（后澄清为不确定，由我建议）
**Ambiguity:** 28.5%

### Round 7
**Q:** 后端具体改什么？
**A:** 不清楚，由我建议 → 结论：前端自行计算差异，后端不改
**Ambiguity:** 26.0%

### Round 8
**Q:** 项目名称是只读展示还是可切换？
**A:** 只读展示项目名
**Ambiguity:** 21.8%

### Round 9
**Q:** 预算汇总区域也是左右分栏？
**A:** 也是左右分栏
**Ambiguity:** 18.0% (PASSED)

</details>
