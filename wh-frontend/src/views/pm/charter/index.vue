<template>
  <div class="charter-list">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px;">
          <span style="font-weight: bold; font-size: 16px;">项目</span>
          <div style="display: flex; align-items: center; gap: 8px;">
            <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
              <el-option label="草稿" value="DRAFT" />
              <el-option label="审批中" value="PENDING_APPROVAL" />
              <el-option label="已通过" value="APPROVED" />
              <el-option label="已驳回" value="REJECTED" />
              <el-option label="已关闭" value="CLOSED" />
            </el-select>
            <el-input v-model="queryParams.keyword" placeholder="项目名称/编号" clearable style="width: 180px" />
            <el-button type="primary" @click="loadData">查询</el-button>
            <el-button @click="resetQuery">重置</el-button>
            <el-button type="primary" @click="$router.push('/pm/charter/form')">
              <el-icon><Plus /></el-icon> 新增项目
            </el-button>
          </div>
        </div>
      </template>

      <!-- Table -->
      <el-table :data="tableData" row-key="id" v-loading="loading" stripe>
        <el-table-column prop="charterCode" label="章程编号" width="200" />
        <el-table-column prop="projectName" label="项目名称" min-width="100" />
        <el-table-column prop="projectCode" label="项目编号" width="160" />
        <el-table-column prop="projectCategory" label="项目分类" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.projectCategory" size="small" :type="categoryTagType(row.projectCategory)">{{ categoryLabel(row.projectCategory) }}</el-tag>
            <span v-else style="color: #999;">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="contractNo" label="合同编号" width="150" />
        <el-table-column prop="pmName" label="项目经理" width="100" />
        <el-table-column prop="sponsorName" label="项目发起人" width="110" />
        <el-table-column prop="startDate" label="计划开始日期" width="120" />
        <el-table-column prop="endDate" label="计划结束日期" width="120" />
        <el-table-column prop="status" label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-tooltip content="查看" placement="top">
              <el-button link type="primary" @click="$router.push(`/pm/charter/detail/${row.id}`)">
                <el-icon><View /></el-icon>
              </el-button>
            </el-tooltip>
            <template v-if="row.status === 'DRAFT' || row.status === 'REJECTED'">
              <el-tooltip content="编辑" placement="top">
                <el-button link type="primary" @click="$router.push(`/pm/charter/form/${row.id}`)">
                  <el-icon><Edit /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="提交" placement="top">
                <el-button link type="success" @click="handleSubmit(row.id)">
                  <el-icon><Promotion /></el-icon>
                </el-button>
              </el-tooltip>
            </template>
            <el-tooltip v-if="row.status === 'DRAFT'" content="删除" placement="top">
              <el-button link type="danger" @click="handleDelete(row.id)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <el-pagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        :pager-count="5"
        @current-change="loadData"
        @size-change="loadData"
        style="margin-top: 16px; justify-content: flex-end;"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import { getCharterListApi, deleteCharterApi, submitCharterApi } from '@/api/pm/charter'
import { ElMessage, ElMessageBox } from 'element-plus'
import { View, Edit, Promotion, Delete } from '@element-plus/icons-vue'

const router = useRouter()

const tableData = ref([])
const total = ref(0)
const loading = ref(false)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  status: '',
  keyword: ''
})

const statusTagType = (status) => {
  const map = { DRAFT: 'info', PENDING_APPROVAL: 'warning', APPROVED: 'success', REJECTED: 'danger', CLOSED: '' }
  return map[status] || 'info'
}

const statusLabel = (status) => {
  const map = { DRAFT: '草稿', PENDING_APPROVAL: '审批中', APPROVED: '已通过', REJECTED: '已驳回', CLOSED: '已关闭' }
  return map[status] || status
}

const categoryTagType = (cat) => {
  const map = { CONTRACT: 'primary', R_D: 'warning', ADVANCE: 'success', PUBLIC: 'info' }
  return map[cat] || 'info'
}
const categoryLabel = (cat) => {
  const map = { CONTRACT: '合同项目', R_D: '研发项目', ADVANCE: '提前执行', PUBLIC: '公共项目' }
  return map[cat] || cat
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getCharterListApi(queryParams)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  queryParams.status = ''
  queryParams.keyword = ''
  queryParams.pageNum = 1
  loadData()
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确认删除该章程？', '提示', { type: 'warning' })
  await deleteCharterApi(id)
  ElMessage.success('删除成功')
  loadData()
}

const handleSubmit = async (id) => {
  await ElMessageBox.confirm('确认提交审批？', '提示', { type: 'warning' })
  await submitCharterApi(id)
  ElMessage.success('提交成功')
  loadData()
}

onMounted(loadData)
onActivated(loadData)
</script>
