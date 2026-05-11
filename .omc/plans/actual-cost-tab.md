# RALPLAN-DR: 项目实际成本Tab

## Metadata
- Plan ID: actual-cost-tab-20260511
- Spec: `.omc/specs/deep-interview-actual-cost-tab-20260511.md`
- Ambiguity: 8.3%
- Mode: SHORT (standard consensus)
- Generated: 2026-05-11

## RALPLAN-DR Summary

### Principles
1. **最小侵入** — 严格遵循 detail.vue 现有 lazy-load 模式（loadedTabs + tabLoading），不重构现有代码
2. **职责分离** — 分类映射（LABOR→人工 等）在前端执行，聚合计算在后端
3. **交互一致性** — 图表点击 + 列表过滤使用统一状态（yearMonth + checkedCategories），AND 逻辑
4. **渐进扩展** — 扩展现有接口参数（向后兼容），新增独立聚合接口

### Decision Drivers
| # | 驱动因素 | 影响 |
|---|---------|------|
| 1 | 后端聚合 vs 前端聚合 | 数据量不可控，后端聚合避免传输全量数据 |
| 2 | 分类映射位置 | 前端映射保持后端通用性 |
| 3 | 内联 vs 独立组件 | 与现有 workHours tab 模式一致 |

### Viable Options
**方案 A: 全部内联到 detail.vue（采用）**
- 遵循现有 workHours tab 模式，改动集中
- 无需新建文件、props/emits
- 状态管理在同一 setup 中，逻辑内聚

**方案 B: 独立组件 ActualCostTab.vue（排除）**
- 与现有模式不一致（workHours tab 完全内联）
- 增加 props/events 传递复杂度

**方案 C: 混合模式（排除）**
- 过度设计，仅一处使用

## ADR (Architecture Decision Record)

**Decision:** 全部内联到 `detail.vue`，后端新增聚合接口 + 扩展现有 list 接口

**Drivers:**
- 现有 5 个 tab 全部内联在 detail.vue（包括 ECharts 图表）
- workHours tab 已有完整的 lazy-load + ECharts + chartInstance/dispose 模式可复用
- 分类映射在前端保持后端接口通用

**Alternatives considered:**
- 独立组件：与现有模式不一致，排除
- 前端聚合：数据量大时性能差，排除

**Why chosen:**
- 唯一与现有架构一致的方案
- 所有现有 tab 都没有独立组件文件
- ECharts 初始化和销毁逻辑可直接复制 workHours tab 模式

**Consequences:**
- detail.vue 增长约 200 行
- 未来如需独立页面需重构（概率低，此 tab 仅项目详情页使用）

## Implementation Plan

### Step 1: Backend — 扩展实际成本列表接口

**文件:** `wh-backend/src/main/java/com/wh/controller/pm/WhPmActualCostController.java`

在现有 `list()` 方法新增参数：
```java
@RequestParam(required = false) String costTypes,  // 逗号分隔: "LABOR,PROCUREMENT"
@RequestParam(required = false) String yearMonth    // 如 "2026-05"
```

**文件:** `wh-backend/src/main/java/com/wh/bo/pm/WhPmActualCostBo.java`

在 `pageList()` 方法新增过滤逻辑：
- `costTypes` → 按逗号 split → `wrapper.in(WhPmActualCost::getCostType, list)`
- `yearMonth` → `wrapper.likeRight(WhPmActualCost::getCostDate, yearMonth)`

**Acceptance:** 调用 `GET /api/pm/actual-costs?projectId=xxx&costTypes=LABOR&yearMonth=2026-05&pageNum=1&pageSize=100` 返回正确过滤的分页结果

### Step 2: Backend — 新增汇总金额接口

**文件:** `wh-backend/src/main/java/com/wh/dao/pm/WhPmActualCostDao.java`

新增按条件汇总的查询方法（用于复选框下汇总金额，需汇总全量数据而非当前页）：
```java
@Select("<script>" +
    "SELECT COALESCE(SUM(CAST(AMOUNT AS REAL)), 0) FROM pm_actual_cost " +
    "WHERE PROJECT_ID = #{projectId} AND DEL_FLAG = '0' " +
    "<if test='yearMonth != null and yearMonth != \"\"'>AND COST_DATE LIKE #{yearMonth} || '%' </if>" +
    "<if test='costTypes != null and costTypes.length > 0'>AND COST_TYPE IN " +
    "<foreach collection='costTypes' item='ct' open='(' separator=',' close=')'>#{ct}</foreach></if>" +
    "</script>")
Double sumAmountByFilter(@Param("projectId") String projectId,
                          @Param("yearMonth") String yearMonth,
                          @Param("costTypes") String[] costTypes);
```

**文件:** `wh-backend/src/main/java/com/wh/bo/pm/WhPmActualCostBo.java`

