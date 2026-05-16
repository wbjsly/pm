# Plan: 升级预算调整区交互改造

**Created:** 2026-05-15 | **Revised:** 2026-05-15 (r2) | **Scope:** Single-file refactor (`upgrade.vue`) | **Complexity:** HIGH

---

## RALPLAN-DR Summary

### Principles (3-5)
1. **Minimal API Surface** -- Backend interface is the contract. Frontend adapts, never the reverse.
2. **Progressive Engagement** -- Empty initial state; user explicitly opts items into adjustment rather than editing a pre-filled form.
3. **Data Integrity** -- Merged submission must produce identical byte-for-byte results for unadjusted items as if they were never touched.
4. **Preserve Everything** -- Every existing feature (waterfall, route guard, position rate fetching, management reserve) must survive intact.

### Decision Drivers (Top 3)
1. **Correctness of merged submission** -- `buildItemsTree()` must reconstruct the full items payload: adjusted items with new values + unadjusted originals with original values. Backend `createBudgetItemRecursive` always calls `insert` and does not read `req.getId()`, so `id` is not required in the payload (included optionally for traceability).
2. **Waterfall real-time sync** -- All computed totals on the right side must reflect merged (adjusted new + unadjusted original) values, not just `adjustedItems` totals.
3. **Single-file constraint** -- No new files, no shared refactors; all changes contained in `upgrade.vue`.

### Viable Options

| # | Option | Pros | Cons |
|---|--------|------|------|
| A | **adjustedItems as separate reactive, form.items removed** -- `adjustedItems` replaces `form.items` entirely as the right-panel data source. | Clean separation; no dual data management; `form` only holds `managementReserve`. | Deletes `form.items` entirely; must rewire ~30 references. |
| B | **adjustedItems added, form.items kept as merged view** -- `adjustItems` tracks explicit selections; `form.items` recomputed by watcher. | Minimal template changes. | Dual-source complexity; watcher sync fragile; two truths problem. |
| C | **adjustedItems replaces form.items, form simplified** -- Same as A, but `form` further simplified to just `{ projectId, managementReserve }`. | Cleanest model; zero confusion about where data lives. | Slightly more refactoring of `form` object shape. |

**Chosen: Option C** -- `adjustedItems` replaces `form.items` entirely. `form` becomes `{ projectId, managementReserve }` only. This eliminates the dual-source problem entirely and makes the data flow unidirectional: originalData (read-only left) -> adjustedItems (explicitly added, editable right) -> merged for submit.

Options A/B were invalidated because B introduces a "two truths" problem (adjustedItems vs form.items can diverge), and A is essentially C with unnecessary `form.items` scaffolding.

### ADR

- **Decision:** Replace `form.items` with `adjustedItems` reactive; simplify `form` to `{ projectId, managementReserve }`; compute all right-side totals and submission payload via merge functions.
- **Drivers:** Data integrity (no stale duplicate), single-file constraint, correctness of waterfall real-time sync.
- **Alternatives considered:** See Option B above (rejected: dual-source complexity). Separating into multiple components was considered but rejected (spec constraint: single file).
- **Why chosen:** Eliminates the root cause of potential data divergence. The merge logic is a pure function computable at any time, making waterfall diff inherently correct.
- **Consequences:** Must rewrite every reference to `form.items` (~30 occurrences in template and script). Template binding changes from `form.items.LABOR` to `adjustedItems.LABOR`. Procurement amount adjustment needs a `manualAdjustment` field to avoid conflicting with `calcProcurementAmount`.
- **Follow-ups:** None. This is a self-contained feature change.

---

## Requirements Summary

将 `upgrade.vue` 的调整后预算区域从「预填原值的全量编辑表单」改为「初始为空、逐行加入的调整区」。用户在原预算明细行点击调整icon后，该行出现在右侧调整区，显示原值（可编辑），并提供快捷 +/- 按钮（人工工时 +/-8h，金额 +/-1000）。

## Acceptance Criteria (from spec)

