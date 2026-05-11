<template>
  <div class="budget-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>预算管理</span>
          <div class="header-actions">
            <el-select v-model="queryParams.projectId" placeholder="项目" clearable filterable style="width: 200px; margin-right: 8px">
              <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
            </el-select>
            <el-select v-model="queryParams.pmId" placeholder="项目经理" clearable filterable style="width: 150px; margin-right: 8px">
              <el-option v-for="pm in pmList" :key="pm.id" :label="pm.label" :value="pm.id" />
            </el-select>
            <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 120px; margin-right: 8px">
              <el-option label="草稿" value="DRAFT" />
              <el-option label="审批中" value="PENDING" />
              <el-option label="已审批" value="APPROVED" />
              <el-option label="已驳回" value="REJECTED" />
            </el-select>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
            <el-tooltip content="新增预算" placement="top">
              <el-button type="primary" @click="handleCreate">
                <el-icon><Plus /></el-icon>
              </el-button>
            </el-tooltip>
          </div>
        </div>
      </template>

      <!-- Table -->
      <el-table :data="tableData" v-loading="loading" :key="tableKey" stripe>
        <el-table-column prop="budgetCode" label="预算编码" width="180" />
        <el-table-column label="项目名称" min-width="200">
          <template #default="{ row }">
            <span>{{ row.projectName }}</span>
            <span v-if="row.projectShortName" style="color: #909399;">（{{ row.projectShortName }}）</span>
          </template>
        </el-table-column>
        <el-table-column prop="pmName" label="项目经理" width="120" />
        <el-table-column prop="version" label="版本号" width="80" />
        <el-table-column prop="projectDirectBudget" label="项目直接预算(元)" width="150">
          <template #default="{ row }">{{ formatMoney(row.costBaseline) }}</template>
        </el-table-column>
        <el-table-column prop="actualCost" label="项目成本(元)" width="150">
          <template #default="{ row }">{{ formatMoney(row.actualCost) }}</template>
        </el-table-column>
        <el-table-column prop="budgetRemaining" label="预算余额(元)" width="150">
          <template #default="{ row }">
            <span :style="{ color: (row.budgetRemaining ?? 0) < 0 ? '#f56c6c' : '' }">
              {{ formatMoney(row.budgetRemaining) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="costRatio" label="预算投入比例" width="120">
          <template #default="{ row }">
            <span :style="{ color: (row.costRatio ?? 0) > 1 ? '#f56c6c' : (row.costRatio ?? 0) > 0.9 ? '#e6a23c' : '' }">
              {{ formatPercent(row.costRatio) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createDate" label="创建日期" width="120">
          <template #default="{ row }">
            {{ formatDate(row.createDate) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-tooltip content="查看" placement="top">
              <el-button link type="primary" @click="handleView(row)" :icon="View" />
            </el-tooltip>
            <el-tooltip content="编辑" placement="top" v-if="row.status === 'DRAFT'">
              <el-button link type="primary" @click="handleEdit(row)" :icon="Edit" />
            </el-tooltip>
            <el-tooltip content="删除" placement="top" v-if="row.status === 'DRAFT'">
              <el-button link type="danger" @click="handleDelete(row)" :icon="Delete" />
            </el-tooltip>
            <el-tooltip content="提交审批" placement="top" v-if="row.status === 'DRAFT'">
              <el-button link type="warning" @click="handleSubmit(row)" :icon="Promotion" />
            </el-tooltip>
            <el-tooltip content="预实对比" placement="top" v-if="row.status === 'APPROVED'">
              <el-button link type="success" @click="handleComparison(row)" :icon="Document" />
            </el-tooltip>
            <el-tooltip content="升级" placement="top" v-if="row.status === 'APPROVED'">
              <el-button link type="primary" @click="handleUpgrade(row)" :icon="Promotion" />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <el-pagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="loadData"
        @size-change="loadData"
        style="margin-top: 16px; justify-content: flex-end"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, View, Edit, Delete, Promotion, Document } from '@element-plus/icons-vue'
import { getBudgetListApi, deleteBudgetApi, submitBudgetApi } from '@/api/pm/budget'
import { getCharterListApi } from '@/api/pm/charter'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const tableKey = ref(0)
const projects = ref([])
const pmList = ref([])
const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  projectId: '',
  pmId: '',
  status: ''
})

const statusType = (status) => {
  const map = { DRAFT: 'info', PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }
  return map[status] || 'info'
}

const formatMoney = (val) => {
  const num = parseFloat(val) || 0
  if (num === 0) return '-'
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const formatPercent = (val) => {
  const num = parseFloat(val) || 0
  if (num === 0) return '-'
  return (num * 100).toFixed(1) + '%'
}

const statusLabel = (status) => {
  const map = { DRAFT: '草稿', PENDING: '审批中', APPROVED: '已审批', REJECTED: '已驳回' }
  return map[status] || status
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.substring(0, 10)
}

const handleUpgrade = (row) => {
  router.push(`/pm/budget/form/${row.id}`)
}

const loadProjects = async () => {
  try {
    const res = await getCharterListApi({ pageNum: 1, pageSize: 100 })
    projects.value = res.data?.records || []
    // Build PM list from charters
    const pmMap = new Map()
    for (const charter of projects.value) {
      if (charter.pmId && charter.pmName) {
        pmMap.set(charter.pmId, { id: charter.pmId, label: charter.pmName })
      }
    }
    pmList.value = Array.from(pmMap.values())
  } catch (e) {
    // Ignore
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getBudgetListApi(queryParams.value)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.value.pageNum = 1
  loadData()
}

const handleReset = () => {
  queryParams.value = { pageNum: 1, pageSize: 10, projectId: '', pmId: '', status: '' }
  loadData()
}

const handleCreate = () => {
  router.push('/pm/budget/form')
}

const handleEdit = (row) => {
  router.push(`/pm/budget/form/${row.id}`)
}

const handleView = (row) => {
  router.push(`/pm/budget/detail/${row.id}`)
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除此预算吗？', '提示', { type: 'warning' })
    await deleteBudgetApi(row.id)
    ElMessage.success('删除成功')
    tableKey.value++
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

const handleSubmit = async (row) => {
  try {
    await ElMessageBox.confirm('确定提交此预算进行审批吗？', '提示', { type: 'warning' })
    await submitBudgetApi(row.id)
    ElMessage.success('提交成功')
    tableKey.value++
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('提交失败')
  }
}

const handleComparison = (row) => {
  router.push(`/pm/budget/comparison/${row.projectId}?budgetId=${row.id}`)
}

onMounted(() => {
  loadProjects()
  loadData()
})
onActivated(loadData)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
