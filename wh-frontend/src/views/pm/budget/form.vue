<template>
  <div class="budget-form">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ isEdit ? '编辑预算' : '新增预算' }}</span>
          <el-button @click="$router.back()">返回</el-button>
        </div>
      </template>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <!-- Project Name (searchable dropdown) -->
        <el-form-item label="项目名称" prop="projectId">
          <el-select v-model="form.projectId" filterable placeholder="请输入搜索项目名称" style="width: 100%" :disabled="isEdit || isProjectLocked">
            <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </el-form-item>

        <!-- Budget Categories -->
        <el-divider>预算科目</el-divider>

        <!-- LABOR -->
        <el-card class="category-card">
          <template #header>
            <div class="category-header">
              <span>人工</span>
              <span class="category-amount">合计：¥ {{ formatMoney(categoryTotal('LABOR')) }} 元人民币</span>
            </div>
          </template>
          <el-table :data="form.items.LABOR" style="width: 100%" size="small">
            <el-table-column label="岗位" width="160">
              <template #default="{ row }">
                <el-select v-model="row.positionId" size="small" filterable placeholder="请选择岗位"
                  @change="handlePositionChange(row)">
                  <el-option v-for="p in positionList" :key="p.id" :label="p.name" :value="p.id" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="工时(小时)" width="160">
              <template #default="{ row }">
                <el-input-number v-model="row.hours" :min="0" size="small" @change="calcLaborAmount(row)" />
              </template>
            </el-table-column>
            <el-table-column label="" width="20" align="center">
              <template #default>
                <span class="table-operator">*</span>
              </template>
            </el-table-column>
            <el-table-column label="成本定额(元/时)" width="180">
              <template #default="{ row }">
                <el-input-number v-model="row.costRate" :min="0" :precision="2" size="small" disabled
                  :class="{ 'zero-rate': row.costRate === 0 }"
                />
              </template>
            </el-table-column>
            <el-table-column label="" width="20" align="center">
              <template #default>
                <span class="table-operator">=</span>
              </template>
            </el-table-column>
            <el-table-column label="金额" width="180">
              <template #default="{ row }">¥ {{ formatMoney(row.amount) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="80">
              <template #default="{ $index }">
                <el-button link type="danger" size="small" @click="removeItem('LABOR', $index)"
                  :disabled="form.items.LABOR.length <= 1">删除</el-button>
              </template>
            </el-table-column>
            <el-table-column label="实际已发生成本" width="140">
              <template #default="{ row }">
                <span class="actual-cost-col">{{ formatMoney(row.actualAmount || 0) }}</span>
              </template>
            </el-table-column>
          </el-table>
          <el-button size="small" @click="addItem('LABOR')" style="margin-top: 8px">
            <el-icon><Plus /></el-icon> 添加人员
          </el-button>
        </el-card>

        <!-- PROCUREMENT -->
        <el-card class="category-card">
          <template #header>
            <div class="category-header">
              <span>采购</span>
              <span class="category-amount">合计：¥ {{ formatMoney(categoryTotal('PROCUREMENT')) }} 元人民币</span>
            </div>
          </template>
          <el-table :data="form.items.PROCUREMENT" style="width: 100%" size="small">
            <el-table-column label="BOM项" width="240">
              <template #default="{ row }">
                <el-input v-model="row.bomItem" size="small" placeholder="BOM项名称" />
              </template>
            </el-table-column>
            <el-table-column label="数量" width="140">
              <template #default="{ row }">
                <el-input-number v-model="row.qty" :min="0" size="small" @change="calcProcurementAmount(row)" />
              </template>
            </el-table-column>
            <el-table-column label="" width="20" align="center">
              <template #default>
                <span class="table-operator">*</span>
              </template>
            </el-table-column>
            <el-table-column label="单价" width="160">
              <template #default="{ row }">
                <el-input-number v-model="row.unitPrice" :min="0" :precision="2" size="small" @change="calcProcurementAmount(row)"
                  :formatter="(value) => value.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',')"
                  :parser="(value) => value.replace(/,/g, '')"
                />
              </template>
            </el-table-column>
            <el-table-column label="" width="20" align="center">
              <template #default>
                <span class="table-operator">=</span>
              </template>
            </el-table-column>
            <el-table-column label="金额" width="180">
              <template #default="{ row }">¥ {{ formatMoney(row.amount) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="80">
              <template #default="{ $index }">
                <el-button link type="danger" size="small" @click="removeItem('PROCUREMENT', $index)"
                  :disabled="form.items.PROCUREMENT.length <= 1">删除</el-button>
              </template>
            </el-table-column>
            <el-table-column label="实际已发生成本" width="140">
              <template #default="{ row }">
                <span class="actual-cost-col">{{ formatMoney(row.actualAmount || 0) }}</span>
              </template>
            </el-table-column>
          </el-table>
          <el-button size="small" @click="addItem('PROCUREMENT')" style="margin-top: 8px">
            <el-icon><Plus /></el-icon> 添加BOM项
          </el-button>
        </el-card>

        <!-- Other Categories: TRAVEL, BUSINESS, ENTERTAINMENT, ACTIVITY, OTHER on one row -->
        <el-card class="category-card">
          <template #header>
            <div class="category-header">
              <span>其他科目</span>
              <span class="category-amount">合计: ¥ {{ formatMoney(otherTotal) }} 元人民币</span>
            </div>
          </template>
          <div class="other-categories-row">
            <template v-for="(cat, idx) in otherCategoryList" :key="cat.value">
              <div class="other-cat-item">
                <label class="other-cat-label">{{ cat.label }}
                  <span class="actual-cost-inline">（实际已发生：¥{{ formatMoney(getOtherActualCost(cat.value)) }}）</span>
                </label>
                <el-input-number
                  v-model="form.items[cat.value][0].amount"
                  :min="0"
                  :precision="2"
                  size="small"
                  controls-position="right"
                  :formatter="(value) => value.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',')"
                  :parser="(value) => value.replace(/,/g, '')"
                />
              </div>
              <span v-if="idx < otherCategoryList.length - 1" class="other-plus-operator">+</span>
            </template>
          </div>
        </el-card>

        <!-- Summary: 总预算 = 项目直接预算 + 项目管理预算 -->
        <el-divider>预算汇总</el-divider>
        <div class="budget-summary-row">
          <div class="summary-item">
            <label>总预算</label>
            <div class="summary-value highlight">¥ {{ formatMoney(totalBudget) }} 元人民币</div>
          </div>
          <div class="summary-operator">=</div>
          <div class="summary-item">
            <label>项目直接预算</label>
            <div class="summary-value">¥ {{ formatMoney(costBaseline) }} 元人民币</div>
          </div>
          <div class="summary-operator">+</div>
          <div class="summary-item input-item">
            <label>项目管理预算</label>
            <el-input-number v-model="form.managementReserve" :min="0" :precision="2" @change="calcTotals" class="reserve-input"
              :formatter="(value) => value.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',')"
              :parser="(value) => value.replace(/,/g, '')"
            />
          </div>
        </div>
      </el-form>

      <div class="form-actions">
        <el-button type="primary" @click="handleSubmit" :loading="submitting">保存</el-button>
        <el-button @click="$router.back()">返回</el-button>
      </div>

      <!-- Quota change confirmation dialog -->
      <el-dialog v-model="quotaDialogVisible" title="定额变更确认" width="480px">
        <div style="margin-bottom: 12px;">该岗位的交付成本定额已更新，请选择：</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="原定额(元/时)">{{ quotaDialogOldRate.toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="新定额(元/时)">
            <span :style="{ color: quotaDialogNewRate === 0 ? '#f56c6c' : '#67c23a', fontWeight: 'bold' }">
              {{ quotaDialogNewRate.toFixed(2) }}
            </span>
          </el-descriptions-item>
        </el-descriptions>
        <template #footer>
          <el-button @click="onQuotaDialogConfirm('old')">保留原值</el-button>
          <el-button type="primary" @click="onQuotaDialogConfirm('new')">使用新定额</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onActivated, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getBudgetDetailApi, getBudgetDetailWithItemsApi, createBudgetApi, updateBudgetApi, getBudgetListApi } from '@/api/pm/budget'
import { getCharterListApi } from '@/api/pm/charter'
import { getPositionListApi, getCurrentRateApi } from '@/api/system/costQuota'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const projects = ref([])
const positionList = ref([])

const isEdit = computed(() => !!route.params.id)
const isProjectLocked = computed(() => !!route.query.projectId && !isEdit.value)

// Per-row quota confirmation dialog
const quotaDialogVisible = ref(false)
const quotaDialogNewRate = ref(0)
const quotaDialogOldRate = ref(0)
const quotaDialogCallback = ref(null) // resolve function

const otherCategoryList = [
  { label: '差旅', value: 'TRAVEL' },
  { label: '商务费用', value: 'BUSINESS' },
  { label: '客户招待费', value: 'ENTERTAINMENT' },
  { label: '活动费', value: 'ACTIVITY' },
  { label: '其他', value: 'OTHER' }
]


const form = ref({
  projectId: '',
  managementReserve: 0,
  items: {
    LABOR: [{ roleCode: 'DEV', positionId: '', hours: 0, costRate: 0, amount: 0 }],
    PROCUREMENT: [{ bomItem: '', qty: 0, unitPrice: 0, amount: 0 }],
    TRAVEL: [{ amount: 0 }],
    BUSINESS: [{ amount: 0 }],
    ENTERTAINMENT: [{ amount: 0 }],
    ACTIVITY: [{ amount: 0 }],
    OTHER: [{ amount: 0 }]
  }
})

const rules = {
  projectId: [{ required: true, message: '请选择项目', trigger: 'change' }]
}

const costBaseline = computed(() => {
  let total = 0
  for (const key of Object.keys(form.value.items)) {
    for (const item of form.value.items[key]) {
      total += item.amount || 0
    }
  }
  return total
})

const totalBudget = computed(() => {
  return costBaseline.value + (form.value.managementReserve || 0)
})

const otherTotal = computed(() => {
  let total = 0
  for (const cat of otherCategoryList) {
    total += form.value.items[cat.value]?.[0]?.amount || 0
  }
  return total
})

const formatMoney = (val) => {
  const num = val || 0
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const getOtherActualCost = (category) => {
  const items = form.value.items[category] || []
  return items.reduce((s, i) => s + (i.actualAmount || 0), 0)
}

const categoryTotal = (category) => {
  const items = form.value.items[category] || []
  return items.reduce((sum, item) => sum + (item.amount || 0), 0)
}

const calcLaborAmount = (row) => {
  row.amount = (row.hours || 0) * (row.costRate || 0)
}

const loadPositions = async () => {
  try {
    const res = await getPositionListApi()
    positionList.value = res.data || []
  } catch { /* ignore */ }
}

const handlePositionChange = async (row) => {
  if (!row.positionId) {
    row.roleCode = ''
    row.costRate = 0
    calcLaborAmount(row)
    return
  }

  try {
    const res = await getCurrentRateApi(row.positionId)
    const data = res.data
    let newRate = 0
    if (data && data.costRate !== undefined && data.costRate !== null) {
      newRate = parseFloat(data.costRate)
    }

    // In edit mode, compare with saved rate and show confirmation dialog
    if (isEdit.value && row._savedCostRate !== undefined && newRate !== row._savedCostRate) {
      quotaDialogOldRate.value = row._savedCostRate
      quotaDialogNewRate.value = newRate
      quotaDialogVisible.value = true

      const userChoice = await new Promise(resolve => { quotaDialogCallback.value = resolve })
      if (userChoice === 'new') {
        row.costRate = newRate
        row._savedCostRate = newRate
      }
      // else: keep original (don't change row.costRate)
    } else {
      row.costRate = newRate
    }

    // Update roleCode from position for backward compat
    const pos = positionList.value.find(p => p.id === row.positionId)
    if (pos) {
      row.roleCode = pos.name || ''
    }
    calcLaborAmount(row)
  } catch {
    row.costRate = 0
    calcLaborAmount(row)
  }
}

const onQuotaDialogConfirm = (choice) => {
  quotaDialogVisible.value = false
  if (quotaDialogCallback.value) {
    quotaDialogCallback.value(choice)
    quotaDialogCallback.value = null
  }
}

const calcProcurementAmount = (row) => {
  row.amount = (row.qty || 0) * (row.unitPrice || 0)
}

const calcTotals = () => {
  // Triggered by management reserve change, computed handles rest
}

const addItem = (category) => {
  if (category === 'LABOR') {
    form.value.items.LABOR.push({ roleCode: 'DEV', positionId: '', hours: 0, costRate: 0, amount: 0, actualAmount: 0 })
  } else if (category === 'PROCUREMENT') {
    form.value.items.PROCUREMENT.push({ bomItem: '', qty: 0, unitPrice: 0, amount: 0, actualAmount: 0 })
  }
}

const removeItem = (category, index) => {
  if (form.value.items[category].length > 1) {
    form.value.items[category].splice(index, 1)
  }
}

const resetForm = () => {
  form.value = {
    projectId: '',
    managementReserve: 0,
    items: {
      LABOR: [{ roleCode: 'DEV', positionId: '', hours: 0, costRate: 0, amount: 0 }],
      PROCUREMENT: [{ bomItem: '', qty: 0, unitPrice: 0, amount: 0 }],
      TRAVEL: [{ amount: 0 }],
      BUSINESS: [{ amount: 0 }],
      ENTERTAINMENT: [{ amount: 0 }],
      ACTIVITY: [{ amount: 0 }],
      OTHER: [{ amount: 0 }]
    }
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true

    const data = {
      projectId: form.value.projectId,
      managementReserve: form.value.managementReserve.toString(),
      items: buildItemsTree()
    }

    if (isEdit.value) {
      await updateBudgetApi(route.params.id, data)
    } else {
      await createBudgetApi(data)
    }
    ElMessage.success('保存成功')
    router.push('/pm/budget')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('保存失败')
  } finally {
    submitting.value = false
  }
}

const allCategories = [
  { label: '人工', value: 'LABOR' },
  { label: '采购', value: 'PROCUREMENT' },
  ...otherCategoryList
]

const buildItemsTree = () => {
  const items = []
  for (const cat of allCategories) {
    const catItems = form.value.items[cat.value] || []
    for (const item of catItems) {
      items.push({
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
  }
  return items
}

const loadProjects = async () => {
  try {
    const [charterRes, budgetRes] = await Promise.all([
      getCharterListApi({ pageNum: 1, pageSize: 100 }),
      getBudgetListApi({ pageNum: 1, pageSize: 1000 })
    ])
    const charters = charterRes.data?.records || []
    const budgets = budgetRes.data?.records || []
    // Get project IDs that already have active budgets
    const budgetedProjectIds = new Set(budgets.map(b => b.projectId))
    let filtered = charters.filter(c => !budgetedProjectIds.has(c.id))
    // In edit mode, ensure current project is included even if it has a budget
    if (isEdit.value && route.params.id) {
      try {
        const budgetRes2 = await getBudgetDetailApi(route.params.id)
        const budget = budgetRes2.data
        if (budget && budget.projectId) {
          const exists = filtered.find(p => p.id === budget.projectId)
          if (!exists) {
            const charter = charters.find(c => c.id === budget.projectId)
            if (charter) filtered.unshift(charter)
          }
        }
      } catch { /* ignore */ }
    }
    // When project is locked (create from list row "+"), ensure it's in options
    if (isProjectLocked.value) {
      const lockProjectId = route.query.projectId
      const exists = filtered.find(p => p.id === lockProjectId)
      if (!exists) {
        const charter = charters.find(c => c.id === lockProjectId)
        if (charter) filtered.unshift(charter)
      }
    }
    projects.value = filtered
  } catch (e) {
    // Ignore
  }
}

const loadData = async () => {
  if (!isEdit.value) return
  try {
    const res = await getBudgetDetailWithItemsApi(route.params.id)
    const data = res.data
    if (!data || !data.budget) {
      ElMessage.error('加载预算数据失败')
      return
    }
    form.value.projectId = data.budget.projectId || ''
    form.value.managementReserve = parseFloat(data.budget.managementReserve) || 0
    // Parse budget items into form structure
    if (data.items) {
      const laborItems = data.items.filter(i => i.category === 'LABOR')
      const procurementItems = data.items.filter(i => i.category === 'PROCUREMENT')
      if (laborItems.length > 0) {
        form.value.items.LABOR = laborItems.map(i => ({
          roleCode: i.roleCode || 'DEV',
          positionId: i.positionId || '',
          hours: parseFloat(i.hours) || 0,
          costRate: parseFloat(i.costRate) || 0,
          amount: parseFloat(i.budgetAmount) || 0,
          actualAmount: parseFloat(i.actualAmount) || 0,
          _savedCostRate: parseFloat(i.costRate) || 0
        }))
      }
      if (procurementItems.length > 0) {
        form.value.items.PROCUREMENT = procurementItems.map(i => ({
          bomItem: i.bomItem || '',
          qty: parseFloat(i.qty) || 0,
          unitPrice: parseFloat(i.unitPrice) || 0,
          amount: parseFloat(i.budgetAmount) || 0,
          actualAmount: parseFloat(i.actualAmount) || 0
        }))
      }
      for (const cat of otherCategoryList) {
        const otherItems = data.items.filter(i => i.category === cat.value)
        if (otherItems.length > 0) {
          form.value.items[cat.value][0].amount = parseFloat(otherItems[0].budgetAmount) || 0
          form.value.items[cat.value][0].actualAmount = parseFloat(otherItems[0].actualAmount) || 0
        }
      }
    }
  } catch (e) {
    ElMessage.error('加载预算数据失败')
  }
}

onMounted(async () => {
  await Promise.all([loadProjects(), loadPositions()])
  if (isProjectLocked.value) {
    form.value.projectId = route.query.projectId
  }
  loadData()
})

onActivated(async () => {
  await Promise.all([loadProjects(), loadPositions()])
  if (!isEdit.value) {
    resetForm()
    if (isProjectLocked.value) {
      form.value.projectId = route.query.projectId
    }
  } else {
    loadData()
  }
})

watch(() => route.params.id, (newId) => {
  if (!route.path.startsWith('/pm/budget/form')) return
  if (!newId) {
    resetForm()
    if (isProjectLocked.value) {
      form.value.projectId = route.query.projectId
    }
  } else {
    loadData()
  }
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.category-card {
  margin-bottom: 12px;
}
.category-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.category-amount {
  color: #409eff;
  font-weight: bold;
}
.other-categories-row {
  display: flex;
  gap: 40px;
  flex-wrap: wrap;
}
.other-cat-item {
  width: 140px;
  flex-shrink: 0;
}
.other-cat-label {
  display: block;
  font-size: 13px;
  color: #606266;
  margin-bottom: 4px;
}
.other-cat-amount {
  font-size: 12px;
  color: #409eff;
  text-align: right;
  margin-top: 4px;
}
.other-plus-operator {
  font-size: 18px;
  font-weight: bold;
  color: #909399;
  align-self: flex-end;
  margin-bottom: 8px;
}
.form-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}
.budget-summary-row {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 24px;
  padding: 16px 0;
}
.summary-item {
  text-align: center;
  min-width: 140px;
}
.summary-item.input-item {
  min-width: 200px;
}
.summary-item label {
  display: block;
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
}
.summary-value {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
}
.summary-value.highlight {
  color: #409eff;
}
.summary-operator {
  font-size: 24px;
  font-weight: bold;
  color: #909399;
  margin-bottom: 4px;
}
.reserve-input {
  width: 100%;
}
.table-operator {
  font-weight: bold;
  color: #909399;
  font-size: 16px;
}
.actual-cost-col {
  color: #67C23A;
  font-weight: bold;
}

.actual-cost-inline {
  color: #67C23A;
  font-size: 12px;
  font-weight: normal;
}

:deep(.zero-rate .el-input__inner) {
  color: #f56c6c;
  font-weight: bold;
}
</style>