- [ ] 调整后预算区域初始为空（不预填原值）
- [ ] 原预算明细（人工/采购/其他科目）每行末尾有调整icon按钮
- [ ] 点击调整icon后，该行出现在调整区，显示原值（可编辑）+ +/- 按钮（工时+/-8h/金额+/-1000）
- [ ] 调整区每行有删除按钮，可移除
- [ ] 每个科目有独立的「全部加入调整」按钮，一键导入该科目所有原预算明细
- [ ] +添加人员/+添加BOM 功能正常，新增项初始值为零
- [ ] 保存时提交完整预算（调整项用新值 + 未调整项用原值），数据计算正确
- [ ] 瀑布流差异汇总面板实时反映调整区变化
- [ ] 管理储备编辑保留
- [ ] 离开未保存修改时弹出确认提示（包括仅调整项变化时的检测）
- [ ] 返回按钮正常工作
- [ ] 现有所有功能不退化

## Non-Goals

- 不修改后端接口
- 不改动数据模型
- 不修改审批流程
- 不改动其他预算页面（index/form/detail/comparison）

---

## Implementation Steps

### Step 1: Left Panel -- Add Adjust Icons and "Add All" Buttons (SAFE, ADDITIVE)

**File:** `wh-frontend/src/views/pm/budget/upgrade.vue`

**Why this first:** This step is purely additive -- it adds new UI elements and handler stubs without changing any existing data flow or template bindings. The page continues to function exactly as before. This allows immediate visual testing and establishes the `addToAdjust` / `addAllToAdjust` API that subsequent steps build on.

**What to do:**

