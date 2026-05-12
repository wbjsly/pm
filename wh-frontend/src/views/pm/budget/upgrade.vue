<template>
  <div class="budget-upgrade">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>升级预算</span>
          <el-button @click="$router.back()">返回</el-button>
        </div>
      </template>

      <div class="split-panels">
        <!-- LEFT: readonly view -->
        <div class="left-panel">
          <div class="panel-title">被升级预算（{{ originalVersion }}）</div>
          <el-card shadow="never" class="panel-card">
            <div class="info-section">
              <el-row :gutter="16">
                <el-col :span="12">
                  <div class="info-item">
                    <label>项目名称</label>
                    <span class="info-value">{{ projectName }}</span>
                  </div>
                </el-col>
                <el-col :span="12">
                  <div class="info-item">
                    <label>预算编码</label>
                    <span class="info-value">{{ originalData.budget?.budgetCode || '' }}</span>
                  </div>
                </el-col>
              </el-row>
              <el-row :gutter="16">
                <el-col :span="12">
                  <div class="info-item">
                    <label>版本号</label>
                    <span class="info-value">{{ originalData.budget?.version || '' }}</span>
                  </div>
                </el-col>
                <el-col :span="12">
                  <div class="info-item">
                    <label>状态</label>
                    <el-tag :type="dictStore.getTagType('BUDGET_STATUS', originalData.budget?.status)">
                      {{ dictStore.getLabel('BUDGET_STATUS', originalData.budget?.status) }}
                    </el-tag>
                  </div>
                </el-col>
              </el-row>
            </div>

            <el-divider>预算科目</el-divider>

            <el-card class="category-card" shadow="never">
              <template #header>
                <div class="category-header">
                  <span>人工</span>
                  <span class="category-amount">合计：¥ {{ formatMoney(leftLaborTotal) }} 元人民币</span>
                </div>
              </template>
              <el-table :data="leftLaborItems" size="small">
                <el-table-column label="岗位" width="140">
                  <template #default="{ row }">
                    <span>{{ row.positionName || roleLabel(row.roleCode) }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="工时(小时)" width="140">
                  <template #default="{ row }">{{ row.hours }}</template>
                </el-table-column>
                <el-table-column label="成本定额(元/时)" width="160">
                  <template #default="{ row }">{{ formatMoney(row.costRate) }}</template>
                </el-table-column>
                <el-table-column label="金额" width="160">
                  <template #default="{ row }">¥ {{ formatMoney(row.budgetAmount) }}</template>
                </el-table-column>
              </el-table>
            </el-card>

            <el-card class="category-card" shadow="never">
              <template #header>
                <div class="category-header">
                  <span>采购</span>
                  <span class="category-amount">合计：¥ {{ formatMoney(leftProcurementTotal) }} 元人民币</span>
                </div>
              </template>
              <el-table :data="leftProcurementItems" size="small">
                <el-table-column label="BOM项" width="200">
                  <template #default="{ row }">{{ row.bomItem }}</template>
                </el-table-column>
                <el-table-column label="数量" width="120">
                  <template #default="{ row }">{{ row.qty }}</template>
                </el-table-column>
                <el-table-column label="单价" width="140">
                  <template #default="{ row }">{{ formatMoney(row.unitPrice) }}</template>
                </el-table-column>
                <el-table-column label="金额" width="160">
                  <template #default="{ row }">¥ {{ formatMoney(row.budgetAmount) }}</template>
                </el-table-column>
              </el-table>
            </el-card>

            <el-card class="category-card" shadow="never">
              <template #header>
                <div class="category-header">
                  <span>其他科目</span>
                  <span class="category-amount">合计：¥ {{ formatMoney(leftOtherTotal) }} 元人民币</span>
                </div>
              </template>
              <div class="other-categories-row">
                <template v-for="(cat, idx) in otherCategoryList" :key="cat.value">
                  <div class="other-cat-item">
                    <label class="other-cat-label">{{ cat.label }}</label>
                    <span class="other-cat-value">¥ {{ formatMoney(getLeftOtherAmount(cat.value)) }}</span>
                  </div>
                  <span v-if="idx < otherCategoryList.length - 1" class="other-plus-operator">+</span>
                </template>
              </div>
            </el-card>

            <el-divider>预算汇总</el-divider>
            <div class="budget-summary-row">
              <div class="summary-item">
                <label>总预算</label>
                <div class="summary-value highlight">¥ {{ formatMoney(leftTotalBudget) }} 元人民币</div>
              </div>
              <div class="summary-operator">=</div>
              <div class="summary-item">
                <label>项目直接预算</label>
                <div class="summary-value">¥ {{ formatMoney(leftCostBaseline) }} 元人民币</div>
              </div>
              <div class="summary-operator">+</div>
              <div class="summary-item">
                <label>项目管理预算</label>
                <div class="summary-value">¥ {{ formatMoney(originalData.budget?.managementReserve) }} 元人民币</div>
              </div>
            </div>
          </el-card>
        </div>

        <!-- RIGHT: editable form -->
        <div class="right-panel">
          <div class="panel-title">升级编辑</div>
          <el-card shadow="never" class="panel-card">
            <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
              <el-form-item label="项目名称" prop="projectId">
                <el-select v-model="form.projectId" filterable placeholder="请输入搜索项目名称" style="width: 100%" disabled>
                  <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
                </el-select>
              </el-form-item>

              <el-divider>预算科目</el-divider>

              <el-card class="category-card" shadow="never">
                <template #header>
                  <div class="category-header">
                    <span>人工</span>
                    <span class="category-amount">合计：¥ {{ formatMoney(categoryTotal('LABOR')) }} 元人民币</span>
                  </div>
                </template>
                <el-table :data="form.items.LABOR" size="small">
                  <el-table-column label="岗位" width="140">
                    <template #default="{ row }">
                      <el-select v-model="row.positionId" size="small" filterable placeholder="请选择岗位"
                        @change="handlePositionChange(row)">
                        <el-option v-for="p in positionList" :key="p.id" :label="p.name" :value="p.id" />
                      </el-select>
                    </template>
                  </el-table-column>
                  <el-table-column label="工时(小时)" width="140">
                    <template #default="{ row }">
                      <el-input-number v-model="row.hours" :min="0" size="small" @change="calcLaborAmount(row)" />
                    </template>
                  </el-table-column>
                  <el-table-column label="成本定额(元/时)" width="160">
                    <template #default="{ row }">
                      <el-input-number v-model="row.costRate" :min="0" :precision="2" size="small" disabled
                        :class="{ 'zero-rate': row.costRate === 0 }" />
                    </template>
                  </el-table-column>
                  <el-table-column label="金额" width="160">
                    <template #default="{ row }">¥ {{ formatMoney(row.amount) }}</template>
                  </el-table-column>
                  <el-table-column label="操作" width="80">
                    <template #default="{ $index }">
                      <el-button link type="danger" size="small" @click="removeItem('LABOR', $index)"
                        :disabled="form.items.LABOR.length <= 1">删除</el-button>
                    </template>
                  </el-table-column>
                </el-table>
                <el-button size="small" @click="addItem('LABOR')" style="margin-top: 8px">
                  <el-icon><Plus /></el-icon> 添加人员
                </el-button>
              </el-card>

              <el-card class="category-card" shadow="never">
                <template #header>
                  <div class="category-header">
                    <span>采购</span>
                    <span class="category-amount">合计：¥ {{ formatMoney(categoryTotal('PROCUREMENT')) }} 元人民币</span>
                  </div>
                </template>
                <el-table :data="form.items.PROCUREMENT" size="small">
                  <el-table-column label="BOM项" width="200">
                    <template #default="{ row }">
                      <el-input v-model="row.bomItem" size="small" placeholder="BOM项名称" />
                    </template>
                  </el-table-column>
                  <el-table-column label="数量" width="120">
                    <template #default="{ row }">
                      <el-input-number v-model="row.qty" :min="0" size="small" @change="calcProcurementAmount(row)" />
                    </template>
                  </el-table-column>
                  <el-table-column label="单价" width="140">
                    <template #default="{ row }">
                      <el-input-number v-model="row.unitPrice" :min="0" :precision="2" size="small" @change="calcProcurementAmount(row)" />
                    </template>
                  </el-table-column>
                  <el-table-column label="金额" width="160">
                    <template #default="{ row }">¥ {{ formatMoney(row.amount) }}</template>
                  </el-table-column>
                  <el-table-column label="操作" width="80">
                    <template #default="{ $index }">
                      <el-button link type="danger" size="small" @click="removeItem('PROCUREMENT', $index)"
                        :disabled="form.items.PROCUREMENT.length <= 1">删除</el-button>
                    </template>
                  </el-table-column>
                </el-table>
                <el-button size="small" @click="addItem('PROCUREMENT')" style="margin-top: 8px">
                  <el-icon><Plus /></el-icon> 添加BOM项
                </el-button>
              </el-card>

              <el-card class="category-card" shadow="never">
                <template #header>
                  <div class="category-header">
                    <span>其他科目</span>
                    <span class="category-amount">合计: ¥ {{ formatMoney(otherTotal) }} 元人民币</span>
                  </div>
                </template>
                <div class="other-categories-row">
                  <template v-for="(cat, idx) in otherCategoryList" :key="cat.value">
                    <div class="other-cat-item">
                      <label class="other-cat-label">{{ cat.label }}</label>
                      <el-input-number
                        v-model="form.items[cat.value][0].amount"
                        :min="0"
                        :precision="2"
                        size="small"
                        controls-position="right"
                      />
                    </div>
                    <span v-if="idx < otherCategoryList.length - 1" class="other-plus-operator">+</span>
                  </template>
                </div>
              </el-card>

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
                  <el-input-number v-model="form.managementReserve" :min="0" :precision="2" @change="calcTotals" class="reserve-input" />
                </div>
              </div>
            </el-form>

            <div class="form-actions">
              <el-button type="primary" @click="handleUpgrade" :loading="submitting">升级</el-button>
              <el-button @click="$router.back()">返回</el-button>
            </div>
          </el-card>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onActivated, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getBudgetDetailWithItemsApi, upgradeBudgetApi } from '@/api/pm/budget'
