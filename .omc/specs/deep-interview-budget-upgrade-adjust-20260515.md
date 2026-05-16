# Deep Interview Spec: 升级预算页面调整区交互改造

## Metadata
- Interview ID: budget-upgrade-adjust-20260515
- Rounds: 7
- Final Ambiguity Score: 15%
- Type: brownfield
- Generated: 2026-05-15
- Threshold: 0.2
- Status: PASSED

## Clarity Breakdown
| Dimension | Score | Weight | Weighted |
|-----------|-------|--------|----------|
| Goal Clarity | 0.95 | 35% | 0.33 |
| Constraint Clarity | 0.85 | 25% | 0.21 |
| Success Criteria | 0.70 | 25% | 0.18 |
| Context Clarity | 0.85 | 15% | 0.13 |
| **Total Clarity** | | | **0.85** |
| **Ambiguity** | | | **0.15** |

## Goal
将 `upgrade.vue` 的调整后预算区域从「预填原值的全量编辑表单」改为「初始为空、逐行加入的调整区」：原预算明细每行末尾增加调整icon，点击后该行出现在调整区（显示原值可编辑，带+/-快捷按钮）；提供分科目的「全部加入调整」按钮；支持新增人员/BOM；保存时前端自动合并调整项和未调整项，组装为现有API兼容格式提交。

## Constraints
- 只修改 `wh-frontend/src/views/pm/budget/upgrade.vue`（前端改动）
- 后端 `upgradeBudgetApi` 接口不动，前端在提交前自动合并调整项和未调整项
- +/- 步长：工时 ±8h，金额 ±1000 元
- 新增人员/BOM 初始值全部为零
- 保留瀑布流布局、差异汇总面板、离开守卫等现有功能
- 配色方案不变：红色(#F56C6C)增加，绿色(#67C23A)减少

## Non-Goals
- 不修改后端接口
- 不改动数据模型
- 不修改审批流程
- 不改动其他预算页面（index/form/detail/comparison）

## Acceptance Criteria
- [ ] 调整后预算区域初始为空（不预填原值）
- [ ] 原预算明细（人工/采购/其他科目）每行末尾有调整icon按钮
- [ ] 点击调整icon后，该行出现在调整区，显示原值（可编辑）+ +/- 按钮（工时±8h/金额±1000）
- [ ] 调整区每行有删除按钮，可移除
- [ ] 每个科目有独立的「全部加入调整」按钮，一键导入该科目所有原预算明细
- [ ] +添加人员/+添加BOM 功能正常，新增项初始值为零
- [ ] 保存时提交完整预算（调整项用新值 + 未调整项用原值），数据计算正确
- [ ] 瀑布流差异汇总面板实时反映调整区变化
- [ ] 管理储备编辑保留
- [ ] 离开未保存修改时弹出确认提示
- [ ] 返回按钮正常工作
- [ ] 现有所有功能不退化

## Assumptions Exposed & Resolved
| Assumption | Challenge | Resolution |
|------------|-----------|------------|
| 调整区预填原值 | 如果只想调几个项，预填全部反而干扰 | 初始为空，逐行加入 |
| 调整是绝对值修改 | 用户说要"叠加调整" | 方案B：显示原值可编辑，+/-在原值基础上增减固定步长 |
| 逐行点击效率低 | 如果有50行要全调 | 分科目「全部加入调整」按钮 |
| 后端需要接收增量数据 | 已确认API结构 | 前端合并后提交绝对值，后端不动 |

## Technical Context
- **文件**: `wh-frontend/src/views/pm/budget/upgrade.vue`（当前约1050行）
- **现有布局**: 瀑布流，左侧原预算只读 + 右侧调整后预算（预填原值可编辑）
- **核心改动**:
  1. `loadOriginalData()` 不再预填 `form.items`，改为只存 `originalData`
  2. 新增 `adjustedItems` reactive 对象，按类别存储已加入调整区的项
  3. 原预算表格每行加操作列，含调整icon按钮
  4. 调整区表格数据源从 `form.items` 改为 `adjustedItems`
  5. `buildItemsTree()` 合并 `adjustedItems` 的新值和未调整项的原值
- **API**: `getBudgetDetailWithItemsApi(id)` 不变；`upgradeBudgetApi(id, data)` 不变

## Ontology (Key Entities)
| Entity | Type | Fields | Relationships |
|--------|------|--------|---------------|
| BudgetUpgrade | core domain | projectName, budgetCode, version, status | has many OriginalBudgetItems, has many AdjustedItems |
| OriginalBudgetItem | core domain | category, positionName, hours, costRate, budgetAmount, bomItem, qty, unitPrice | referenced by AdjustedItem |
| AdjustedItem | core domain | category, originalRef, currentValue (hours/qty/amount), isNew | references OriginalBudgetItem (nullable for new items) |
| CategoryAdjustPanel | supporting | category, items[], hasAddAllButton, hasAddNewButton | contains AdjustedItems |

## Interview Transcript
<details>
<summary>Full Q&A (7 rounds)</summary>

### Round 1
**Q:** 调整机制：用户点击调整icon后，在调整区看到和编辑的是什么？
**A:** 方案B: 显示原值可编辑，+/-在原值基础上增减
**Ambiguity:** 52%

### Round 2
**Q:** 保存范围：只提交调整区中的项还是合并未调整项？+/-按钮控制最终值还是增量字段？
**A:** 合并保存；在原值基础上增减固定步长
**Ambiguity:** 47%

### Round 3
**Q:** 后端API是否需要改动？
**A:** 后端不动，前端合并
**Ambiguity:** 42%

### Round 4 (Contrarian)
**Q:** 调整区是否支持移除和全部加入？
**A:** 支持移除+全部加入
**Ambiguity:** 36%

### Round 5
**Q:** 验收标准优先级排序
**A:** 数据计算正确 > 新增功能 > 现有功能不退化 > 交互完整
**Ambiguity:** 26%

### Round 6 (Simplifier)
**Q:** +/-步长是多少？其他科目如何调整？
**A:** 工时±8h/金额±1000；其他科目也加调整icon
**Ambiguity:** 19.5%

### Round 7
**Q:** 全部加入范围+新增项初始值
**A:** 分科目全部加入；新增项零值初始
**Ambiguity:** 15% (PASSED)

</details>
