# Plan: 预算升级页面瀑布流布局改造

## RALPLAN-DR 摘要

### Principles (原则)
1. **最小变更原则** — 仅改布局和新增差异计算，不动后端 API、数据加载 (`loadOriginalData`)、提交逻辑 (`handleUpgrade`, `buildItemsTree`)
2. **功能完整性** — 所有现有交互保留：`handlePositionChange` 岗位定额变更、`addItem`/`removeItem` 增删行、`calcLaborAmount`/`calcProcurementAmount` 金额计算
3. **视觉清晰度** — 每个科目的原预算、编辑表单、差异徽章在垂直流中一目了然；差异颜色语义正确（红=增加 / 绿=减少）
4. **代码可维护性** — 单文件保持各科目模板独立，不做强制抽象；新增 computed 按科目分组，命名清晰
5. **防御性计算** — 差异计算处理除零、NaN、undefined 等边界情况

### Decision Drivers (决策驱动因素)
1. **科目结构差异大** — 人工有岗位下拉+工时+定额，采购有 BOM项+数量+单价，其他科目是简单金额输入，强制抽象为通用组件会导致 slots/props 接口过度复杂
2. **回归风险控制** — 核心计算逻辑（`calcLaborAmount`, `calcProcurementAmount`, `buildItemsTree`, `handlePositionChange`）不能引入 bug
3. **测试覆盖** — 差异计算逻辑需要单元测试验证边界情况（除零、负值、空数据）

### Viable Options (可行方案)
| 方案 | 状态 | 理由 |
|------|------|------|
| **A: 纯内联（选定）** | 选定 | 所有差异计算、离开守卫逻辑直接在 `<script setup>` 中实现，不新建文件。最简单、最安全、零认知跳转 |
| B: 提取通用 `BudgetCategorySection.vue` | 淘汰 | 人工/采购/其他科目的字段、校验、交互完全不同，强制抽象需要大量 slots + conditional props，接口复杂度反而超过收益 |
| C: 提取 composable | 考虑后放弃 | 虽然差异计算可独立测试，但引入新文件和新目录（`composables/`）增加项目结构复杂度，且测试可直接通过 wrapper.vm 访问 computed。收益不足以抵消额外的文件管理负担 |

### Pre-mortem (风险预判) — 3个失败场景
1. **差异计算符号错误** — 编辑后金额 < 原预算时显示为红色增加（应为绿色减少），颜色语义颠倒。缓解：使用测试用例覆盖正负值场景
2. **瀑布流中某科目表格撑破布局** — 长 BOM 项名称或岗位名称导致表格宽度超出容器，破坏视觉对齐。缓解：设置 `max-width` + `text-overflow: ellipsis`，各科目卡片 `overflow-x: auto`
3. **beforeRouteLeave 误触发** — 用户刚加载页面未做任何修改，离开时却提示"有未保存修改"。缓解：在 `watch` 中做深度比较而非引用比较，且仅在 `form` 与 `originalData` 有实际差异时才标记为 dirty

### Expanded Test Plan (扩展测试计划)

#### Unit Tests
- 差异金额计算：正差异、负差异、零差异、原预算为0的除零处理
- 差异百分比计算：同上
- 全局差异汇总：多科目聚合后的总差异
- `beforeRouteLeave` 守卫：dirty=true 时弹窗、dirty=false 时直接放行

#### Integration Tests
- 页面加载时原数据和编辑表单正确填充
- 修改人工工时后差异徽章实时更新
- 修改采购数量后差异徽章实时更新
- 修改其他科目金额后差异徽章实时更新
- `handleUpgrade` 提交时携带正确的差异数据

#### E2E Tests
- 完整流程：加载页面 -> 修改各科目数据 -> 确认差异显示正确 -> 点击升级 -> 跳转到预算列表
- 离开确认：修改数据 -> 点击返回 -> 弹窗确认 -> 确认离开