import { getCharterListApi } from '@/api/pm/charter'
import { getPositionListApi, getCurrentRateApi } from '@/api/system/costQuota'

import { useDictStore } from '@/store/dict'

const route = useRoute()
const router = useRouter()
const dictStore = useDictStore()
const formRef = ref(null)
const submitting = ref(false)
const projects = ref([])
const positionList = ref([])

const originalData = ref({ budget: {}, items: [] })

const originalVersion = computed(() => originalData.value.budget?.version || '')

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

const allItems = computed(() => originalData.value.items || [])

const leftLaborItems = computed(() => allItems.value.filter(i => i.category === 'LABOR'))
const leftProcurementItems = computed(() => allItems.value.filter(i => i.category === 'PROCUREMENT'))

const leftLaborTotal = computed(() => leftLaborItems.value.reduce((s, i) => s + (i.budgetAmount || 0), 0))
const leftProcurementTotal = computed(() => leftProcurementItems.value.reduce((s, i) => s + (i.budgetAmount || 0), 0))
const leftOtherTotal = computed(() => {
  let total = 0
  for (const cat of otherCategoryList) {
    total += allItems.value.filter(i => i.category === cat.value).reduce((s, i) => s + (i.budgetAmount || 0), 0)
  }
  return total
})
const leftCostBaseline = computed(() => allItems.value.reduce((s, i) => s + (i.budgetAmount || 0), 0))
const leftTotalBudget = computed(() => leftCostBaseline.value + (parseFloat(originalData.value.budget?.managementReserve) || 0))

