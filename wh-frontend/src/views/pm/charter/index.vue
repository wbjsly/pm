<template>
  <div class="charter-list">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px;">
          <span style="font-weight: bold; font-size: 16px;">项目立项</span>
          <div style="display: flex; align-items: center; gap: 8px;">
            <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
              <el-option v-for="item in dictStore.getDictItems('CHARTER_STATUS')" :key="item.itemCode" :label="item.label" :value="item.itemCode" />
            </el-select>
            <el-input v-model="queryParams.keyword" placeholder="项目名称/编号" clearable style="width: 180px" />
            <el-button type="primary" @click="loadData">查询</el-button>
            <el-button @click="resetQuery">重置</el-button>
            <el-tooltip content="新增项目" placement="top">
              <el-button type="primary" @click="$router.push('/pm/charter/form')">
                <el-icon><Plus /></el-icon>
              </el-button>
            </el-tooltip>
          </div>
        </div>
      </template>

      <!-- Table -->
      <el-table :data="tableData" row-key="id" v-loading="loading" stripe>
        <el-table-column prop="projectCode" label="项目编号" width="160" />
        <el-table-column label="项目名称" min-width="180">
          <template #default="{ row }">
            <span>{{ row.projectName }}</span>
            <span v-if="row.projectShortName" style="color: #909399;">（{{ row.projectShortName }}）</span>
          </template>
        </el-table-column>
        <el-table-column prop="projectCategory" label="项目分类" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.projectCategory" size="small" :type="categoryTagType(row.projectCategory)">{{ categoryLabel(row.projectCategory) }}</el-tag>
            <span v-else style="color: #999;">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="pmName" label="项目经理" width="100" />
        <el-table-column prop="progress" label="项目进度" width="100">
          <template #default="{ row }">
            <el-tag :type="dictStore.getTagType('CHARTER_PROGRESS', row.progress)">{{ dictStore.getLabel('CHARTER_PROGRESS', row.progress) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startDate" label="计划开始日期" width="120" />
        <el-table-column prop="endDate" label="计划结束日期" width="120" />
        <el-table-column prop="status" label="审批状态" width="100">
          <template #default="{ row }">
            <el-tag :type="dictStore.getTagType('CHARTER_STATUS', row.status)">{{ dictStore.getLabel('CHARTER_STATUS', row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
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
            <template v-if="row.status === 'PENDING_APPROVAL'">
              <el-tooltip content="通过" placement="top">
                <el-button link type="success" @click="handleApprove(row.id)">
                  <el-icon><Select /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="驳回" placement="top">
                <el-button link type="danger" @click="handleReject(row.id)">
                  <el-icon><CloseBold /></el-icon>
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

    <el-dialog v-model="approvalDialog.visible" :title="approvalDialog.mode === 'approve' ? '审批通过' : '驳回'" width="450px">
      <el-form label-width="80px">
        <el-form-item :label="approvalDialog.mode === 'approve' ? '审批意见' : '驳回原因'">
          <el-input v-model="approvalDialog.comment" type="textarea" :rows="3"
            :placeholder="approvalDialog.mode === 'approve' ? '可选，输入审批意见' : '请输入驳回原因'" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approvalDialog.visible = false">取消</el-button>
        <el-button :type="approvalDialog.mode === 'approve' ? 'success' : 'danger'" @click="confirmApproval">
          {{ approvalDialog.mode === 'approve' ? '确认通过' : '确认驳回' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import { getCharterListApi, deleteCharterApi, submitCharterApi, approveCharterApi, rejectCharterApi } from '@/api/pm/charter'
import { ElMessage, ElMessageBox } from 'element-plus'
import { View, Edit, Promotion, Delete, Select, CloseBold } from '@element-plus/icons-vue'

import { useDictStore } from '@/store/dict'

const router = useRouter()
const dictStore = useDictStore()

const tableData = ref([])
const total = ref(0)
const loading = ref(false)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  status: '',
  keyword: ''
})

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

const approvalDialog = reactive({
  visible: false,
  mode: 'approve',
  comment: '',
  charterId: ''
})

const showApprovalDialog = (id, mode) => {
  approvalDialog.charterId = id
  approvalDialog.mode = mode
  approvalDialog.comment = ''
  approvalDialog.visible = true
}

const handleApprove = (id) => showApprovalDialog(id, 'approve')
const handleReject = (id) => showApprovalDialog(id, 'reject')

const confirmApproval = async () => {
  const { charterId, mode, comment } = approvalDialog
  try {
    if (mode === 'approve') {
      await approveCharterApi(charterId, { rejectReason: comment || undefined })
      ElMessage.success('审批通过')
    } else {
      if (!comment) {
        ElMessage.warning('请输入驳回原因')
        return
      }
      await rejectCharterApi(charterId, { rejectReason: comment })
      ElMessage.success('已驳回')
    }
    approvalDialog.visible = false
    loadData()
  } catch {
    // handled by interceptor
  }
}

const handleSubmit = async (id) => {
  try {
    await ElMessageBox.confirm('确认提交审批？', '提示', { type: 'warning' })
    await submitCharterApi(id)
    ElMessage.success('提交成功')
    loadData()
  } catch {
    // user cancelled or error
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确认删除该章程？', '提示', { type: 'warning' })
    await deleteCharterApi(id)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    // user cancelled or error
  }
}

onMounted(loadData)
onActivated(loadData)
</script>