#### Observability
- 页面渲染性能：瀑布流布局下大量数据行（50+ 人工行）的渲染时间
- computed 差异计算开销：多个 computed 对大数据集的响应式开销

---

## Context
当前 `wh-frontend/src/views/pm/budget/upgrade.vue` 使用 `split-panels` flex 左右双栏布局（左侧 = 原预算只读 / 右侧 = 编辑表单）。需要改为垂直瀑布流布局，每个科目区块内独立展示"原预算 + 编辑表单 + 差异徽章"。

## Work Objectives
1. 重构模板为垂直瀑布流布局
2. 新增各科目及全局的差异计算与展示（金额差异 + 百分比差异）
3. 添加 `beforeRouteLeave` / `beforeunload` 离开确认守卫
4. 同步更新 `upgrade.test.js` 覆盖差异计算和新布局
5. 保持所有现有功能不变

## Guardrails

### Must Have
- 每个科目区块：上方差异徽章，内部左右两栏（原预算只读 + 编辑表单）
- 顶部项目信息行右侧：全局差异汇总面板
- 底部：预算汇总（原汇总 vs 新汇总 + 差异）、升级/返回按钮
- 差异颜色：红色 `#F56C6C`（增加）、绿色 `#67C23A`（减少）
- 所有现有交互功能完整保留
- 后端接口不动，差异纯前端 computed 计算

### Must NOT Have
- 不拆分新组件文件（单体重构）
- 不修改后端 API 调用签名
- 不修改 `buildItemsTree` 数据结构
- 不改变 `formatMoney` 行为

---

## Task Flow

### Step 1: 重构模板布局结构
**文件:** `wh-frontend/src/views/pm/budget/upgrade.vue` (template 区块)

**改动点:**
1. 移除 `<div class="split-panels">` 及其 `display: flex` 样式
2. 移除 `.left-panel` / `.right-panel` 包装，改为垂直流结构：

```
<el-card>
  <template #header> [升级预算 | 返回按钮] </template>

  <!-- 1. 项目信息区 + 全局差异面板 -->
  <div class="project-info-bar">
    <div class="info-readonly"> [项目名称/预算编码/版本号/状态] </div>
    <div class="global-diff-panel"> [全局差异汇总] </div>
  </div>

  <el-divider>预算科目</el-divider>

  <!-- 2. 人工科目 -->
  <div class="category-section">
    <div class="section-header">
      <span>人工</span>
      <span class="diff-badge" :style="laborDiffStyle"> [差异金额 / 差异百分比] </span>
    </div>
    <div class="section-body">
      <div class="section-left"> [原人工只读表格] </div>
      <div class="section-right"> [人工编辑表单] </div>
    </div>
  </div>

  <!-- 3. 采购科目（同人工布局） -->

  <!-- 4. 其他科目（同人工布局） -->

  <el-divider>预算汇总</el-divider>

  <!-- 5. 预算汇总对比 -->
  <div class="section-body">
    <div class="section-left"> [原汇总只读] </div>
    <div class="section-right"> [新汇总编辑 + 差异] </div>
  </div>

  <!-- 6. 按钮 -->
  <div class="form-actions"> [升级 | 返回] </div>
</el-card>
```

3. 每个科目的原预算表格数据直接复用现有 computed（`leftLaborItems`, `leftProcurementTotal`, `getLeftOtherAmount` 等）
4. **`<el-form>` 包裹所有可编辑区域**：当前 `<el-form :model="form" :rules="rules" ref="formRef">` 在旧布局中包裹右侧面板。新布局中应将其放在项目信息区之后、第一个 `<el-divider>` 之前，包裹所有三个科目编辑区 + 预算汇总编辑区 + 提交按钮，确保 `formRef.validate()` 正常工作。
5. 编辑表单部分直接挪移现有代码，保持 template 内部的 `v-model` 和事件绑定不变

