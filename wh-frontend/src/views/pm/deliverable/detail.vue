<template>
  <div class="deliverable-detail">
    <el-card v-loading="loading">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span style="font-weight: bold; font-size: 16px;">成果物详情</span>
          <div style="display: flex; gap: 8px;">
            <template v-if="deliverable.status === 'PENDING_APPROVAL' && isSponsor">
              <el-button type="success" @click="handleApprove">
                <el-icon><Select /></el-icon> 审批通过
              </el-button>
              <el-button type="danger" @click="handleReject">
                <el-icon><Close /></el-icon> 驳回
              </el-button>
            </template>
            <el-button v-if="deliverable.status === 'APPROVED' && isPm" type="success" @click="handleDeliver">
              <el-icon><Checked /></el-icon> 标记已交付
            </el-button>
            <el-button @click="$router.push('/pm/deliverable')">返回</el-button>
          </div>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="成果物编号">{{ deliverable.deliverableCode }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(deliverable.status)">{{ statusLabel(deliverable.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="名称">{{ deliverable.name }}</el-descriptions-item>
        <el-descriptions-item label="所属项目">{{ projectName }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ deliverable.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="计划交付日期">{{ deliverable.plannedDeliveryDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="实际交付日期">{{ deliverable.actualDeliveryDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建人">{{ deliverable.createByName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发起人">{{ deliverable.sponsorName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批意见" :span="2">{{ deliverable.approvalComment || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ deliverable.remarks || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div v-if="attachments.length > 0" style="margin-top: 20px;">
        <h4 style="margin-bottom: 12px;">附件 ({{ attachments.length }})</h4>
        <div v-for="(att, idx) in attachments" :key="idx" style="display: flex; align-items: center; gap: 12px; margin-bottom: 8px;">
          <el-icon><Document /></el-icon>
          <span>{{ att.fileName }}</span>
          <span style="color: #999; font-size: 12px;">{{ formatFileSize(att.fileSize) }}</span>
          <span style="color: #999; font-size: 12px;">{{ att.uploadTime }}</span>
          <el-button link type="primary" @click="handleDownload(idx)">下载</el-button>
        </div>
      </div>
    </el-card>

    <!-- Approve/Reject dialogs -->
    <el-dialog v-model="dialog.visible" :title="dialog.title" width="400px">
      <el-input v-model="dialog.comment" type="textarea" :placeholder="dialog.placeholder" />
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button :type="dialog.confirmType" @click="dialog.onConfirm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import {
  getDeliverableDetailApi, approveDeliverableApi, rejectDeliverableApi,
  deliverDeliverableApi, getDeliverableAttachmentUrlApi
} from '@/api/pm/deliverable'
import { getCharterDetailApi } from '@/api/pm/charter'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Select, Close, Checked, Document } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const roles = computed(() => userStore.userInfo?.roles || [])
const isPm = computed(() => roles.value.some(r => r.toLowerCase().includes('role_pm')))
const isSponsor = computed(() => roles.value.some(r => r.toLowerCase().includes('role_sponsor')))

const deliverable = ref({})
const attachments = ref([])
const projectName = ref('')
const loading = ref(false)

const statusTagType = (status) => {
  const map = { DRAFT: 'info', PENDING_APPROVAL: 'warning', APPROVED: 'success', DELIVERED: '', REJECTED: 'danger' }
  return map[status] || 'info'
}
const statusLabel = (status) => {
  const map = { DRAFT: '草稿', PENDING_APPROVAL: '审批中', APPROVED: '已通过', DELIVERED: '已交付', REJECTED: '已驳回' }
  return map[status] || status
}

const formatFileSize = (bytes) => {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const loadDetail = async () => {
  if (!route.params.id) return
  loading.value = true
  try {
    const res = await getDeliverableDetailApi(route.params.id)
    if (res.code === 200) {
      deliverable.value = res.data

      if (res.data.attachments) {
        try { attachments.value = JSON.parse(res.data.attachments) } catch { attachments.value = [] }
      } else {
        attachments.value = []
      }
      loadProjectName(res.data.projectId)
    }
  } catch {
    ElMessage.error('加载详情失败')
    router.back()
  } finally {
    loading.value = false
  }
}

const loadProjectName = async (projectId) => {
  try {
    const res = await getCharterDetailApi(projectId)
    if (res.code === 200) {
      projectName.value = res.data.projectName
    }
  } catch { /* ignore */ }
}

const handleDownload = async (index) => {
  try {
    const res = await getDeliverableAttachmentUrlApi(route.params.id, index)
    if (res.code === 200) {
      window.open(res.data, '_blank')
    }
  } catch {
    ElMessage.error('获取下载链接失败')
  }
}

const dialog = reactive({
  visible: false, title: '', placeholder: '', confirmType: 'primary',
  comment: '', mode: '', onConfirm: () => {}
})

const showDialog = (mode) => {
  if (mode === 'approve') {
    dialog.title = '审批通过'
    dialog.placeholder = '审批意见（可选）'
    dialog.confirmType = 'success'
    dialog.mode = 'approve'
  } else {
    dialog.title = '驳回'
    dialog.placeholder = '驳回原因'
    dialog.confirmType = 'danger'
    dialog.mode = 'reject'
  }
  dialog.comment = ''
  dialog.visible = true
}

dialog.onConfirm = async () => {
  try {
    if (dialog.mode === 'approve') {
      await approveDeliverableApi(route.params.id, { comment: dialog.comment })
      ElMessage.success('审批通过')
    } else {
      await rejectDeliverableApi(route.params.id, { rejectReason: dialog.comment })
      ElMessage.success('已驳回')
    }
    dialog.visible = false
    loadDetail()
  } catch {
    ElMessage.error('操作失败')
  }
}

const handleApprove = () => showDialog('approve')
const handleReject = () => showDialog('reject')

const handleDeliver = async () => {
  try {
    await ElMessageBox.confirm('确定标记为已交付吗？', '确认', { type: 'info' })
    await deliverDeliverableApi(route.params.id)
    ElMessage.success('已标记为已交付')
    loadDetail()
  } catch {
    // cancelled or error
  }
}

onMounted(loadDetail)
watch(() => route.params.id, (newId) => { if (newId && route.path.includes('/pm/deliverable/detail')) loadDetail() })
</script>

<style scoped>
.deliverable-detail {
  padding: 8px;
}
</style>
