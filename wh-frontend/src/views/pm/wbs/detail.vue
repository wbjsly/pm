<template>
  <div class="wbs-detail">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span style="font-weight: bold; font-size: 16px;">WBS详情</span>
          <el-button @click="$router.back()">返回</el-button>
        </div>
      </template>

      <div v-if="detail" style="margin-bottom: 16px;">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="编码">{{ detail.wbsCode }}</el-descriptions-item>
          <el-descriptions-item label="名称">{{ detail.name }}</el-descriptions-item>
          <el-descriptions-item label="产品">{{ detail.productName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="模块">{{ detail.moduleName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="优先级">
            <el-tag v-if="detail.priority" :type="priorityTagType(detail.priority)" size="small">{{ priorityLabel(detail.priority) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="技术难度">
            <el-tag v-if="detail.techDifficulty" :type="difficultyTagType(detail.techDifficulty)" size="small">{{ difficultyLabel(detail.techDifficulty) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="计划责任人">{{ detail.plannedOwnerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="实际负责人">{{ detail.ownerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="估算工时">{{ detail.effortEstimate || '-' }} 小时</el-descriptions-item>
          <el-descriptions-item label="估算成本">{{ detail.budgetEstimate || '-' }} 元</el-descriptions-item>
          <el-descriptions-item label="计划开始">{{ detail.latestVersion?.plannedStartDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="计划完成">{{ detail.latestPlannedEndDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="实际完成">{{ detail.actualEndDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="实际完成人">{{ detail.actualCompletedBy || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="wbsStatusTagType(detail.status)" size="small">{{ wbsStatusLabel(detail.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="版本">
            <el-link v-if="detail.latestVersion" type="primary" @click="$router.push(`/pm/wbs/history/${detail.id}`)">
              版本 {{ detail.latestVersion.versionNumber }} →
            </el-link>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ detail.description || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- Action buttons -->
      <div style="margin-top: 24px; display: flex; gap: 8px;">
        <el-button v-if="detail?.status === 'PLANNED'" type="primary" @click="$router.push(`/pm/wbs/form/${detail.id}`)">
          <el-icon><Edit /></el-icon> 编辑
        </el-button>
        <el-button v-if="detail?.status === 'IN_PROGRESS'" type="warning" @click="handleSuspend">
          <el-icon><VideoPause /></el-icon> 暂停
        </el-button>
        <el-button v-if="detail?.status === 'SUSPENDED'" type="success" @click="handleResume">
          <el-icon><VideoPlay /></el-icon> 恢复
        </el-button>
        <el-button v-if="detail?.status === 'COMPLETED'" type="info" @click="handleReopen">
          <el-icon><RefreshRight /></el-icon> 重新打开
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getWbsDetailApi, suspendWbsApi, resumeWbsApi, reopenWbsApi } from '@/api/pm/wbs'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, VideoPause, VideoPlay, RefreshRight } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const detail = ref(null)

const loadDetail = async () => {
  const res = await getWbsDetailApi(route.params.id)
  detail.value = res.data
}

const handleSuspend = async () => {
  await ElMessageBox.confirm('确认暂停该WBS？', '提示', { type: 'warning' })
  await suspendWbsApi(route.params.id)
  ElMessage.success('已暂停')
  loadDetail()
}

const handleResume = async () => {
  await resumeWbsApi(route.params.id)
  ElMessage.success('已恢复')
  loadDetail()
}

const handleReopen = async () => {
  await ElMessageBox.confirm('确认重新打开该WBS？', '提示', { type: 'warning' })
  await reopenWbsApi(route.params.id)
  ElMessage.success('已重新打开')
  loadDetail()
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

onMounted(loadDetail)
</script>
