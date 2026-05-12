<template>
  <div class="deliverable-list">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px;">
          <span style="font-weight: bold; font-size: 16px;">成果管理</span>
          <div style="display: flex; align-items: center; gap: 8px;">
            <el-select v-model="progressFilter" multiple collapse-tags collapse-tags-tooltip placeholder="项目进度" clearable style="width: 180px">
              <el-option v-for="item in dictStore.getDictItems('CHARTER_PROGRESS')" :key="item.itemCode" :label="item.label" :value="item.itemCode" />
            </el-select>
            <el-input v-model="keyword" placeholder="搜索项目名称/简称" clearable style="width: 180px" @keyup.enter="loadProjects" />
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
          <span style="font-weight: bold; margin-right: 24px; font-size: 13px;">{{ project.projectName }}{{ project.projectShortName ? '（' + project.projectShortName + '）' : '' }}</span>
          <span v-if="project.pmName" style="margin-right: 24px; color: #666; font-size: 12px;">
            <el-icon style="vertical-align: -2px;"><User /></el-icon> {{ project.pmName }}
          </span>
          <el-tag size="small" :type="dictStore.getTagType('CHARTER_PROGRESS', project.progress)">{{ dictStore.getLabel('CHARTER_PROGRESS', project.progress) }}</el-tag>
          <span v-if="project.endDate" style="margin-left: 24px; color: #666; font-size: 12px;">完成日期: {{ project.endDate }}</span>
          <span v-if="deliverableCounts[project.id] != null" style="margin-left: 24px; color: #666; font-size: 12px;">成果物: {{ deliverableCounts[project.id] }}个</span>
          <span v-if="attachmentCounts[project.id] != null" style="margin-left: 16px; color: #666; font-size: 12px;">附件: {{ attachmentCounts[project.id] }}个</span>
          <span style="margin-left: auto; display: flex; gap: 8px;" v-if="expandedProjectId === project.id">
            <el-tooltip content="新增成果物" placement="top">
              <el-button link type="primary" size="small" @click.stop="$router.push(`/pm/deliverable/form?projectId=${project.id}`)">
                <el-icon><Plus /></el-icon>
              </el-button>
            </el-tooltip>
          </span>
        </div>

        <!-- Deliverable table for expanded project -->
        <div v-if="expandedProjectId === project.id" style="padding: 12px 0;">
          <el-table :data="deliverableData" row-key="id" v-loading="loading" stripe>
            <el-table-column prop="deliverableCode" label="成果物编号" width="220" />
            <el-table-column prop="name" label="名称" min-width="140" />
            <el-table-column label="附件数量" width="80" align="center">
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
                             @click="handleDownload(row.id, idx, att.fileName)">
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
                <el-tag :type="dictStore.getTagType('DELIVERABLE_STATUS', row.status)">{{ dictStore.getLabel('DELIVERABLE_STATUS', row.status) }}</el-tag>
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
        </div>
      </div>

      <!-- Empty state -->
      <el-empty v-if="!loading && projectList.length === 0" description="暂无数据" />

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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onActivated } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getDeliverableListApi, deleteDeliverableApi, submitDeliverableApi, deleteDeliverableAttachmentApi, downloadDeliverableAttachmentApi } from '@/api/pm/deliverable'
import { getCharterListApi } from '@/api/pm/charter'
import { ElMessage, ElMessageBox } from 'element-plus'
import { View, Edit, Promotion, Delete, Plus, Paperclip, Document, Close, ArrowRight, User } from '@element-plus/icons-vue'

import { useDictStore } from '@/store/dict'

const router = useRouter()
const route = useRoute()
const dictStore = useDictStore()

const projectList = ref([])
const total = ref(0)
const loading = ref(false)
const expandedProjectId = ref(null)
const deliverableData = ref([])
const deliverableCounts = ref({})
const attachmentCounts = ref({})
const keyword = ref('')
const progressFilter = ref(['IN_PROGRESS', 'ACCEPTED'])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 5
})

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

