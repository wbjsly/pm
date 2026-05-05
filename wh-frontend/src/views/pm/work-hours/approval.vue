<template>
  <div class="work-hours-approval">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>工时审批</span>
          <div class="header-actions">
            <el-select v-model="selectedMonth" placeholder="月份" style="width: 120px" @change="loadData">
              <el-option :label="`${year}年${month}月`" :value="month" />
              <el-option v-if="prevMonth > 0" :label="`${prevYear}年${prevMonth}月`" :value="prevMonth" />
            </el-select>
            <el-input v-model="filterProject" placeholder="项目名称" clearable style="width: 160px" @clear="applyFilter" @keyup.enter="applyFilter" />
            <el-input v-model="filterCreator" placeholder="录入人" clearable style="width: 120px" @clear="applyFilter" @keyup.enter="applyFilter" />
            <el-button type="primary" size="small" @click="applyFilter">查询</el-button>
            <el-button @click="$router.push('/pm/work-hours')">返回</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="logDate" label="日期" width="120" />
        <el-table-column prop="projectName" label="项目名称" min-width="180" />
        <el-table-column prop="projectShortName" label="项目简称" width="100" />
        <el-table-column prop="createByName" label="录入人" width="100" />
        <el-table-column prop="hoursWorked" label="工时(h)" width="90" />
        <el-table-column prop="workDescription" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="success" size="small" @click="handleApprove(row)">通过</el-button>
            <el-button link type="danger" size="small" @click="handleReject(row)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="batch-actions" v-if="selectedRows.length > 0">
        <span>已选 {{ selectedRows.length }} 条</span>
        <el-button type="success" size="small" @click="handleBatchApprove">批量通过</el-button>
        <el-button type="danger" size="small" @click="handleBatchReject">批量驳回</el-button>
      </div>
    </el-card>

    <!-- Reject dialog -->
    <el-dialog v-model="rejectVisible" title="驳回工时" width="400px">
      <el-form :model="rejectForm" label-width="80px">
        <el-form-item label="驳回原因">
          <el-input v-model="rejectForm.reason" type="textarea" :rows="3" placeholder="请输入驳回原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReject">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPendingWorkHoursApi, approveWorkHourApi, rejectWorkHourApi, batchApproveWorkHoursApi, batchRejectWorkHoursApi } from '@/api/pm/workHours'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const allData = ref([])
const tableData = ref([])
const selectedRows = ref([])
const rejectVisible = ref(false)
const rejectForm = ref({ reason: '', ids: [] })
const filterProject = ref('')
const filterCreator = ref('')

const now = new Date()
const year = ref(now.getFullYear())
const month = ref(now.getMonth() + 1)
const selectedMonth = ref(now.getMonth() + 1)

const prevMonth = computed(() => {
  let m = month.value - 1
  if (m < 1) m = 12
  return m
})
const prevYear = computed(() => {
  return month.value === 1 ? year.value - 1 : year.value
})

function handleSelectionChange(rows) {
  selectedRows.value = rows
}

async function loadData() {
  loading.value = true
  try {
    const res = await getPendingWorkHoursApi(String(year.value), selectedMonth.value)
    allData.value = res.data || []
    applyFilter()
  } catch (e) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

function applyFilter() {
  const kwProject = filterProject.value.trim().toLowerCase()
  const kwCreator = filterCreator.value.trim().toLowerCase()
  tableData.value = allData.value.filter(row => {
    const matchProject = !kwProject || (row.projectName || '').toLowerCase().includes(kwProject)
    const matchCreator = !kwCreator || (row.createByName || '').toLowerCase().includes(kwCreator)
    return matchProject && matchCreator
  })
}

async function handleApprove(row) {
  try {
    await ElMessageBox.confirm('确定通过此工时记录吗？', '提示', { type: 'warning' })
    await approveWorkHourApi(row.id, {})
    ElMessage.success('审批通过')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('审批失败')
  }
}

function handleReject(row) {
  rejectForm.value = { reason: '', ids: [row.id] }
  rejectVisible.value = true
}

async function confirmReject() {
  if (!rejectForm.value.reason) {
    ElMessage.warning('请输入驳回原因')
    return
  }
  try {
    if (rejectForm.value.ids.length === 1) {
      await rejectWorkHourApi(rejectForm.value.ids[0], { reason: rejectForm.value.reason })
    } else {
      await batchRejectWorkHoursApi(rejectForm.value.ids, rejectForm.value.reason)
    }
    ElMessage.success('驳回成功')
    rejectVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error('驳回失败')
  }
}

function handleBatchApprove() {
  const ids = selectedRows.value.map(r => r.id)
  ElMessageBox.confirm(`确定批量通过 ${ids.length} 条工时记录吗？`, '提示', { type: 'warning' }).then(async () => {
    try {
      const res = await batchApproveWorkHoursApi(ids)
      const skipped = res.data?.skipped || 0
      ElMessage.success(`成功通过 ${res.data?.success || 0} 条，跳过 ${skipped} 条`)
      loadData()
    } catch (e) {
      ElMessage.error('批量审批失败')
    }
  }).catch(() => {})
}

function handleBatchReject() {
  rejectForm.value = { reason: '', ids: selectedRows.value.map(r => r.id) }
  rejectVisible.value = true
}

onMounted(loadData)
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
  gap: 8px;
}
.batch-actions {
  margin-top: 12px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