**验收标准:**
- 页面以垂直瀑布流渲染，不再有左右全页双栏
- 所有三个科目的原预算只读表格和编辑表单均可见
- 按钮在页面底部

---

### Step 2: 新增差异计算 computed
**文件:** `wh-frontend/src/views/pm/budget/upgrade.vue` (script setup 区块)

**改动点:**
新增以下 computed 属性（约 25 行代码）：

```js
// 各科目差异计算
const diffData = computed(() => {
  // 计算原预算各科目金额
  const leftLabor = leftLaborTotal.value
  const leftProcurement = leftProcurementTotal.value
  const leftOther = leftOtherTotal.value
  const leftSubTotal = leftCostBaseline.value  // 项目直接预算（不含管理储备）
  const leftManagementReserve = parseFloat(originalData.value.budget?.managementReserve) || 0
  const leftTotal = leftTotalBudget.value

  // 计算编辑后各科目金额
  const rightLabor = categoryTotal('LABOR')
  const rightProcurement = categoryTotal('PROCUREMENT')
  const rightOther = otherTotal.value
  const rightSubTotal = costBaseline.value  // 项目直接预算（不含管理储备）
  const rightManagementReserve = form.value.managementReserve || 0
  const rightTotal = totalBudget.value

  // 差异
  const laborDiff = rightLabor - leftLabor
  const procurementDiff = rightProcurement - leftProcurement
  const otherDiff = rightOther - leftOther
  const subTotalDiff = laborDiff + procurementDiff + otherDiff  // 科目差异合计
  const managementReserveDiff = rightManagementReserve - leftManagementReserve
  const totalDiff = subTotalDiff + managementReserveDiff  // 总差异

  const safePercent = (diff, base) => base ? (diff / base * 100) : (diff !== 0 ? 100 : 0)

  return {
    labor:       { diff: laborDiff, percent: safePercent(laborDiff, leftLabor) },
    procurement: { diff: procurementDiff, percent: safePercent(procurementDiff, leftProcurement) },
    other:       { diff: otherDiff, percent: safePercent(otherDiff, leftOther) },
    subTotal:    { diff: subTotalDiff, percent: safePercent(subTotalDiff, leftSubTotal) },
    managementReserve: { diff: managementReserveDiff, percent: safePercent(managementReserveDiff, leftManagementReserve) },
    total:       { diff: totalDiff, percent: safePercent(totalDiff, leftTotal) }
  }
})
```

2. 差异颜色计算辅助函数：
```js
const diffColor = (val) => val > 0 ? '#F56C6C' : val < 0 ? '#67C23A' : '#909399'
const diffSign = (val) => val > 0 ? '+' : ''
const diffStyle = (val) => ({ color: diffColor(val) })
```

**验收标准:**
- `diffData.labor.diff` 等于编辑后人工合计减原人工合计
- 原预算为 0 时百分比显示 100%（如果编辑后 > 0）或 0（如果编辑后也是 0）
- diffColor(100) 返回 `#F56C6C`，diffColor(-100) 返回 `#67C23A`

---

### Step 3: 差异展示 UI 集成
**文件:** `wh-frontend/src/views/pm/budget/upgrade.vue` (template + style 区块)

**改动点:**
1. 每个科目 section-header 右侧添加差异徽章：
```html
<span class="diff-badge" :style="diffStyle(diffData.labor.diff)">
  {{ diffSign(diffData.labor.diff) }}¥{{ formatMoney(diffData.labor.diff) }}
  <span class="diff-percent">({{ diffSign(diffData.labor.percent) }}{{ diffData.labor.percent.toFixed(1) }}%)</span>
</span>
```

