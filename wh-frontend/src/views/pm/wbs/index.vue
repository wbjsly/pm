<template>
  <div class="wbs-list">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span style="font-weight: bold; font-size: 16px;">WBS管理</span>
          <div style="display: flex; align-items: center; gap: 8px;">
            <el-input v-model="keyword" placeholder="搜索WBS编码/名称" clearable style="width: 180px" @keyup.enter="loadProjects" />
            <el-button type="primary" @click="loadProjects">查询</el-button>
            <el-button @click="resetQuery">重置</el-button>
          </div>
        </div>
      </template>

      <!-- Project accordion list -->
      <div v-for="project in projectList" :key="project.id" style="margin-bottom: 8px;">
        <div style="display: flex; align-items: center; padding: 10px 16px; background: #f5f7fa; cursor: pointer; border-radius: 4px;"
             @click="toggleProject(project.id)">
          <el-icon style="transition: transform 0.2s; margin-right: 12px;" :style="{ transform: expandedProjectId === project.id ? 'rotate(90deg)' : '' }">
            <ArrowRight />
          </el-icon>
          <span style="font-weight: bold; margin-right: 24px; font-size: 12px;">{{ project.projectName }}</span>
          <el-tag size="small">{{ project.charterCode }}</el-tag>
          <el-tag size="small" :type="statusTagType(project.status)" style="margin-left: 16px;">{{ statusLabel(project.status) }}</el-tag>
          <span v-if="project.pmName" style="margin-left: 24px; color: #666; font-size: 12px;">PM: {{ project.pmName }}</span>
          <span v-if="project.wbsTotalEffort != null" style="margin-left: 24px; color: #666; font-size: 12px;">工时: {{ project.wbsTotalEffort }}h</span>
          <span v-if="project.wbsLatestEndDate" style="margin-left: 24px; color: #666; font-size: 12px;">最晚: {{ project.wbsLatestEndDate }}</span>
          <span style="margin-left: auto; display: flex; gap: 8px;" v-if="expandedProjectId === project.id">
            <el-button type="primary" size="small" @click.stop="handleAdd(project.id)">
              <el-icon><Plus /></el-icon> 新增WBS
            </el-button>
            <el-button size="small" @click.stop="handleImport(project.id)">
              <el-icon><Upload /></el-icon> 导入
            </el-button>
            <el-button size="small" @click.stop="handleExport(project.id)">
              <el-icon><Download /></el-icon> 导出
            </el-button>
          </span>
        </div>

        <!-- WBS tree table for expanded project -->
        <div v-if="expandedProjectId === project.id" style="padding: 12px 0;">
          <el-table :data="wbsTreeData" row-key="id" :tree-props="{ children: 'children' }" v-loading="loading" stripe>
            <el-table-column prop="wbsCode" label="编码" width="100" />
            <el-table-column prop="name" label="名称" min-width="120" />
            <el-table-column prop="productName" label="产品" width="100" />
            <el-table-column prop="moduleName" label="模块" width="80" />
            <el-table-column prop="priority" label="优先级" width="80">
              <template #default="{ row }">
                <el-tag v-if="row.priority" :type="priorityTagType(row.priority)" size="small">{{ priorityLabel(row.priority) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="techDifficulty" label="技术难度" width="90">
              <template #default="{ row }">
                <el-tag v-if="row.techDifficulty" :type="difficultyTagType(row.techDifficulty)" size="small">{{ difficultyLabel(row.techDifficulty) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="wbsStatusTagType(row.status)" size="small">{{ wbsStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="latestPlannedEndDate" label="计划完成" width="110" />
            <el-table-column prop="actualEndDate" label="实际完成" width="110" />
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-tooltip content="查看" placement="top">
                  <el-button link type="primary" @click="$router.push(`/pm/wbs/detail/${row.id}`)">
                    <el-icon><View /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-tooltip v-if="row.status === 'PLANNED'" content="编辑" placement="top">
                  <el-button link type="primary" @click="$router.push(`/pm/wbs/form/${row.id}`)">
                    <el-icon><Edit /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-tooltip v-if="row.status === 'PLANNED' || row.status === 'SUSPENDED'" content="删除" placement="top">
                  <el-button link type="danger" @click="handleDelete(row.id)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-tooltip v-if="row.status === 'IN_PROGRESS'" content="暂停" placement="top">
                  <el-button link type="warning" @click="handleSuspend(row.id)">
                    <el-icon><VideoPause /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-tooltip v-if="row.status === 'SUSPENDED'" content="恢复" placement="top">
                  <el-button link type="success" @click="handleResume(row.id)">
                    <el-icon><VideoPlay /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-tooltip v-if="row.status === 'COMPLETED'" content="重新打开" placement="top">
                  <el-button link type="info" @click="handleReopen(row.id)">
                    <el-icon><RefreshRight /></el-icon>
                  </el-button>
                </el-tooltip>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- Pagination -->
      <el-pagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[5, 10, 20]"
        layout="total, sizes, prev, pager, next"
        @current-change="loadProjects"
        @size-change="loadProjects"
        style="margin-top: 16px; justify-content: flex-end;"
      />
    </el-card>

    <!-- Import Dialog -->
    <ImportDialog ref="importDialogRef" @refresh="handleExpandProject(expandedProjectId)" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onActivated } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProjectsForWbsApi, getWbsListApi, deleteWbsApi, suspendWbsApi, resumeWbsApi, reopenWbsApi, exportWbsApi } from '@/api/pm/wbs'
import { ElMessage, ElMessageBox } from 'element-plus'
import { View, Edit, Delete, Plus, Upload, Download, ArrowRight, VideoPause, VideoPlay, RefreshRight } from '@element-plus/icons-vue'
import ImportDialog from './ImportDialog.vue'

const route = useRoute()
const router = useRouter()

const projectList = ref([])
const total = ref(0)
const loading = ref(false)
const expandedProjectId = ref(null)
const wbsTreeData = ref([])
const keyword = ref('')
const importDialogRef = ref(null)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 5
})

const loadProjects = async () => {
  loading.value = true
  try {
    const res = await getProjectsForWbsApi(queryParams)
    projectList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch {
    ElMessage.error('加载项目失败')
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  keyword.value = ''
  queryParams.pageNum = 1
  loadProjects()
}

const toggleProject = (projectId) => {
  if (expandedProjectId.value === projectId) {
    expandedProjectId.value = null
    wbsTreeData.value = []
  } else {
    handleExpandProject(projectId)
  }
}

const handleExpandProject = async (projectId) => {
  expandedProjectId.value = projectId
  wbsTreeData.value = []
  loading.value = true
  try {
    const res = await getWbsListApi({ projectId, keyword: keyword.value })
    wbsTreeData.value = res.data || []
  } catch {
    ElMessage.error('加载WBS数据失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = (projectId) => {
  router.push(`/pm/wbs/form?projectId=${projectId}`)
}

const handleImport = (projectId) => {
  importDialogRef.value?.open(projectId)
}

const handleExport = async (projectId) => {
  try {
    const res = await exportWbsApi(projectId)
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `wbs_export_${projectId}.csv`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('导出失败')
  }
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确认删除该WBS节点？存在子节点的节点不可删除', '提示', { type: 'warning' })
  await deleteWbsApi(id)
  ElMessage.success('删除成功')
  handleExpandProject(expandedProjectId.value)
}

const handleSuspend = async (id) => {
  await ElMessageBox.confirm('确认暂停该WBS？', '提示', { type: 'warning' })
  await suspendWbsApi(id)
  ElMessage.success('已暂停')
  handleExpandProject(expandedProjectId.value)
}

const handleResume = async (id) => {
  await resumeWbsApi(id)
  ElMessage.success('已恢复')
  handleExpandProject(expandedProjectId.value)
}

const handleReopen = async (id) => {
  await ElMessageBox.confirm('确认重新打开该WBS？', '提示', { type: 'warning' })
  await reopenWbsApi(id)
  ElMessage.success('已重新打开')
  handleExpandProject(expandedProjectId.value)
}

// Status and tag helpers
const statusTagType = (status) => {
  const map = { DRAFT: 'info', PENDING_APPROVAL: 'warning', APPROVED: 'success', REJECTED: 'danger', CLOSED: '' }
  return map[status] || 'info'
}
const statusLabel = (status) => {
  const map = { DRAFT: '草稿', PENDING_APPROVAL: '审批中', APPROVED: '已通过', REJECTED: '已驳回', CLOSED: '已关闭' }
  return map[status] || status
}
const wbsStatusTagType = (status) => {
  const map = { PLANNED: 'info', IN_PROGRESS: 'warning', COMPLETED: 'success', SUSPENDED: 'danger' }
  return map[status] || 'info'
}
const wbsStatusLabel = (status) => {
  const map = { PLANNED: '计划中', IN_PROGRESS: '进行中', COMPLETED: '已完成', SUSPENDED: '已暂停' }
  return map[status] || status
}
const priorityTagType = (p) => {
  const map = { P0: 'danger', P1: 'danger', P2: 'warning', P3: '', P4: 'info', P5: 'info' }
  return map[p] || 'info'
}
const priorityLabel = (p) => {
  return p || ''
}
const difficultyTagType = (d) => {
  const map = { HIGH: 'danger', MEDIUM: 'warning', LOW: 'success' }
  return map[d] || 'info'
}
const difficultyLabel = (d) => {
  const map = { HIGH: '高', MEDIUM: '中', LOW: '低' }
  return map[d] || d
}

// Check for refresh signal from form page
if (route.query.refresh && expandedProjectId.value) {
  handleExpandProject(expandedProjectId.value)
}

onMounted(loadProjects)
onActivated(() => {
  if (route.query.refresh && expandedProjectId.value) {
    handleExpandProject(expandedProjectId.value)
  } else {
    loadProjects()
  }
})
</script>