新增方法：
```java
public Double getSumByFilter(String projectId, String yearMonth, String costTypes) {
    String[] types = (costTypes != null && !costTypes.isEmpty()) ? costTypes.split(",") : null;
    return actualCostDao.sumAmountByFilter(projectId, yearMonth, types);
}
```

**文件:** `wh-backend/src/main/java/com/wh/controller/pm/WhPmActualCostController.java`

新增端点：
```java
@GetMapping("/sum")
public R<Double> sum(@RequestParam String projectId,
                     @RequestParam(required = false) String yearMonth,
                     @RequestParam(required = false) String costTypes) {
    return R.ok(actualCostBo.getSumByFilter(projectId, yearMonth, costTypes));
}
```

**Acceptance:** 调用 `GET /api/pm/actual-costs/sum?projectId=xxx&costTypes=LABOR&yearMonth=2026-05` 返回正确的汇总金额

### Step 3: Backend — 新增按月聚合接口

**文件:** `wh-backend/src/main/java/com/wh/dao/pm/WhPmActualCostDao.java`

新增查询方法：
```java
@Select("SELECT SUBSTR(COST_DATE, 1, 7) AS yearMonth, " +
    "COALESCE(SUM(CASE WHEN COST_TYPE = 'LABOR' THEN CAST(AMOUNT AS REAL) ELSE 0 END), 0) AS laborAmount, " +
    "COALESCE(SUM(CASE WHEN COST_TYPE = 'PROCUREMENT' THEN CAST(AMOUNT AS REAL) ELSE 0 END), 0) AS procurementAmount, " +
    "COALESCE(SUM(CASE WHEN COST_TYPE NOT IN ('LABOR', 'PROCUREMENT') THEN CAST(AMOUNT AS REAL) ELSE 0 END), 0) AS otherAmount " +
    "FROM pm_actual_cost WHERE PROJECT_ID = #{projectId} AND DEL_FLAG = '0' " +
    "GROUP BY SUBSTR(COST_DATE, 1, 7) ORDER BY yearMonth")
List<Map<String, Object>> aggregateMonthlyByProject(@Param("projectId") String projectId);
```

**文件:** `wh-backend/src/main/java/com/wh/bo/pm/WhPmActualCostBo.java`

新增方法：
```java
public List<Map<String, Object>> getMonthlyAggregation(String projectId)
```

**文件:** `wh-backend/src/main/java/com/wh/controller/pm/WhPmActualCostController.java`

新增端点：
```java
@GetMapping("/aggregation")
public R<List<Map<String, Object>>> aggregation(@RequestParam String projectId)
```

**Acceptance:** 调用 `GET /api/pm/actual-costs/aggregation?projectId=xxx` 返回 `[{yearMonth: "2026-01", laborAmount: 10000, procurementAmount: 5000, otherAmount: 3000}, ...]`

### Step 4: Frontend — 扩展 API 文件

**文件:** `wh-frontend/src/api/pm/actualCost.js`

`getActualCostListApi` 已存在（`actualCost.js:3`），只需新增聚合和汇总两个方法：
```javascript
// 获取月度聚合数据（图表）
export function getActualCostAggregationApi(projectId) {
  return request({ url: '/pm/actual-costs/aggregation', method: 'get', params: { projectId } })
}

// 获取按条件汇总金额（复选框下方）
export function getActualCostSumApi(params) {
  return request({ url: '/pm/actual-costs/sum', method: 'get', params })
}
```

**Acceptance:** API 调用正确，返回格式与后端一致

### Step 5: Frontend — 添加实际成本Tab到 detail.vue

**文件:** `wh-frontend/src/views/pm/charter/detail.vue`

#### 4a: 模板 — 新增 tab-pane（在 budget tab 后）
```html
<el-tab-pane label="项目实际成本" name="actualCost">
  <div v-loading="tabLoading.actualCost">
    <!-- 堆叠柱状图 -->
    <div ref="actualCostChartRef" style="width: 100%; height: 400px;"></div>
    
    <!-- 分类过滤器 -->
    <div style="margin: 16px 0;">
      <el-checkbox-group v-model="checkedCategories" @change="onCategoryChange">
        <el-checkbox label="all">全部</el-checkbox>
        <el-checkbox label="LABOR">人工</el-checkbox>
        <el-checkbox label="PROCUREMENT">采购</el-checkbox>
        <el-checkbox label="OTHER">其他</el-checkbox>
      </el-checkbox-group>
      <div style="margin-top: 8px; font-weight: bold;">
        汇总：¥ {{ formatMoney(currentSummaryAmount) }}
      </div>
    </div>
    
    <!-- 实际成本列表 -->
    <el-table :data="actualCostList" stripe size="small">
      <el-table-column prop="costDate" label="成本日期" width="120" />
      <el-table-column label="成本分类" width="100">
        <template #default="{ row }">{{ comparisonCategoryLabels[row.costType] || row.costType }}</template>
      </el-table-column>
      <el-table-column label="金额" width="140" align="right">
        <template #default="{ row }">¥ {{ formatMoney(row.amount) }}</template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="200" />
    </el-table>
    
    <el-pagination
      v-model:current-page="actualCostPage.pageNum"
      v-model:page-size="actualCostPage.pageSize"
      :page-sizes="[100]"
      :total="actualCostPage.total"
      layout="total, prev, pager, next"
      @current-change="loadActualCostList"
    />
  </div>
</el-tab-pane>
```