const projectName = computed(() => {
  const projectId = originalData.value.budget?.projectId
  const p = projects.value.find(x => x.id === projectId)
  return p?.projectName || ''
})

const costBaseline = computed(() => {
  let total = 0
  for (const key of Object.keys(form.value.items)) {
    for (const item of form.value.items[key]) {
      total += item.amount || 0
    }
  }
  return total
})

const totalBudget = computed(() => costBaseline.value + (form.value.managementReserve || 0))

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

const getLeftOtherAmount = (category) => {
  const items = allItems.value.filter(i => i.category === category)
  return items.reduce((s, i) => s + (i.budgetAmount || 0), 0)
}

const roleLabel = (code) => {
  const map = { DEV: '开发(DEV)', QA: '测试(QA)', BA: '产品经理(BA)', ARCH: '架构师(ARCH)', PM: '项目经理(PM)' }
  return map[code] || code
}

const categoryTotal = (category) => {
  const items = form.value.items[category] || []
  return items.reduce((sum, item) => sum + (item.amount || 0), 0)
}

const calcLaborAmount = (row) => {
  row.amount = (row.hours || 0) * (row.costRate || 0)
}

const calcProcurementAmount = (row) => {
  row.amount = (row.qty || 0) * (row.unitPrice || 0)
}