const loadProjects = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize,
      status: 'APPROVED'
    }
    if (progressFilter.value && progressFilter.value.length > 0) {
      params.progress = progressFilter.value.join(',')
    }
    if (keyword.value) {
      params.keyword = keyword.value
    }
    const res = await getCharterListApi(params)
    projectList.value = res.data.records || []
    total.value = res.data.total || 0

    // Fetch deliverable + attachment counts for each project
    if (projectList.value.length > 0) {
      const dCounts = {}
      const aCounts = {}
      await Promise.all(projectList.value.map(async (p) => {
        try {
          const r = await getDeliverableListApi({ projectId: p.id, pageSize: 999 })
          const records = r.data.records || []
          dCounts[p.id] = r.data.total || 0
          aCounts[p.id] = records.reduce((sum, d) => sum + getAttachments(d).length, 0)
        } catch {
          dCounts[p.id] = 0
          aCounts[p.id] = 0
        }
      }))
      deliverableCounts.value = dCounts
      attachmentCounts.value = aCounts
    }
  } catch {
    ElMessage.error('加载项目失败')
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  keyword.value = ''
  progressFilter.value = ['IN_PROGRESS', 'ACCEPTED']
  queryParams.pageNum = 1
  expandedProjectId.value = null
  deliverableData.value = []
  loadProjects()
}

const toggleProject = (projectId) => {
  if (expandedProjectId.value === projectId) {
    expandedProjectId.value = null
    deliverableData.value = []
  } else {
    handleExpandProject(projectId)
  }
}

const handleExpandProject = async (projectId) => {
  expandedProjectId.value = projectId
  deliverableData.value = []
  loading.value = true
  try {
    const res = await getDeliverableListApi({ projectId, pageSize: 999 })
    deliverableData.value = res.data.records || []
  } catch {
    ElMessage.error('加载成果物数据失败')
  } finally {
    loading.value = false
  }
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确认删除该成果物？', '提示', { type: 'warning' })
  await deleteDeliverableApi(id)
  ElMessage.success('删除成功')
  if (expandedProjectId.value) {
    handleExpandProject(expandedProjectId.value)
    refreshProjectCounts(expandedProjectId.value)
  }
}

const handleSubmit = async (id) => {
  await ElMessageBox.confirm('确认提交审批？', '提示', { type: 'warning' })
  await submitDeliverableApi(id)
  ElMessage.success('提交成功')
  if (expandedProjectId.value) {
    handleExpandProject(expandedProjectId.value)
  }
}

const handleDownload = async (deliverableId, index, filename) => {
  try {
    const blob = await downloadDeliverableAttachmentApi(deliverableId, index)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', filename)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('下载附件失败')
  }
}

const handleDeleteAttachment = async (row, index) => {
  try {
    await ElMessageBox.confirm('确认删除该附件？', '提示', { type: 'warning' })
    const res = await deleteDeliverableAttachmentApi(row.id, index)
    if (res.code === 200) {
      row.attachments = JSON.stringify(res.data)
      ElMessage.success('删除成功')
      refreshProjectCounts(expandedProjectId.value)
    }
  } catch {
    // cancelled or error
  }
}

const refreshProjectCounts = async (projectId) => {
  try {
    const r = await getDeliverableListApi({ projectId, pageSize: 999 })
    const records = r.data.records || []
    deliverableCounts.value[projectId] = r.data.total || 0
    attachmentCounts.value[projectId] = records.reduce((sum, d) => sum + getAttachments(d).length, 0)
  } catch { /* ignore */ }
}

// Keep expanded project state when returning from form/detail pages
if (route.query.refresh && route.query.projectId) {
  handleExpandProject(route.query.projectId)
}

onMounted(loadProjects)
onActivated(() => {
  if (route.query.refresh && route.query.projectId) {
    handleExpandProject(route.query.projectId)
    refreshProjectCounts(route.query.projectId)
  } else {
    loadProjects()
  }
})
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