#### 4b: Script — 新增状态变量
```javascript
const actualCostChartRef = ref(null)
let actualCostChartInstance = null
let actualCostResizeHandler = null

const checkedCategories = ref(['all'])
const yearMonthFilter = ref(null)  // null = 全部月份
const currentSummaryAmount = ref(0)

const actualCostAggregation = ref([])  // 图表数据
const actualCostList = ref([])         // 列表数据
const actualCostPage = reactive({ pageNum: 1, pageSize: 100, total: 0 })
```

#### 4c: Script — 新增加载逻辑
```javascript
const loadActualCost = async () => {
  if (loadedTabs.actualCost) return
  tabLoading.actualCost = true
  try {
    const aggRes = await getActualCostAggregationApi(route.params.id)
    actualCostAggregation.value = aggRes.data || []
    await Promise.all([loadActualCostList(), loadActualCostSum()])
    loadedTabs.actualCost = true
    await nextTick()
    renderActualCostChart()
  } catch { ElMessage.error('加载实际成本失败') }
  finally { tabLoading.actualCost = false }
}

const loadActualCostList = async () => {
  const costTypes = checkedCategories.value.includes('all')
    ? null
    : expandCategoryFilter(checkedCategories.value)
  const res = await getActualCostListApi({
    projectId: route.params.id,
    costTypes: costTypes,
    yearMonth: yearMonthFilter.value,
    pageNum: actualCostPage.pageNum,
    pageSize: actualCostPage.pageSize
  })
  actualCostList.value = res.data?.records || []
  actualCostPage.total = res.data?.total || 0
}

const loadActualCostSum = async () => {
  // 通过后端 SUM 接口获取全量汇总（不限于当前页）
  const costTypes = checkedCategories.value.includes('all')
    ? null
    : expandCategoryFilter(checkedCategories.value)
  const res = await getActualCostSumApi({
    projectId: route.params.id,
    costTypes: costTypes,
    yearMonth: yearMonthFilter.value
  })
  currentSummaryAmount.value = res.data || 0
}
```

#### 4d: Script — 分类映射工具（复用已有 `comparisonCategoryLabels`）
```javascript
// 表格中成本分类列直接使用已有的 comparisonCategoryLabels 对象
// 不需要新增 categoryLabel() 函数 — 使用 comparisonCategoryLabels[row.costType] || row.costType

const expandCategoryFilter = (checked) => {
  // 将前端复选框值展开为后端 costType 列表
  const mapping = {
    LABOR: ['LABOR'],
    PROCUREMENT: ['PROCUREMENT'],
    OTHER: ['TRAVEL', 'BUSINESS', 'ENTERTAINMENT', 'ACTIVITY', 'OTHER']
  }
  return checked.flatMap(c => mapping[c] || []).join(',')
}
```