2. 全局差异汇总面板（在项目信息行右侧）：
```html
<div class="global-diff-panel">
  <div class="diff-title">预算变更汇总</div>
  <div class="diff-row">人工 <span :style="diffStyle(diffData.labor.diff)">...</span></div>
  <div class="diff-row">采购 <span :style="diffStyle(diffData.procurement.diff)">...</span></div>
  <div class="diff-row">其他 <span :style="diffStyle(diffData.other.diff)">...</span></div>
  <div class="diff-row sub-total">科目差异合计 <span :style="diffStyle(diffData.subTotal.diff)">...</span></div>
  <el-divider />
  <div class="diff-row" v-if="Math.abs(diffData.managementReserve.diff) > 0.01">
    管理储备 <span :style="diffStyle(diffData.managementReserve.diff)">...</span>
  </div>
  <div class="diff-row total">总预算差异 <span :style="diffStyle(diffData.total.diff)">...</span></div>
</div>
```

全局差异面板中：
- 「科目差异合计」= 人工差异 + 采购差异 + 其他差异（仅直接预算部分）
- 「管理储备差异」= 编辑后 managementReserve - 原 managementReserve
- 「总预算差异」= 科目差异合计 + 管理储备差异
- 管理储备差异为 0 时隐藏该行，避免干扰

3. 新增 CSS（约 60 行）：
   - `.project-info-bar`: flex, space-between
   - `.global-diff-panel`: 固定宽度（如 280px），浅灰背景，圆角
   - `.category-section`: 下边距
   - `.section-header`: flex, space-between, 与差异徽章对齐
   - `.diff-badge`: 内联块，padding, border-radius, font-weight bold
   - `.section-body`: flex, gap, 两个子元素各 flex:1
   - `.diff-percent`: 稍小字号
   - `.diff-row`: flex, space-between
   - `.diff-row.total`: font-weight bold, 上边框

4. 移除旧 CSS（`.split-panels`, `.left-panel`, `.right-panel`, `.panel-title`, `.panel-card`）中不再需要的样式

**验收标准:**
- 每个科目表头右侧有差异徽章，正差异红色带 `+` 号，负差异绿色
- 全局差异汇总面板在项目信息行右侧，列出各科目差异及总计
- 各科目区块内部左右两栏等宽排列

---

### Step 4: 添加离开未保存修改确认
**文件:** `wh-frontend/src/views/pm/budget/upgrade.vue` (script setup 区块)

**改动点:**
1. 新增 `isDirty` ref：
```js
const isDirty = ref(false)
```

2. 在 `watch` 中监听 form 深度变化，与 originalData 对比标记 dirty：
```js
watch(form, () => {
  // 深度比较 form.items 各科目金额与原始数据
  isDirty.value = hasUnsavedChanges()
}, { deep: true })
```

3. `hasUnsavedChanges()` 函数实现（比较关键字段）：
```js
const hasUnsavedChanges = () => {
  // 比较 managementReserve
  if (form.value.managementReserve !== (parseFloat(originalData.value.budget?.managementReserve) || 0)) return true
  // 比较项目直接预算（各科目合计）
  if (Math.abs(costBaseline.value - leftCostBaseline.value) > 0.01) return true
  return false
}
```

4. 添加 `beforeunload` 事件监听（在 `onMounted` 注册，`onUnmounted` 移除，避免 `<keep-alive>` 缓存场景下监听器累积）：

```js
onMounted(() => window.addEventListener('beforeunload', handleBeforeUnload))
onUnmounted(() => window.removeEventListener('beforeunload', handleBeforeUnload))
```

**导入语句修改（需同时修改两处已有 import）：**
- 第 285 行：`import { ref, computed, onMounted, onActivated, watch } from 'vue'` → 增加 `onUnmounted`：`import { ref, computed, onMounted, onUnmounted, onActivated, watch } from 'vue'`
- 第 286 行：`import { useRoute, useRouter } from 'vue-router'` → 增加 `onBeforeRouteLeave`：`import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'`
- 第 287 行：`import { ElMessage } from 'element-plus'` → 增加 `ElMessageBox`：`import { ElMessage, ElMessageBox } from 'element-plus'`