**1a. LABOR left table** (current line ~137-152):
- Add an "操作" column (width: 60) at the end of the left LABOR table.
- Each row gets an adjust icon button (use `<el-icon><Edit /></el-icon>` or an icon component consistent with the project's icon conventions).
- Click handler: `addToAdjust('LABOR', row)` where `row` is the original item from `leftLaborItems`.
- Below the table, add `<el-button size="small" @click="addAllToAdjust('LABOR')">全部加入调整</el-button>`.

**1b. PROCUREMENT left table** (current line ~211-224):
- Same as LABOR: add "操作" column with adjust icon per row.
- Add "全部加入调整" button below the table.

**1c. 其他科目 left panel** (current line ~279-288):
- In each `.other-cat-item` div, add a small adjust icon next to the label.
- Click handler: `addToAdjust(cat.value, { category: cat.value, budgetAmount: getLeftOtherAmount(cat.value) })`.
- Add "全部加入调整" button below the row.

**1d. Logic -- `addToAdjust(category, originalItem)`:**
```js
function addToAdjust(category, originalItem) {
  // Skip if already added
  if (originalItem.id) {
    const exists = adjustedItems[category].some(item => item.originalId === originalItem.id)
    if (exists) return
  }
  // For single-value categories (其他科目), also dedupe by non-empty array
  const singleValueCategories = ['TRAVEL','BUSINESS','ENTERTAINMENT','ACTIVITY','OTHER']
  if (singleValueCategories.includes(category) && adjustedItems[category].length > 0) {
    return  // already has one item for this 其他科目 sub-category
  }
  const newItem = createAdjustedItem(category, originalItem)
  adjustedItems[category].push(newItem)
}
```
> Note: 其他科目 sub-categories each hold a single value. The `originalItem` passed to `addToAdjust` is constructed from the category data since 其他科目 items in `originalData.items` may not have the same structure as LABOR/PROCUREMENT items. The `originalItem.id` fallback check plus the array-length guard ensures deduplication works regardless of whether the original item has an `id`.

**1e. Logic -- `addAllToAdjust(category)`:**
```js
function addAllToAdjust(category) {
  const items = allItems.value.filter(i => i.category === category)
  for (const item of items) {
    addToAdjust(category, item)
  }
}
```

**Acceptance criteria:**
- Clicking adjust icon on a LABOR row adds that item to right panel (via `adjustedItems`)
- Clicking adjust icon on a PROCUREMENT row adds that item to right panel
- Clicking adjust icon on a 其他科目 sub-category adds that item to right panel
- "全部加入调整" per category adds ALL items of that category (idempotent -- no duplicates)
- Clicking the same adjust icon twice does NOT create duplicates
- For 其他科目, clicking adjust on the same sub-category twice only keeps one item
- Existing right panel still works (still reads `form.items` during this step)

---

### Step 2: Data Model -- Create adjustedItems, Keep form.items Coexisting

**File:** `wh-frontend/src/views/pm/budget/upgrade.vue`

**Why second:** Both data sources (`form.items` and `adjustedItems`) coexist. Right panel still reads from `form.items` (unchanged). Left panel handlers from Step 1 write to `adjustedItems`. This validates the new data structure against real data before any consumer switches over.

**What to do:**

**2a. Define `adjustedItems` reactive and `nextId` counter:**
```js
let nextId = 0
const adjustedItems = reactive({
  LABOR: [],
  PROCUREMENT: [],
  TRAVEL: [],
  BUSINESS: [],
  ENTERTAINMENT: [],
  ACTIVITY: [],
  OTHER: []
})
```

**2b. Create `createAdjustedItem(category, originalItem)`:**
Returns a plain object with:
- `_key: nextId++` -- local unique key for Vue `:key` bindings
- `originalId: originalItem?.id || null` -- maps back to original for merge dedup (may be null for 其他科目 constructed items)
- Category-specific fields copied from `originalItem` (or zeroed for new items):
  - LABOR: `{ roleCode, positionId, hours, costRate, amount }`
  - PROCUREMENT: `{ bomItem, qty, unitPrice, amount, manualAdjustment: 0 }`
  - 其他科目 (TRAVEL/BUSINESS/ENTERTAINMENT/ACTIVITY/OTHER): `{ amount }`

The `_key` field is a Vue-internal key only -- it MUST be excluded from `buildItemsTree()` output (verified in Step 5).

**2c. Update dirty-checking infrastructure:**

Current code (line 539): `watch(form, () => { isDirty.value = hasUnsavedChanges() }, { deep: true })`

After `form` is refactored to only `{ projectId, managementReserve }`, this watch will not detect changes to `adjustedItems`. Replace with:

```js
// Watch managementReserve for changes
watch(() => form.value.managementReserve, () => {
  isDirty.value = hasUnsavedChanges()
})

// Watch adjustedItems for any item changes (deep)
watch(adjustedItems, () => {
  isDirty.value = hasUnsavedChanges()
}, { deep: true })
```

Also add explicit reset in `loadOriginalData()` after successful load:
```js
isDirty.value = false
```

**2d. Update `hasUnsavedChanges()`:**
```js
const hasUnsavedChanges = () => {
  // Check management reserve for changes
  if (Math.abs((form.value.managementReserve || 0) - (parseFloat(originalData.value.budget?.managementReserve) || 0)) > 0.01) return true
  // Check if any items have been added to adjustedItems
  for (const cat of allCategories) {
    if ((adjustedItems[cat.value] || []).length > 0) return true
  }
  return false
}
```
This is intentionally conservative: any non-empty adjustedItems = dirty, even if values haven't changed. The user took explicit action to add items, so treat it as a modification.

**2e. Clear `adjustedItems` on data reload:**

In `loadOriginalData()`, add at the very start (before the API call):
```js
// Reset all adjustedItems arrays to prevent data leak across budget versions
for (const cat of allCategories) {
  adjustedItems[cat.value].length = 0
}
```
This prevents navigating from budget A to budget B from leaking adjusted items.

**2f. Do NOT remove `form.items` yet.** Left panel reads from `originalData`, right panel still reads from `form.items`. `adjustedItems` is populated but not yet consumed by templates. This keeps the page functional while `adjustedItems` is being built up.

**Acceptance criteria:**
- `adjustedItems` exists as a reactive with 7 empty category arrays
- `createAdjustedItem` produces correct objects for all 3 category types (LABOR, PROCUREMENT, 其他科目)
- `createAdjustedItem` always includes `originalId` (from `originalItem.id` or null)
- `hasUnsavedChanges()` returns true when `adjustedItems` non-empty OR `managementReserve` changed
- `hasUnsavedChanges()` returns false when `adjustedItems` empty AND `managementReserve` unchanged
- `isDirty.value = false` is explicitly set in `loadOriginalData()` after successful load
- Two watchers exist: `watch(() => form.value.managementReserve, ...)` + `watch(adjustedItems, ..., { deep: true })`
- `loadOriginalData()` clears all `adjustedItems[category]` arrays at start
- Navigating from budget A to budget B does not leak adjusted items
- Page loads without errors (right panels still show form.items data)

---

### Step 3: Right Panel -- Rewire to adjustedItems (ONE CATEGORY AT A TIME)

**File:** `wh-frontend/src/views/pm/budget/upgrade.vue`

**Why third:** With `adjustedItems` populated by Step 1-2 handlers and the dirty-checking infrastructure in place, this step switches template bindings from `form.items.X` to `adjustedItems.X`. Do it category by category so each switch can be tested independently before moving to the next.

**What to do:**

**3a. LABOR right table** (current line ~159-192):
- Change `:data="form.items.LABOR"` to `:data="adjustedItems.LABOR"`
- Keep: position select, hours input-number, costRate display, amount display.
- **Add** a "+/-" column between "工时" and "成本定额" with two buttons:
  ```html
  <el-table-column label="+/-" width="100">
    <template #default="{ row }">
      <el-button size="small" @click="adjustHours(row, -8)">-8h</el-button>
      <el-button size="small" @click="adjustHours(row, 8)">+8h</el-button>
    </template>
  </el-table-column>
  ```
- The existing "操作" column changes from "删除" (with disabled logic) to "移除" (always enabled):
  ```html
  <el-button link type="danger" size="small" @click="removeFromAdjust('LABOR', $index)">移除</el-button>
  ```
- "+ 添加人员" button: changes `@click="addItem('LABOR')"` to add a new zero-value item to `adjustedItems.LABOR` via `createAdjustedItem('LABOR', null)`.
- Empty state: when `adjustedItems.LABOR.length === 0`, show placeholder text "暂未调整，请点击左侧调整图标添加".

**3b. PROCUREMENT right table** (current line ~231-260):
- Change `:data="form.items.PROCUREMENT"` to `:data="adjustedItems.PROCUREMENT"`
- Add "+/-" column with amount +/-1000 buttons:
  ```html
  <el-button size="small" @click="adjustAmount(row, -1000)">-1000</el-button>
  <el-button size="small" @click="adjustAmount(row, 1000)">+1000</el-button>
  ```
- CRITICAL: Procurement items need a `manualAdjustment` field to avoid conflict with `calcProcurementAmount`.
  - Add `manualAdjustment: 0` to `createAdjustedItem` for PROCUREMENT items.
  - `adjustAmount(row, delta)` modifies `row.manualAdjustment += delta`, then calls `calcProcurementAmount(row)`.
  - `calcProcurementAmount(row)` becomes:
    ```js
    row.amount = (row.qty || 0) * (row.unitPrice || 0) + (row.manualAdjustment || 0)
    ```
  - This ensures `@change` on qty/unitPrice (which calls `calcProcurementAmount`) does not silently wipe the manual +/-1000 adjustment.
- Existing "操作" column: change to "移除" (always enabled).
- "+ 添加BOM项" button: changes to add to `adjustedItems.PROCUREMENT` via `createAdjustedItem('PROCUREMENT', null)`.
- Empty state placeholder: "暂未调整，请点击左侧调整图标添加".

**3c. 其他科目 right panel** (current line ~294-308):
- Change `form.items[cat.value][0].amount` to read from `adjustedItems[cat.value][0]?.amount || 0`.
- Show adjusted sub-categories inline (only those that have been added), each with:
  - Category label
  - Editable amount (input-number)
  - +/-1000 buttons (using `adjustAmount` -- 其他科目 items have `manualAdjustment: 0` but the same pattern applies since `calcProcurementAmount`-equivalent logic does not apply; direct amount modification is fine here)
  - Remove button
- "+" operators between visible categories (like left side).
- Empty state: when no 其他科目 sub-categories are in `adjustedItems`, show placeholder.
- For 其他科目 items, `adjustAmount(row, delta)` directly modifies `row.amount` (no qty/unitPrice recalculation to conflict with).

**3d. Logic -- new helper functions:**
```js
function adjustHours(row, delta) {
  row.hours = Math.max(0, (row.hours || 0) + delta)
  calcLaborAmount(row)
}

function adjustAmount(row, delta) {
  if (row.manualAdjustment !== undefined) {
    // PROCUREMENT: modify manualAdjustment, then recalculate
    row.manualAdjustment += delta
    calcProcurementAmount(row)
  } else {
    // OTHER categories: direct amount modification
    row.amount = Math.max(0, (row.amount || 0) + delta)
  }
}

function removeFromAdjust(category, index) {
  adjustedItems[category].splice(index, 1)
}
```

**Acceptance criteria:**
- Right panels show ONLY items explicitly added via adjust icon or "全部加入调整"
- +/-8h buttons on LABOR rows modify hours and auto-recalculate amount (hours * costRate)
- +/-1000 buttons on PROCUREMENT rows modify `manualAdjustment`, and `calcProcurementAmount` computes `amount = qty * unitPrice + manualAdjustment`
- Changing qty/unitPrice on a PROCUREMENT row does NOT reset the manualAdjustment
- +/-1000 on 其他科目 modifies amount directly
- "移除" button removes the row from adjustedItems (not just hiding)
- "添加人员" creates new LABOR row with all-zero values in adjustedItems
- "添加BOM项" creates new PROCUREMENT row with all-zero values in adjustedItems
- Position selection and rate fetching still work on adjusted LABOR items
- Empty adjustedItems category shows placeholder "暂未调整，请点击左侧调整图标添加"
- Data flows: Left adjust icon -> adjustedItems.LABOR.push(...) -> right table shows new row

---

### Step 4: Computation Layer -- Merge Totals & Submission

**File:** `wh-frontend/src/views/pm/budget/upgrade.vue`

**Why fourth:** With right panels reading `adjustedItems` and left panels reading `originalData`, the computation layer wires everything together: totals, waterfall diff, and `buildItemsTree()` for submission. This is a pure computation step -- it only adds/modifies computed properties and helper functions, no template changes.

**What to do:**

**4a. Create merge helper -- `adjustedOriginalIds`:**
```js
const adjustedOriginalIds = computed(() => {
  const ids = new Set()
  for (const cat of allCategories) {
    for (const item of adjustedItems[cat.value] || []) {
      if (item.originalId) ids.add(item.originalId)
    }
  }
  return ids
})
```
Items with `originalId === null` (newly created items, 其他科目 constructed items) are naturally excluded from the Set, so they don't suppress their corresponding original items in the merge.

**4b. Create `mergedCategoryTotal`:**
```js
// Single-value categories: if adjusted, use adjusted only (skip original)
const singleValueCategories = new Set(otherCategoryList.map(c => c.value))

const mergedCategoryTotal = (category) => {
  // For single-value categories (TRAVEL, BUSINESS, etc.):
  // if user added it to adjust, use adjusted total only → no double count
  if (singleValueCategories.has(category) && adjustedItems[category].length > 0) {
    return adjustedItems[category].reduce((s, i) => s + (i.amount || 0), 0)
  }
  // For multi-value categories (LABOR, PROCUREMENT):
  // merge adjusted items + unadjusted originals by originalId exclusion
  let total = 0
  for (const item of (adjustedItems[category] || [])) {
    total += item.amount || 0
  }
  for (const item of (originalData.value.items || [])) {
    if (item.category === category && !adjustedOriginalIds.value.has(item.id)) {
      total += parseFloat(item.budgetAmount) || 0
    }
  }
  return total
}
```
This avoids the double-counting bug for 其他科目: when `adjustedItems[category].length > 0`, the short-circuit returns adjusted-only total, skipping the original items loop entirely. For LABOR/PROCUREMENT (multi-value), the `originalId`-based exclusion correctly handles partial adjustments.

**4c. Rewire computed properties that currently read `form.items`:**

| Current | New |
|---------|-----|
| `costBaseline` (line 446-454): iterates `form.value.items` | Sum `mergedCategoryTotal()` across all categories |
| `categoryTotal('LABOR')` (line 481-483): reads `form.value.items[category]` | `mergedCategoryTotal('LABOR')` |
| `categoryTotal('PROCUREMENT')` (line 481-483): reads `form.value.items[category]` | `mergedCategoryTotal('PROCUREMENT')` |
| `otherTotal` (line 458-463): reads `form.value.items[cat.value]` | Sum `mergedCategoryTotal()` across `otherCategoryList` |
| `totalBudget` (line 456): `costBaseline + managementReserve` | Same formula, but `costBaseline` now uses merged totals |

**4d. Rewrite `buildItemsTree()`:**
```js
const buildItemsTree = () => {
  const items = []
  for (const cat of allCategories) {
    // Add adjusted items (with their new values)
    for (const item of (adjustedItems[cat.value] || [])) {
      // IMPORTANT: Exclude _key from output -- it is a Vue-internal key only
      items.push({
        // id intentionally omitted: backend createBudgetItemRecursive always calls insert,
        // never reads req.getId(). originalId is preserved for merge dedup only.
        category: cat.value,
        amount: (item.amount || 0).toString(),
        level: 1,
        roleCode: item.roleCode,
        positionId: item.positionId,
        hours: item.hours?.toString(),
        costRate: item.costRate?.toString(),
        bomItem: item.bomItem,
        qty: item.qty?.toString(),
        unitPrice: item.unitPrice?.toString()
      })
    }
    // For single-value categories, skip originals if adjusted items exist
    // Prevents double-counting: same short-circuit as mergedCategoryTotal (Step 4b)
    if (singleValueCategories.has(cat.value) && adjustedItems[cat.value].length > 0) continue
    // Add unadjusted original items (with original values)
    for (const orig of (originalData.value.items || [])) {
      if (orig.category === cat.value && !adjustedOriginalIds.value.has(orig.id)) {
        items.push({
          category: orig.category,
          amount: (orig.budgetAmount || 0).toString(),
          level: 1,
          roleCode: orig.roleCode,
          positionId: orig.positionId,
          hours: orig.hours?.toString(),
          costRate: orig.costRate?.toString(),
          bomItem: orig.bomItem,
          qty: orig.qty?.toString(),
          unitPrice: orig.unitPrice?.toString()
        })
      }
    }
  }
  return items
}
```
> Note: `id` is removed from the output because backend `createBudgetItemRecursive` (WhPmBudgetBo.java:411-455) always calls `insert` and never reads `req.getId()`. Including `id` has no effect on the current backend.

**4e. Update `diffData` computed:**
The `diffData` computed (line 493) already uses `categoryTotal()`, `otherTotal`, `costBaseline`, `totalBudget` -- which in Step 4c are rewired to use merged totals. No further changes needed to `diffData` itself.

**Acceptance criteria:**
- Waterfall diff panel shows correct differences when items are adjusted
- Adding an item to adjust with unchanged values shows zero diff
- Modifying adjusted item values updates waterfall in real-time
- `buildItemsTree()` output includes ALL items: adjusted (new values) + unadjusted originals (original values)
- `buildItemsTree()` output contains NO `_key` fields and NO `id` fields
- `mergedCategoryTotal('LABOR')` = sum of adjusted LABOR items + sum of unadjusted original LABOR items
- Items in `adjustedItems` correctly replace their original counterparts in the merge (no double counting)
- `hasUnsavedChanges()` returns true when adjustedItems non-empty or managementReserve changed

---

### Step 5: Cleanup -- Remove form.items, Polish, Edge Cases

**File:** `wh-frontend/src/views/pm/budget/upgrade.vue`

**Why last:** All consumers have been switched to `adjustedItems`. Now safely remove all `form.items` scaffolding and verify nothing broke. This is a cleanup + verification step.

**What to do:**

**5a. Remove `items` from `form` ref definition:**
```js
const form = ref({
  projectId: '',
  managementReserve: 0
})
```
The old `items` block (LABOR/PROCUREMENT/TRAVEL/BUSINESS/ENTERTAINMENT/ACTIVITY/OTHER) is deleted.

**5b. Remove `items` pre-fill from `loadOriginalData()`:**
Delete the entire block (lines 710-735 in current code) that maps API data into `form.value.items.LABOR`, `form.value.items.PROCUREMENT`, and `form.value.items[cat.value]`. The function now only sets:
- `form.value.projectId`
- `form.value.managementReserve`
- Plus the new `adjustedItems` reset at the start (added in Step 2e)

**5c. Rewrite `addItem()` to operate on adjustedItems:**
```js
const addItem = (category) => {
  const newItem = createAdjustedItem(category, null)
  adjustedItems[category].push(newItem)
}
```

**5d. Rewrite `removeItem()` to operate on adjustedItems:**
```js
const removeItem = (category, index) => {
  adjustedItems[category].splice(index, 1)
}
```
Note: Since "移除" is always enabled (no minimum-row requirement for adjusted items), remove the `length > 1` guard if present.

**5e. Verify no remaining `form.items` references:**
Search entire file for `form.value.items` and `form.items` -- there should be zero matches after cleanup.

**5f. Verify route leave guard with dirty checking:**
- `onBeforeRouteLeave` uses `isDirty` ref (line 566) -- unchanged
- `beforeunload` handler uses `isDirty` ref (line 543) -- unchanged
- `onActivated` checks `isDirty.value` before reloading (line 561) -- unchanged
- The two watchers from Step 2c keep `isDirty` in sync

**5g. Verify `loadOriginalData()` resets dirty and clears adjustedItems on route change:**
The `watch(() => route.params.id, ...)` at line 578 calls `loadOriginalData()` which:
1. Clears all `adjustedItems[cat.value]` at start (Step 2e)
2. Sets `isDirty.value = false` after successful load (Step 2c)

**5h. Add verification step in comments or a quick sanity check:**
- `console.assert(JSON.stringify(buildItemsTree()).indexOf('"_key"') === -1, '_key leaked into buildItemsTree output')`
- Or add a comment instructing manual verification during testing.

**5i. Empty state placeholders (if not added in Step 3):**
Confirm each category panel shows "暂未调整，请点击左侧调整图标添加" when `adjustedItems[category].length === 0`.

**Acceptance criteria:**
- No remaining references to `form.items` in template or script
- `form` ref definition only contains `{ projectId: '', managementReserve: 0 }`
- `addItem` pushes to `adjustedItems[category]` via `createAdjustedItem`
- `removeItem` splices from `adjustedItems[category]`
- `loadOriginalData()` clears adjustedItems at start, sets `isDirty = false` on success
- Route leave guard prompts when adjustedItems non-empty
- `beforeunload` prompts when adjustedItems non-empty
- Navigation between budgets (different route param) resets adjustedItems
- Position rate fetching works on adjusted LABOR items
- Empty adjustedItems shows "暂未调整，请点击左侧调整图标添加" placeholder
- All existing features (management reserve, project info display, submit flow) work correctly
- No console errors
- `buildItemsTree()` output contains zero `_key` fields

---

## Risks and Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Template reactivity breaks when switching from `form.items.X` to `adjustedItems.X` | Medium | High -- broken UI | `adjustedItems` is `reactive()`, supporting deep reactivity same as `form` ref. Test each category panel independently in Step 3. |
| `buildItemsTree()` missing fields causes backend validation error | Low | High -- save fails | Backend `createBudgetItemRecursive` only reads `level`, `category`, `amount`, and category-specific fields. We preserve all these. `id` is intentionally omitted (backend ignores it). |
| "其他科目" layout complexity leads to CSS breakage | Medium | Medium -- ugly UI | Use same `.other-categories-row` flex layout on right side. Test with 0, 1, 3, 5 sub-categories added. |
| Item identity collision (two items with same original id) | Very Low | Low | `adjustedOriginalIds` uses Set -- natural deduplication. |
| Performance with many items (50+ rows in adjustedItems) | Low | Low | Vue 3 computed properties with Set lookups are O(1). Deep watch on `adjustedItems` is the main cost but only fires on user edits. |
| Procurement manualAdjustment lost on qty/unitPrice change | Medium | High | Mitigated: `calcProcurementAmount` uses `qty * unitPrice + manualAdjustment`. `@change` on qty/unitPrice recalculates correctly. |
| 其他科目 merge double-counting when items have no `id` | Medium | High | Mitigated: `addToAdjust` double-checks `adjustedItems[category].length > 0` for single-value categories. `adjustedOriginalIds` uses `originalId` which is null for 其他科目, so the original item is NOT excluded (correct: the adjusted value shows in adjustedItems, the original shows separately). The `mergedCategoryTotal` correctly totals adjusted + unadjusted. The double-counting would occur if the construct had both an entry in `adjustedItems` AND the original being counted. But since 其他科目 sub-categories are single-value, the user can only add one item per category, so the merge correctly shows: 1 adjusted item + 0 unadjusted = the total for that category. Verified: this works because `addToAdjust` prevents adding more than 1 item per 其他科目 sub-category. |

---

## Verification Steps

1. **Smoke test:** Load page, verify left panels show original data, right panels are empty (placeholder shown).
2. **Adjust flow:** Click adjust icon on one LABOR row -- verify it appears in right panel with correct original values.
3. **+/- buttons (LABOR):** Click +8h on a LABOR item -- verify hours increase by 8 and amount recalculates. Click -8h -- decreases.
4. **+/- buttons (PROCUREMENT):** Click +1000 on PROCUREMENT -- amount increases by 1000 (via manualAdjustment). Change qty -- verify amount recalculates to `qty * unitPrice + manualAdjustment` (manualAdjustment preserved).
5. **Procurement +/-1000 persistence:** Add procurement item, click +1000 twice (amount +2000), then change qty. Verify amount = (new qty * unitPrice) + 2000.
6. **Add All:** Click "全部加入调整" on LABOR -- verify ALL original LABOR items appear in right panel.
7. **Remove:** Click "移除" on one adjusted item -- verify it disappears from right panel.
8. **Waterfall:** Modify an adjusted item's value -- verify waterfall diff updates in real-time with correct color (red for increase, green for decrease).
9. **New item:** Click "+ 添加人员" -- verify a zero-value row appears. Select a position -- verify rate auto-fills and amount calculates.
10. **Submit:** Click "升级" -- verify success, redirect to budget list. Refresh and verify data persisted correctly (all items present, adjusted values used).
11. **Leave guard:** Modify adjustedItems, navigate away -- verify confirmation dialog appears. Cancel -- stays on page. Confirm -- navigates.
12. **Data leak:** Navigate from budget A to budget B -- verify `adjustedItems` is empty for budget B (no leak from budget A).
13. **Dirty flag:** Add item, remove it (adjustedItems empty again), navigate away -- verify NO confirmation dialog (isDirty should be false since adjustedItems is empty and managementReserve unchanged).
14. **buildItemsTree verification:** Call `buildItemsTree()` and verify output contains no `_key` fields and no `id` fields.
15. **Regression:** Test management reserve editing, project info display, return button, 其他科目 flow.
16. **其他科目 dedup:** Click adjust on TRAVEL twice -- verify only one item appears in adjustedItems.TRAVEL.

---

## Revision Changelog

### r2 (2026-05-15) -- Architect/Critic Review Fixes

**CRITICAL fixes:**
1. **Dirty checking after refactoring (Fix #1):** Added `watch(adjustedItems, () => { isDirty.value = hasUnsavedChanges() }, { deep: true })` to Step 2c. Changed `watch(form, ...)` to `watch(() => form.value.managementReserve, ...)` since `form` will only have 2 scalar fields. Added explicit `isDirty.value = false` in `loadOriginalData()`.
2. **Data leak across route param changes (Fix #2):** Added `for (const cat of allCategories) { adjustedItems[cat.value].length = 0 }` at the start of `loadOriginalData()` in Step 2e.
3. **其他科目 dedup + merge double-counting (Fix #3):** Updated `addToAdjust` in Step 1d with a dual guard: `originalItem.id` check for LABOR/PROCUREMENT items AND `adjustedItems[category].length > 0` check for single-value 其他科目 sub-categories. Updated `createAdjustedItem` to always include `originalId`. Verified merge logic in Step 4b correctly handles the null-`originalId` case. Added risk row for this scenario.

**MAJOR fixes:**
4. **Procurement +/-1000 design conflict (Fix #4):** Added `manualAdjustment: 0` field to PROCUREMENT items in `createAdjustedItem` (Step 2b). `adjustAmount(row, delta)` now modifies `row.manualAdjustment` instead of `row.amount` directly, then calls `calcProcurementAmount(row)` which becomes `row.amount = qty * unitPrice + manualAdjustment` (Step 3b). This prevents `@change` on qty/unitPrice from silently wiping the +/-1000 adjustment. Added verification step #5 for this specific edge case.
5. **Step ordering (Fix #5):** Reordered from [Data Model, Left Panel, Right Panel, Computation, Cleanup] to [Left Panel, Data Model, Right Panel, Computation, Cleanup]. Step 1 is now safe and additive (no existing bindings changed). Step 2 keeps both `form.items` and `adjustedItems` coexisting. Step 3 switches bindings incrementally. This enables testing after each step.
6. **buildItemsTree id claim incorrect (Fix #6):** Removed `id` field from `buildItemsTree()` output. Added comment that backend `createBudgetItemRecursive` always calls `insert` and never reads `req.getId()`. Original `originalId` field is kept on `adjustedItems` items for merge dedup only, never serialized to the API payload.

**OTHER improvements:**
- Added `isDirty.value = false` after successful `loadOriginalData()` (was missing)
- Added explicit `_key` exclusion verification in Step 5h
- Added empty state placeholder text "暂未调整，请点击左侧调整图标添加" to Step 3a/3b/3c and Step 5i
- Updated `removeItem` to remove the `length > 1` guard (adjusted items don't have a minimum-row requirement)
- Updated `hasUnsavedChanges` to check `adjustedItems` non-empty instead of comparing `costBaseline` (which depends on `form.items`)
- Added regression test step for 其他科目 dedup (verification #16)
- Updated Risk table with two new rows: procurement manualAdjustment conflict and 其他科目 merge double-counting
