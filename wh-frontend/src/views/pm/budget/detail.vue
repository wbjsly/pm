<template>
  <div class="budget-detail">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>查看预算</span>
          <el-button @click="$router.back()">返回</el-button>
        </div>
      </template>

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
              <span class="info-value">{{ data.budget?.budgetCode || '' }}</span>
            </div>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <div class="info-item">
              <label>版本号</label>
              <el-tag :type="versionType">{{ data.budget?.version || '' }}</el-tag>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="info-item">
              <label>状态</label>
              <el-tag :type="statusType(data.budget?.status)">{{ statusLabel(data.budget?.status) }}</el-tag>
            </div>
          </el-col>
        </el-row>
      </div>

        <!-- Budget Categories -->
        <el-divider>预算科目</el-divider>

        <!-- LABOR -->
        <el-card class="category-card">
          <template #header>
            <div class="category-header">
              <span>人工</span>
              <span class="category-amount">合计：¥ {{ formatMoney(laborTotal) }} 元人民币</span>
            </div>
          </template>
          <el-table :data="laborItems" style="width: 100%" size="small">
            <el-table-column label="岗位" width="160">
              <template #default="{ row }">
                <span>{{ row.positionName || roleLabel(row.roleCode) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="工时(小时)" width="160">
              <template #default="{ row }">
                <span>{{ row.hours }}</span>
              </template>
            </el-table-column>
            <el-table-column label="" width="20" align="center">
              <template #default>
                <span class="table-operator">*</span>
              </template>
            </el-table-column>
            <el-table-column label="成本定额(元/时)" width="180">
              <template #default="{ row }">
                <span>{{ formatMoney(row.costRate) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="" width="20" align="center">
              <template #default>
                <span class="table-operator">=</span>
              </template>
            </el-table-column>
            <el-table-column label="金额" width="180">
              <template #default="{ row }">¥ {{ formatMoney(row.budgetAmount) }}</template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- PROCUREMENT -->
        <el-card class="category-card">
          <template #header>
            <div class="category-header">
              <span>采购</span>
              <span class="category-amount">合计：¥ {{ formatMoney(procurementTotal) }} 元人民币</span>
            </div>
          </template>
          <el-table :data="procurementItems" style="width: 100%" size="small">
            <el-table-column label="BOM项" width="240">
              <template #default="{ row }">
                <span>{{ row.bomItem }}</span>
              </template>
            </el-table-column>
            <el-table-column label="数量" width="140">
              <template #default="{ row }">
                <span>{{ row.qty }}</span>
              </template>
            </el-table-column>
            <el-table-column label="" width="20" align="center">
              <template #default>
                <span class="table-operator">*</span>
              </template>
            </el-table-column>
            <el-table-column label="单价" width="160">
              <template #default="{ row }">
                <span>{{ formatMoney(row.unitPrice) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="" width="20" align="center">
              <template #default>
                <span class="table-operator">=</span>
              </template>
            </el-table-column>
            <el-table-column label="金额" width="180">
              <template #default="{ row }">¥ {{ formatMoney(row.budgetAmount) }}</template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- Other Categories -->
        <el-card class="category-card">
          <template #header>
            <div class="category-header">
              <span>其他科目</span>
              <span class="category-amount">合计：¥ {{ formatMoney(otherTotal) }} 元人民币</span>
            </div>
          </template>
          <div class="other-categories-row">
            <template v-for="(cat, idx) in otherCategoryList" :key="cat.value">
              <div class="other-cat-item">
                <label class="other-cat-label">{{ cat.label }}</label>
                <span class="other-cat-value">{{ formatMoney(getOtherAmount(cat.value)) }}</span>
              </div>
              <span v-if="idx < otherCategoryList.length - 1" class="other-plus-operator">+</span>
            </template>
          </div>
        </el-card>

        <!-- Summary -->
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
          <div class="summary-item">
            <label>项目管理预算</label>
            <div class="summary-value">¥ {{ formatMoney(data.budget?.managementReserve) }} 元人民币</div>
          </div>
        </div>

      <div class="form-actions">
        <el-button @click="$router.back()">返回</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onActivated, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getBudgetDetailWithItemsApi } from '@/api/pm/budget'
import { getCharterListApi } from '@/api/pm/charter'

const route = useRoute()
const data = ref({
  budget: {},
  items: []
})
const projects = ref([])
const loading = ref(false)

const otherCategoryList = [
  { label: '差旅', value: 'TRAVEL' },
  { label: '商务费用', value: 'BUSINESS' },
  { label: '客户招待费', value: 'ENTERTAINMENT' },
  { label: '活动费', value: 'ACTIVITY' },
  { label: '其他', value: 'OTHER' }
]

const projectName = computed(() => {
  const projectId = data.value.budget?.projectId
  const p = projects.value.find(x => x.id === projectId)
  return p?.projectName || ''
})

const allItems = computed(() => data.value.items || [])

const laborItems = computed(() => allItems.value.filter(i => i.category === 'LABOR'))
const procurementItems = computed(() => allItems.value.filter(i => i.category === 'PROCUREMENT'))

const laborTotal = computed(() => laborItems.value.reduce((s, i) => s + (i.budgetAmount || 0), 0))
const procurementTotal = computed(() => procurementItems.value.reduce((s, i) => s + (i.budgetAmount || 0), 0))

const otherTotal = computed(() => {
  let total = 0
  for (const cat of otherCategoryList) {
    const items = allItems.value.filter(i => i.category === cat.value)
    total += items.reduce((s, i) => s + (i.budgetAmount || 0), 0)
  }
  return total
})

const costBaseline = computed(() => {
  return allItems.value.reduce((s, i) => s + (i.budgetAmount || 0), 0)
})

const totalBudget = computed(() => {
  return costBaseline.value + (parseFloat(data.value.budget?.managementReserve) || 0)
})

const formatMoney = (val) => {
  const num = val || 0
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const getOtherAmount = (category) => {
  const items = allItems.value.filter(i => i.category === category)
  return items.reduce((s, i) => s + (i.budgetAmount || 0), 0)
}

const roleLabel = (code) => {
  const map = { DEV: '开发(DEV)', QA: '测试(QA)', BA: '产品经理(BA)', ARCH: '架构师(ARCH)', PM: '项目经理(PM)' }
  return map[code] || code
}

const statusType = (status) => {
  const map = { DRAFT: 'info', PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }
  return map[status] || 'info'
}

const statusLabel = (status) => {
  const map = { DRAFT: '草稿', PENDING: '审批中', APPROVED: '已审批', REJECTED: '已驳回' }
  return map[status] || status
}

const versionType = (status) => {
  const s = data.value.budget?.status
  return statusType(s)
}

const loadData = async () => {
  if (!route.params.id) return
  try {
    const res = await getBudgetDetailWithItemsApi(route.params.id)
    data.value = res.data || { budget: {}, items: [] }
  } catch (e) {
    ElMessage.error('加载预算数据失败')
  }
}

const loadProjects = async () => {
  try {
    const res = await getCharterListApi({ pageNum: 1, pageSize: 100 })
    projects.value = res.data?.records || []
  } catch (e) {
    // Ignore
  }
}

onMounted(async () => {
  await loadProjects()
  loadData()
})

onActivated(loadData)

watch(() => route.params.id, () => {
  if (!route.path.startsWith('/pm/budget/detail')) return
  loadData()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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
  font-size: 14px;
  color: #409eff;
  font-weight: bold;
}
.other-plus-operator {
  font-size: 18px;
  font-weight: bold;
  color: #909399;
  align-self: flex-end;
  margin-bottom: 8px;
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
.form-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}
.table-operator {
  font-weight: bold;
  color: #909399;
  font-size: 16px;
}
</style>