5. 添加 `beforeRouteLeave` 导航守卫（需新增 `ElMessageBox` 导入：`import { ElMessage, ElMessageBox } from 'element-plus'`），在 `<script setup>` 中使用：
```js
import { onBeforeRouteLeave } from 'vue-router'
onBeforeRouteLeave((to, from, next) => {
  if (isDirty.value) {
    ElMessageBox.confirm('您有未保存的修改，确定要离开吗？', '提示', {
      confirmButtonText: '确定离开',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => next()).catch(() => next(false))
  } else {
    next()
  }
})
```

**验收标准:**
- 修改任意科目金额后 `isDirty` 为 true
- 未修改任何数据时 `isDirty` 为 false
- `isDirty=true` 时点击返回或刷新页面，弹出确认对话框
- 确认后可以离开，取消后停留在当前页
- keep-alive 缓存切换时不会丢失编辑数据（见下方 onActivated 修改）

6. **修改 `onActivated`** 以兼容 keep-alive 场景：

```js
// 当前 onActivated（第 557-560 行）无条件调用 loadOriginalData()
// 修改为仅在非 dirty 状态下重新加载，避免覆盖用户编辑
onActivated(async () => {
  await Promise.all([loadProjects(), loadPositions()])
  if (!isDirty.value) {
    loadOriginalData()
  }
})
```

---

### Step 5: 更新测试
**文件:** `wh-frontend/src/__tests__/views/pm/budget/upgrade.test.js`

**改动点:**

0. **测试基础设施准备**（在 describe 块顶部）：
   - 构造 `mockData` 对象：`{ budget: { projectId, budgetCode, version, managementReserve: '5000' }, items: [ { category:'LABOR', positionId, positionName, hours:10, costRate:100, budgetAmount:1000 }, { category:'LABOR', ... }, { category:'PROCUREMENT', bomItem, qty:2, unitPrice:500, budgetAmount:1000 }, { category:'BUSINESS', budgetAmount:500 } ] }`
   - 修改 API mock 返回 `mockData`：`vi.mock('@/api/pm/budget', () => ({ getBudgetDetailWithItemsApi: vi.fn(() => Promise.resolve({ data: mockData })), upgradeBudgetApi: vi.fn() }))`
   - 通过 `wrapper.vm.diffData` / `wrapper.vm.isDirty` 访问内部状态进行断言
   - elementStubs 确保 `el-divider`、`el-table-column` 被 stub

1. 保留现有 `'渲染预算升级页面'` 测试
2. 新增测试用例：
   - `'差异计算: 人工增加显示红色正数'` — 修改 wrapper.vm form.items 后验证 diffData.labor
   - `'差异计算: 采购减少显示绿色负数'`
   - `'差异计算: 原预算为0时百分比处理正确'`
   - `'瀑布流布局: 三个科目区块均渲染'` — 检查 `.category-section` 数量 >= 3
   - `'显示全局差异汇总面板'` — 检查 `.global-diff-panel` 存在
   - `'修改数据后 marked dirty'` — 修改 form.items.LABOR[0].hours 后验证 isDirty
   - `'未修改数据时 not dirty'` — 加载后立即验证 isDirty 为 false

**验收标准:**
- 所有新增测试用例通过
- 现有测试不退化

---

### Step 6: 手动验证与回归测试
**验证步骤:**
1. 打开预算升级页面，确认垂直瀑布流布局正确渲染
2. 修改人工工时，确认差异徽章实时更新且颜色正确
3. 修改采购数量/单价，确认同上
4. 修改其他科目金额，确认同上
5. 全局差异汇总面板与各科目差异数据一致
6. 点击"添加人员"/"添加BOM项"功能正常
7. 删除行功能正常
8. 切换岗位，定额自动更新功能正常
9. 修改数据后点击返回，确认弹窗提示；点击取消后停留在页面
10. 点击升级，确认提交成功并跳转
11. **keep-alive 场景**：修改数据后切换到其他标签页再切回，确认（a）编辑数据保留（b）差异徽章正确（c）isDirty 状态正确（d）dirty=false 时切回不触发离开弹窗

