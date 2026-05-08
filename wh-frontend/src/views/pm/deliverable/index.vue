<template>
  <div class="deliverable-list">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px;">
          <span style="font-weight: bold; font-size: 16px;">成果物管理</span>
          <div style="display: flex; align-items: center; gap: 8px;">
            <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
              <el-option label="草稿" value="DRAFT" />
              <el-option label="审批中" value="PENDING_APPROVAL" />
              <el-option label="已通过" value="APPROVED" />
              <el-option label="已交付" value="DELIVERED" />
              <el-option label="已驳回" value="REJECTED" />
            </el-select>
            <el-input v-model="queryParams.keyword" placeholder="名称/编号" clearable style="width: 180px" />
            <el-button type="primary" @click="loadData">查询</el-button>
            <el-button @click="resetQuery">重置</el-button>
            <el-tooltip content="新增成果物" placement="top">
              <el-button type="primary" @click="$router.push('/pm/deliverable/form')">
                <el-icon><Plus /></el-icon>
              </el-button>
            </el-tooltip>
          </div>
        </div>
      </template>

      <el-table :data="tableData" row-key="id" v-loading="loading" stripe>
        <el-table-column prop="deliverableCode" label="成果物编号" width="220" />
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column label="所属项目" width="140">
          <template #default="{ row }">
            <span v-if="row.projectShortName">{{ row.projectShortName }}</span>
            <span v-else style="color: #999;">-</span>
          </template>
        </el-table-column>
        <el-table-column label="附件" width="80" align="center">
          <template #default="{ row }">
            <el-popover
              v-if="getAttachments(row).length > 0"
              placement="right"
              :width="260"
              trigger="hover"
              :show-after="200"
            >
              <template #reference>
                <el-link type="primary" underline="never">
                  <el-icon><Paperclip /></el-icon>
                  {{ getAttachments(row).length }}
                </el-link>
              </template>
              <div v-for="(att, idx) in getAttachments(row)" :key="idx"
                   class="attach-item"
                   :style="idx > 0 ? 'border-top: 1px solid #eee;' : ''">
                <el-button v-if="row.status === 'DRAFT' || row.status === 'REJECTED'"
                           link type="danger" class="attach-del-btn"
                           @click.stop="handleDeleteAttachment(row, idx)">
                  <el-icon><Close /></el-icon>
                </el-button>
                <el-link type="default" underline="never"
                         class="attach-link"
                         @click="handleDownload(row.id, idx)">
                  <el-icon><Document /></el-icon> {{ att.fileName }}
                </el-link>
              </div>
            </el-popover>
            <span v-else style="color: #999;">0</span>
          </template>
        </el-table-column>
        <el-table-column prop="plannedDeliveryDate" label="计划交付日期" width="130" />
        <el-table-column prop="actualDeliveryDate" label="实际交付日期" width="130">
          <template #default="{ row }">
            <span v-if="row.actualDeliveryDate">{{ row.actualDeliveryDate }}</span>
            <span v-else style="color: #999;">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createByName" label="创建人" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-tooltip content="查看" placement="top">
              <el-button link type="primary" @click="$router.push(`/pm/deliverable/detail/${row.id}`)">
                <el-icon><View /></el-icon>
              </el-button>
            </el-tooltip>
            <template v-if="row.status === 'DRAFT' || row.status === 'REJECTED'">
              <el-tooltip content="编辑" placement="top">
                <el-button link type="primary" @click="$router.push(`/pm/deliverable/form/${row.id}`)">
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
import { getDeliverableListApi, deleteDeliverableApi, submitDeliverableApi, getDeliverableAttachmentUrlApi, deleteDeliverableAttachmentApi } from '@/api/pm/deliverable'
import { getCharterListApi } from '@/api/pm/charter'
import { ElMessage, ElMessageBox } from 'element-plus'
import { View, Edit, Promotion, Delete, Plus, Paperclip, Document, Close } from '@element-plus/icons-vue'

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
  const map = { DRAFT: 'info', PENDING_APPROVAL: 'warning', APPROVED: 'success', DELIVERED: '', REJECTED: 'danger' }
  return map[status] || 'info'
}

const statusLabel = (status) => {
  const map = { DRAFT: '草稿', PENDING_APPROVAL: '审批中', APPROVED: '已通过', DELIVERED: '已交付', REJECTED: '已驳回' }
  return map[status] || status
}

const getAttachments = (row) => {
  if (!row.attachments) return []
  try {
    return JSON.parse(row.attachments)
      .filter(a => !a.deleted)
      .sort((a, b) => (a.fileName || '').localeCompare(b.fileName || ''))
  } catch {
    return []
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getDeliverableListApi(queryParams)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
    fillProjectInfo()
  } catch {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const fillProjectInfo = async () => {
  if (tableData.value.length === 0) return
  try {
    const res = await getCharterListApi({ pageSize: 999, status: 'APPROVED' })
    const projects = res.data.records || []
    const map = {}
    projects.forEach(p => { map[p.id] = p.projectShortName || p.projectName })
    tableData.value.forEach(d => {
      d.projectShortName = map[d.projectId] || '-'
    })
  } catch { /* ignore */ }
}

const resetQuery = () => {
  queryParams.status = ''
  queryParams.keyword = ''
  queryParams.pageNum = 1
  loadData()
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确认删除该成果物？', '提示', { type: 'warning' })
  await deleteDeliverableApi(id)
  ElMessage.success('删除成功')
  loadData()
}

const handleSubmit = async (id) => {
  await ElMessageBox.confirm('确认提交审批？', '提示', { type: 'warning' })
  await submitDeliverableApi(id)
  ElMessage.success('提交成功')
  loadData()
}

const handleDownload = async (deliverableId, index) => {
  try {
    const res = await getDeliverableAttachmentUrlApi(deliverableId, index)
    if (res.code === 200) {
      window.open(res.data, '_blank')
    }
  } catch {
    ElMessage.error('获取下载链接失败')
  }
}

const handleDeleteAttachment = async (row, index) => {
  try {
    await ElMessageBox.confirm('确认删除该附件？', '提示', { type: 'warning' })
    const res = await deleteDeliverableAttachmentApi(row.id, index)
    if (res.code === 200) {
      row.attachments = JSON.stringify(res.data)
      ElMessage.success('删除成功')
    }
  } catch {
    // cancelled or error
  }
}

onMounted(loadData)
onActivated(loadData)
</script>

<style scoped>
.deliverable-list {
  padding: 8px;
}
</style>

<style>
.attach-item {
  display: flex;
  align-items: center;
  padding: 4px 0;
}
.attach-link {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  justify-content: flex-start;
  color: #333 !important;
}
.attach-link:hover {
  color: #409eff !important;
}
.attach-del-btn {
  flex-shrink: 0;
  margin-right: 2px;
  font-size: 14px;
}
</style>
