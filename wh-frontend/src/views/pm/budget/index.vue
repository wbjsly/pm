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
              <el-option v-for="item in dictStore.getDictItems('BUDGET_STATUS')" :key="item.itemCode" :label="item.label" :value="item.itemCode" />
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

      <el-table
        :data="tableData"
        v-loading="loading"
        :key="tableKey"
        row-key="id"
        :expand-row-keys="expandRowKeys"
        @expand-change="handleExpand"
        @row-click="handleRowClick"
        stripe
        style="width: 100%"
      >
        <el-table-column type="expand">
          <template #default="{ row }">
            <el-table
              v-if="row.children && row.children.length > 0"
              :data="row.children"
              size="small"
              style="margin: 4px 20px 8px 48px; width: auto;"
            >
              <el-table-column prop="budgetCode" label="预算编码" width="180" />
              <el-table-column prop="version" label="版本号" width="90" />
              <el-table-column label="项目总预算(元)" width="150">
                <template #default="{ row: r }">{{ formatMoney(r.totalBudget) }}</template>
              </el-table-column>
              <el-table-column label="直接预算(元)" width="150">
                <template #default="{ row: r }">{{ formatMoney(r.costBaseline) }}</template>
              </el-table-column>
              <el-table-column label="人工(元)" width="140">
                <template #default="{ row: r }">{{ formatMoney(r.laborAmount) }}</template>
              </el-table-column>
              <el-table-column label="采购(元)" width="140">
                <template #default="{ row: r }">{{ formatMoney(r.procurementAmount) }}</template>
              </el-table-column>
              <el-table-column label="其他(元)" width="140">
                <template #default="{ row: r }">{{ formatMoney(r.otherAmount) }}</template>
              </el-table-column>
              <el-table-column label="管理储备(元)" width="140">
                <template #default="{ row: r }">{{ formatMoney(r.managementReserve) }}</template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="{ row: r }">
                  <el-tag :type="dictStore.getTagType('BUDGET_STATUS', r.status)">{{ dictStore.getLabel('BUDGET_STATUS', r.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="创建日期" width="120">
                <template #default="{ row: r }">
                  {{ formatDate(r.createDate) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="200">
                <template #default="{ row: r }">
                  <el-tooltip content="查看" placement="top">
                    <el-button link type="primary" @click="handleView(r)" :icon="View" />
                  </el-tooltip>
                  <el-tooltip content="编辑" placement="top" v-if="r.status === 'DRAFT'">
                    <el-button link type="primary" @click="handleEdit(r)" :icon="Edit" />
                  </el-tooltip>
                  <el-tooltip content="删除" placement="top" v-if="r.status === 'DRAFT'">
                    <el-button link type="danger" @click="handleDelete(r)" :icon="Delete" />
                  </el-tooltip>
                  <el-tooltip content="提交审批" placement="top" v-if="r.status === 'DRAFT'">
                    <el-button link type="warning" @click="handleSubmit(r)" :icon="Promotion" />
                  </el-tooltip>
                  <el-tooltip content="预实对比" placement="top" v-if="r.status === 'APPROVED'">
                    <el-button link type="success" @click="handleComparison(r)" :icon="Document" />
                  </el-tooltip>
                </template>
              </el-table-column>
            </el-table>
            <span v-else style="padding: 8px 48px; color: #909399; display: inline-block;">暂无预算版本</span>
          </template>
        </el-table-column>
        <el-table-column label="项目名称" min-width="200">
          <template #default="{ row }">
            <span style="font-weight: 600;">{{ row.projectName }}</span>
            <span v-if="row.projectShortName" style="color: #909399;">（{{ row.projectShortName }}）</span>
          </template>
        </el-table-column>
        <el-table-column label="项目状态" width="100">
          <template #default="{ row }">
            <el-tag :type="dictStore.getTagType('CHARTER_STATUS', row.projectStatus)">{{ dictStore.getLabel('CHARTER_STATUS', row.projectStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最新已审批版本" width="130">
          <template #default="{ row }">
            <span v-if="row.latestApprovedVersion">{{ row.latestApprovedVersion }}</span>
            <span v-else style="color: #c0c4cc;">-</span>
          </template>
        </el-table-column>
        <el-table-column label="项目总预算(元)" width="150">
          <template #default="{ row }">
            <span v-if="row.latestApprovedTotalBudget">{{ formatMoney(row.latestApprovedTotalBudget) }}</span>
            <span v-else style="color: #c0c4cc;">-</span>
          </template>
        </el-table-column>
        <el-table-column label="已审批项目直接预算(元)" width="180">
          <template #default="{ row }">
            <span v-if="row.latestApprovedAmount">{{ formatMoney(row.latestApprovedAmount) }}</span>
            <span v-else style="color: #c0c4cc;">-</span>
          </template>
        </el-table-column>
        <el-table-column label="实际成本(元)" width="140">
          <template #default="{ row }">
            {{ formatMoney(row.projectActualCost) }}
          </template>
        </el-table-column>
        <el-table-column label="预算余额(元)" width="140">
          <template #default="{ row }">
            <span :style="{ color: (row.projectBudgetRemaining ?? 0) < 0 ? '#f56c6c' : '' }">
              {{ formatMoney(row.projectBudgetRemaining) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="投入比例" width="110">
          <template #default="{ row }">
            <span :style="{ color: (row.projectCostRatio ?? 0) > 1 ? '#f56c6c' : (row.projectCostRatio ?? 0) > 0.9 ? '#e6a23c' : '' }">
              {{ formatPercent(row.projectCostRatio) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="版本数" width="70">
          <template #default="{ row }">
            {{ row.versionCount }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; gap: 2px;">
              <el-tooltip content="预实对比" placement="top" v-if="row.latestApprovedBudgetId">
                <el-button link type="success" @click="handleProjectComparison(row)" :icon="Document" />
              </el-tooltip>
              <el-tooltip content="升级" placement="top" v-if="row.latestApprovedBudgetId && !hasDraftOrPendingBudget(row)">
                <el-button link type="primary" @click="handleProjectUpgrade(row)" :icon="Promotion" />
              </el-tooltip>
              <el-tooltip content="新增预算" placement="top" v-if="!row.hasAnyBudget">
                <el-button link type="primary" @click="handleProjectCreate(row)" :icon="Plus" />
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>

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
import { getProjectBudgetsApi, deleteBudgetApi, submitBudgetApi } from '@/api/pm/budget'
import { getCharterListApi } from '@/api/pm/charter'

import { useDictStore } from '@/store/dict'

const router = useRouter()
const dictStore = useDictStore()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const tableKey = ref(0)
const projects = ref([])
const pmList = ref([])
const expandRowKeys = ref([])
const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  projectId: '',
  pmId: '',
  status: ''
})

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

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.substring(0, 10)
}

const handleExpand = (row, expandedRows) => {
  if (expandedRows.length > 0) {
    expandRowKeys.value = [expandedRows[expandedRows.length - 1].id]
  } else {
    expandRowKeys.value = []
  }
}

const handleRowClick = (row) => {
  if (expandRowKeys.value.includes(row.id)) {
    expandRowKeys.value = []
  } else {
    expandRowKeys.value = [row.id]
  }
}

const handleUpgrade = (row) => {
  router.push(`/pm/budget/upgrade/${row.id}`)
}

const hasDraftOrPendingBudget = (row) => {
  return row.children && row.children.some(c => c.status === 'DRAFT' || c.status === 'PENDING')
}

const handleProjectUpgrade = (row) => {
  router.push(`/pm/budget/upgrade/${row.latestApprovedBudgetId}`)
}

const loadProjects = async () => {
  try {
    const res = await getCharterListApi({ pageNum: 1, pageSize: 100 })
    projects.value = res.data?.records || []
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
    const res = await getProjectBudgetsApi(queryParams.value)
    const records = res.data.records || []
    total.value = res.data.total || 0

    tableData.value = records.map(project => {
      const children = (project.budgets || []).map(b => ({
        ...b,
        isProject: false
      }))
      return {
        id: 'proj-' + project.projectId,
        projectId: project.projectId,
        projectName: project.projectName,
        projectShortName: project.projectShortName,
        projectStatus: project.projectStatus,
        hasAnyBudget: project.hasAnyBudget,
        latestApprovedVersion: project.latestApprovedVersion,
        latestApprovedAmount: project.latestApprovedAmount,
        latestApprovedTotalBudget: project.latestApprovedTotalBudget,
        latestApprovedBudgetId: project.latestApprovedBudgetId,
        projectActualCost: project.projectActualCost,
        projectBudgetRemaining: project.projectBudgetRemaining,
        projectCostRatio: project.projectCostRatio,
        versionCount: children.length,
        children
      }
    })
  } catch (e) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.value.pageNum = 1
  expandRowKeys.value = []
  loadData()
}

const handleReset = () => {
  queryParams.value = { pageNum: 1, pageSize: 10, projectId: '', pmId: '', status: '' }
  expandRowKeys.value = []
  loadData()
}

const handleCreate = () => {
  router.push('/pm/budget/form')
}

const handleProjectCreate = (row) => {
  router.push(`/pm/budget/form?projectId=${row.projectId}`)
}

const handleEdit = (row) => {
  const version = parseFloat(row.version)
  if (version > 0.5) {
    router.push(`/pm/budget/upgrade/${row.id}`)
  } else {
    router.push(`/pm/budget/form/${row.id}`)
  }
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

const handleProjectComparison = (row) => {
  if (row.latestApprovedBudgetId) {
    router.push(`/pm/budget/comparison/${row.projectId}?budgetId=${row.latestApprovedBudgetId}`)
  }
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