---

> **浮点数容差说明**：管理储备差异判断使用 `Math.abs(diff) > 0.01` 而非 `!== 0`，避免浮点精度导致无意义的"管理储备: ¥0.00"行显示。`hasUnsavedChanges()` 使用合计值比较而非逐项深度比对——这意味着若用户对科目内部做 offsetting 修改（如人工+5000同时采购-5000），合计不变时不会标记 dirty。这是有意的简化，因为升级场景下此类操作极为罕见。如后续需要更精确的比较，可改为 JSON.stringify 深度对比。

## Success Criteria
- [ ] 页面以垂直瀑布流布局渲染，无左右全页双栏
- [ ] 三个科目各有差异徽章（右上角），正差异红色 + 号，负差异绿色
- [ ] 项目信息行右侧有全局差异汇总面板
- [ ] 所有现有功能完整保留：handlePositionChange、增删行、calcLaborAmount、calcProcurementAmount、handleUpgrade、loadOriginalData
- [ ] beforeRouteLeave 守卫在修改后弹窗确认
- [ ] 后端 API 调用签名不变（`getBudgetDetailWithItemsApi`, `upgradeBudgetApi`）
- [ ] `npm run test -- upgrade` 全部测试通过（含新增测试用例）
- [ ] 原有测试 `'渲染预算升级页面'` 不退化

## ADR (Architecture Decision Record)

### Decision
采用纯内联方案：所有布局改造、差异计算、离开守卫逻辑均在 `upgrade.vue` 单文件内实现，不提取子组件或 composable 文件。

### Drivers
- 三个科目（人工/采购/其他科目）的模板结构差异太大：人工有岗位下拉+工时+定额输入，采购有 BOM项+数量+单价输入，其他科目是简单的金额输入框
- 强制抽象为统一组件需要大量条件渲染和插槽传递，增加认知负担而非减少
- 本次改造仅涉及 1 个文件，新增代码量约 150-200 行，仍在可维护范围内

### Alternatives Considered
- **提取 `BudgetCategorySection.vue` 通用组件**: 淘汰。三个科目字段、交互、校验完全不同，抽象后 props 需包含 10+ 可选字段和多个 slots，接口设计成本高于收益

### Why Chosen
单体重构保持各科目的模板独立，开发者可以直接看到每个科目的完整结构，无需在组件文件和单文件组件间跳转。当前文件 732 行，改造后约 900 行，仍在合理范围内。

### Consequences
- **正面**: 回归风险最低，无需设计组件接口，所有逻辑集中可见
- **负面**: 文件较长，但结构化注释和区块分割可以缓解可读性问题
- **未来风险**: 如果后续需要第三个科目类型（如新增"外包"科目），届时再考虑组件提取

### Follow-ups
- 无。此计划是一次性布局改造。

---

## File Manifest
| 文件 | 操作 | 说明 |
|------|------|------|
| `wh-frontend/src/views/pm/budget/upgrade.vue` | 修改 | 模板重构 + 差异计算 + 离开守卫 + 样式调整 |
| `wh-frontend/src/__tests__/views/pm/budget/upgrade.test.js` | 修改 | 新增差异计算和布局测试用例 |

## 变更量估算
- Template: ~160 行改动（移除 60 行旧结构 + 新增 ~220 行新结构）
- Script: ~100 行新增（diffData computed + 辅助函数 + dirty 检测 + route guard + onActivated 修改）
- Style: ~110 行改动（移除 30 行旧样式 + 新增 ~140 行新样式）
- Tests: ~90 行新增（含 mock 数据构造）
- **upgrade.vue 净增长约 170 行（731 → ~900 行）**
