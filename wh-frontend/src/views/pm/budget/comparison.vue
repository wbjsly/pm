<template>
  <div class="budget-comparison">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-button @click="$router.back()">返回</el-button>
          <span>预实对比 - {{ projectName }}</span>
          <el-select v-model="selectedVersion" placeholder="版本" @change="loadData" style="width: 150px">
            <el-option v-for="v in versions" :key="v.id" :label="v.version" :value="v.id" />
          </el-select>
        </div>
      </template>

      <div v-loading="loading">
        <!-- Summary -->
        <el-row :gutter="16" style="margin-bottom: 16px">
          <el-col :span="8">
            <el-card shadow="never">
              <div class="stat-card">
                <div class="stat-label">总预算</div>
                <div class="stat-value">¥ {{ data.totalBudget?.toFixed(2) || '0.00' }}</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="never">
              <div class="stat-card">
                <div class="stat-label">实际成本</div>
                <div class="stat-value" :class="{ 'over-budget': data.totalRatio > 1 }">
                  ¥ {{ data.totalActual?.toFixed(2) || '0.00' }}
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="never">
              <div class="stat-card">
                <div class="stat-label">执行比率</div>
                <div class="stat-value" :style="{ color: ratioColor(data.totalRatio) }">
                  {{ ((data.totalRatio || 0) * 100).toFixed(1) }}%
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <!-- Comparison Table -->
        <el-table :data="flatItems" border stripe row-key="id">
          <el-table-column prop="name" label="预算科目" min-width="150" />
          <el-table-column label="层级" width="80">
            <template #default="{ row }">
              <el-tag size="small" :type="row.level === 1 ? '' : 'info'">{{ row.level === 1 ? '一级' : '二级' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="预算金额" width="130" align="right">
            <template #default="{ row }">¥ {{ row.budgetAmount?.toFixed(2) || '0.00' }}</template>
          </el-table-column>
          <el-table-column label="实际金额" width="130" align="right">
            <template #default="{ row }">¥ {{ row.actualAmount?.toFixed(2) || '0.00' }}</template>
          </el-table-column>
          <el-table-column label="比率" width="100" align="center">
            <template #default="{ row }">
              <span :style="{ color: ratioColor(row.ratio), fontWeight: 'bold' }">
                {{ ((row.ratio || 0) * 100).toFixed(1) }}%
              </span>
            </template>
          </el-table-column>
          <el-table-column label="详情" width="80" v-if="hasDetail">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="showDetail(row)" v-if="row.level === 2">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getBudgetComparisonApi, getBudgetVersionsApi } from '@/api/pm/budget'

const route = useRoute()
const loading = ref(false)
const data = ref({
  totalBudget: 0,
  totalActual: 0,
  totalRatio: 0,
  items: []
})
const versions = ref([])
const selectedVersion = ref('')
const projectName = ref('')

const categoryLabels = {
  LABOR: '人工',
  PROCUREMENT: '采购',
  TRAVEL: '差旅',
  BUSINESS: '商务费用',
  ENTERTAINMENT: '客户招待费',
  ACTIVITY: '活动费',
  OTHER: '其他'
}

const flatItems = computed(() => {
  const items = []
  for (const item of data.value.items || []) {
    items.push({
      id: item.id,
      name: categoryLabels[item.category] || item.category,
      level: item.level,
      budgetAmount: item.budgetAmount,
      actualAmount: item.actualAmount,
      ratio: item.ratio
    })
    for (const child of item.children || []) {
      let childName = categoryLabels[child.category] || child.category
      if (child.roleCode) childName += ` - ${child.roleCode}`
      if (child.bomItem) childName += ` - ${child.bomItem}`
      items.push({
        id: child.id,
        name: childName,
        level: child.level,
        budgetAmount: child.budgetAmount,
        actualAmount: child.actualAmount,
        ratio: child.ratio
      })
    }
  }
  return items
})

const hasDetail = computed(() => {
  return flatItems.value.some(item => item.level === 2)
})

const ratioColor = (ratio) => {
  if (!ratio) return '#67c23a'
  if (ratio < 0.8) return '#67c23a'
  if (ratio < 0.95) return '#e6a23c'
  if (ratio <= 1) return '#f56c6c'
  return '#f56c6c'
}

const showDetail = (row) => {
  // Could navigate to actual cost detail filtered by budget item
  ElMessage.info(`查看科目: ${row.name}`)
}

const loadData = async () => {
  loading.value = true
  try {
    // Use budgetId from query or projectId from params
    const budgetId = route.query.budgetId || route.params.projectId
    const res = await getBudgetComparisonApi(budgetId, selectedVersion.value ? { version: selectedVersion.value } : {})
    data.value = res.data
    projectName.value = res.data.projectName || ''
  } catch (e) {
    ElMessage.error('加载预实对比数据失败')
  } finally {
    loading.value = false
  }
}

const loadVersions = async () => {
  try {
    const res = await getBudgetVersionsApi(route.params.projectId)
    versions.value = res.data || []
    if (versions.value.length > 0) {
      selectedVersion.value = versions.value[0].id
    }
  } catch (e) {
    // No versions available
  }
}

onMounted(() => {
  loadVersions()
  loadData()
})
</script>

<style scoped>
.card-header {
  display: flex;
  gap: 16px;
  align-items: center;
}
.stat-card {
  text-align: center;
  padding: 8px 0;
}
.stat-label {
  color: #909399;
  font-size: 14px;
}
.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  margin-top: 8px;
}
.stat-value.over-budget {
  color: #f56c6c;
}
</style>