const calcTotals = () => {}

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
    const newRate = (data && data.costRate !== undefined && data.costRate !== null)
      ? parseFloat(data.costRate) : 0
    row.costRate = newRate

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

const addItem = (category) => {
  if (category === 'LABOR') {
    form.value.items.LABOR.push({ roleCode: 'DEV', positionId: '', hours: 0, costRate: 0, amount: 0 })
  } else if (category === 'PROCUREMENT') {
    form.value.items.PROCUREMENT.push({ bomItem: '', qty: 0, unitPrice: 0, amount: 0 })
  }
}

const removeItem = (category, index) => {
  if (form.value.items[category].length > 1) {
    form.value.items[category].splice(index, 1)
  }
}

const handleUpgrade = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true

    const data = {
      projectId: form.value.projectId,
      managementReserve: form.value.managementReserve.toString(),
      items: buildItemsTree()
    }

    await upgradeBudgetApi(route.params.id, data)
    ElMessage.success('预算升级已提交审批')
    router.push('/pm/budget')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('升级失败')
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

const loadPositions = async () => {
  try {
    const res = await getPositionListApi()
    positionList.value = res.data || []
  } catch { /* ignore */ }
}

const loadProjects = async () => {
  try {
    const res = await getCharterListApi({ pageNum: 1, pageSize: 100 })
    projects.value = res.data?.records || []
  } catch { /* ignore */ }
}

const loadOriginalData = async () => {
  if (!route.params.id) return
  try {
    const res = await getBudgetDetailWithItemsApi(route.params.id)
    originalData.value = res.data || { budget: {}, items: [] }

    const data = originalData.value
    form.value.projectId = data.budget?.projectId || ''
    form.value.managementReserve = parseFloat(data.budget?.managementReserve) || 0

    // Pre-fill editable form with original budget items
    if (data.items) {
      const laborItems = data.items.filter(i => i.category === 'LABOR')
      const procurementItems = data.items.filter(i => i.category === 'PROCUREMENT')
      if (laborItems.length > 0) {
        form.value.items.LABOR = laborItems.map(i => ({
          roleCode: i.roleCode || 'DEV',
          positionId: i.positionId || '',
          hours: parseFloat(i.hours) || 0,
          costRate: parseFloat(i.costRate) || 0,
          amount: parseFloat(i.budgetAmount) || 0
        }))
      }
      if (procurementItems.length > 0) {
        form.value.items.PROCUREMENT = procurementItems.map(i => ({
          bomItem: i.bomItem || '',
          qty: parseFloat(i.qty) || 0,
          unitPrice: parseFloat(i.unitPrice) || 0,
          amount: parseFloat(i.budgetAmount) || 0
        }))
      }
      for (const cat of otherCategoryList) {
        const otherItems = data.items.filter(i => i.category === cat.value)
        if (otherItems.length > 0) {
          form.value.items[cat.value][0].amount = parseFloat(otherItems[0].budgetAmount) || 0
        }
      }
    }
  } catch (e) {
    ElMessage.error('加载原预算数据失败')
  }
}

onMounted(async () => {
  await Promise.all([loadProjects(), loadPositions()])
  loadOriginalData()
})

onActivated(async () => {
  await Promise.all([loadProjects(), loadPositions()])
  loadOriginalData()
})

watch(() => route.params.id, (newId) => {
  if (!route.path.startsWith('/pm/budget/upgrade')) return
  if (newId) {
    loadOriginalData()
  }
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.split-panels {
  display: flex;
  gap: 16px;
}

.left-panel,
.right-panel {
  flex: 1;
  min-width: 0;
  max-height: calc(100vh - 160px);
  overflow-y: auto;
}

.panel-title {
  font-size: 15px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 12px;
  padding-left: 4px;
  border-left: 3px solid #409eff;
  padding: 4px 0 4px 10px;
}

.panel-card {
  border: 1px solid #ebeef5;
}

.info-section {
  padding: 16px 0;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.info-item label {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
  min-width: 60px;
}

.info-value {
  font-size: 14px;
  color: #303133;
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

.other-cat-value {
  display: block;
  font-size: 12px;
  color: #303133;
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

:deep(.zero-rate .el-input__inner) {
  color: #f56c6c;
  font-weight: bold;
}
</style>