#### 4e: Script — ECharts 图表渲染
```javascript
const renderActualCostChart = () => {
  if (!actualCostChartRef.value) return
  if (actualCostChartInstance) actualCostChartInstance.dispose()
  actualCostChartInstance = echarts.init(actualCostChartRef.value)
  const data = actualCostAggregation.value
  
  // 计算总成本
  const totalCost = data.reduce((s, d) => s + (d.laborAmount || 0) + (d.procurementAmount || 0) + (d.otherAmount || 0), 0)
  
  actualCostChartInstance.setOption({
    title: {
      text: `¥ ${totalCost.toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`,
      left: 'center',
      top: 0,
      triggerEvent: true,
      textStyle: { fontSize: 14, fontWeight: 'bold', color: '#409eff' }
    },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['人工', '采购', '其他'], top: 0, right: 0 },
    grid: { left: 60, right: 40, top: 40, bottom: 40 },
    xAxis: { type: 'category', data: data.map(d => d.yearMonth), axisLabel: { rotate: 45 } },
    yAxis: { type: 'value', name: '元' },
    series: [
      { name: '人工', type: 'bar', stack: 'total', data: data.map(d => d.laborAmount || 0), itemStyle: { color: '#409eff' } },
      { name: '采购', type: 'bar', stack: 'total', data: data.map(d => d.procurementAmount || 0), itemStyle: { color: '#67c23a' } },
      { name: '其他', type: 'bar', stack: 'total', data: data.map(d => d.otherAmount || 0), itemStyle: { color: '#e6a23c' } }
    ]
  })
  
  // 点击柱子事件
  actualCostChartInstance.on('click', (params) => {
    if (params.componentType === 'series') {
      yearMonthFilter.value = params.name  // 如 "2026-05"
      actualCostPage.pageNum = 1
      loadActualCostList()
      loadActualCostSum()
    }
  })
  
  // 点击标题事件 → 清除年月过滤
  actualCostChartInstance.on('click', (params) => {
    if (params.componentType === 'title') {
      yearMonthFilter.value = null
      actualCostPage.pageNum = 1
      loadActualCostList()
      loadActualCostSum()
    }
  })
  
  actualCostResizeHandler = () => actualCostChartInstance?.resize()
  window.addEventListener('resize', actualCostResizeHandler)
}
```

#### 4f: Script — 复选框变更（互斥逻辑）
```javascript
// 跟踪上一次的选中值，用于检测"全部"↔单项切换方向
const prevCheckedCategories = ref(['all'])

const onCategoryChange = (val) => {
  const hadAll = prevCheckedCategories.value.includes('all')
  const hasAll = val.includes('all')

  if (hasAll && !hadAll) {
    // 刚选了"全部" → 只保留"all"
    checkedCategories.value = ['all']
  } else if (hadAll && hasAll && val.length > 1) {
    // "全部"仍在 + 新增了具体项 → 移除"all"
    checkedCategories.value = val.filter(v => v !== 'all')
  } else if (!hasAll && val.length === 0) {
    // 全部取消 → 默认回退"全部"
    checkedCategories.value = ['all']
  }

  prevCheckedCategories.value = [...checkedCategories.value]
  actualCostPage.pageNum = 1
  loadActualCostList()
  loadActualCostSum()
}
```

#### 4g: Script — 扩展 loadedTabs / tabLoading / handleTabClick / watch
```javascript
// loadedTabs 新增
const loadedTabs = reactive({
  tasks: false, budget: false, workHours: false, deliverables: false,
  actualCost: false
})

// tabLoading 新增
const tabLoading = reactive({
  tasks: false, budget: false, workHours: false, deliverables: false,
  actualCost: false
})

// handleTabClick 新增映射 + resize
const loaders = {
  tasks: loadWbs, budget: loadBudget, workHours: loadWorkHours,
  deliverables: loadDeliverables, actualCost: loadActualCost
}
// 在 handleTabClick 函数内现有 workHours resize 后新增:
// if (paneName === 'actualCost') { await nextTick(); actualCostChartInstance?.resize() }

// watch 重置逻辑新增 — 重置所有实际成本tab状态
actualCostAggregation.value = []
actualCostList.value = []
currentSummaryAmount.value = 0
yearMonthFilter.value = null
checkedCategories.value = ['all']
prevCheckedCategories.value = ['all']
actualCostPage.pageNum = 1
actualCostPage.total = 0
if (actualCostChartInstance) { actualCostChartInstance.dispose(); actualCostChartInstance = null }
if (actualCostResizeHandler) { window.removeEventListener('resize', actualCostResizeHandler); actualCostResizeHandler = null }
```

#### 4h: Script — 导入新增
```javascript
import { getActualCostListApi, getActualCostAggregationApi, getActualCostSumApi } from '@/api/pm/actualCost'
```

#### 4i: Script — onBeforeUnmount 清理
```javascript
if (actualCostChartInstance) { actualCostChartInstance.dispose(); actualCostChartInstance = null }
if (actualCostResizeHandler) { window.removeEventListener('resize', actualCostResizeHandler); actualCostResizeHandler = null }
```

**Acceptance:** 打开项目详情 → 点击实际成本tab → 图表渲染 + 列表加载 + 复选框可用 + 分页正常

### Step 6: 集成验证

- 启动后端，验证两个 API 端点正常工作
- 启动前端，验证完整交互流程（chart → filter → checkbox → pagination）
- 切换项目验证状态重置

## Success Criteria (from Spec)
- [ ] 项目详情页「项目预算」tab后出现「项目实际成本」tab
- [ ] 点击tab后：堆叠柱状图显示，标题显示汇总金额
- [ ] 复选框默认全部选中，单行显示汇总金额
- [ ] 列表展示成本日期、成本分类、金额、描述，100行/页
- [ ] 点击柱子 → 过滤该月数据
- [ ] 点击标题 → 恢复全部数据
- [ ] 复选框切换 → 列表过滤 + 汇总更新
- [ ] 年月过滤 + 分类过滤 AND 逻辑
- [ ] 切换项目正确重置
