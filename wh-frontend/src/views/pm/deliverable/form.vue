<template>
  <div class="deliverable-form">
    <el-card>
      <template #header>
        <span style="font-weight: bold; font-size: 16px;">{{ isEdit ? '编辑成果物' : '新增成果物' }}</span>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" style="max-width: 700px;" :validate-on-rule-change="false">
        <el-form-item label="所属项目" prop="projectId">
          <el-select v-model="form.projectId" placeholder="请选择已审批通过的项目" filterable
                     :disabled="projectDisabled" style="width: 100%">
            <el-option v-for="p in projectOptions" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入成果物名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="计划交付日期" prop="plannedDeliveryDate">
          <el-date-picker v-model="form.plannedDeliveryDate" type="date" placeholder="请选择"
                          value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remarks" type="textarea" :rows="2" placeholder="备注信息" />
        </el-form-item>
        <el-form-item v-if="isEdit" label="附件">
          <div style="width: 100%;">
            <div v-if="attachments.length > 0" style="margin-bottom: 12px;">
              <el-tag v-for="(att, idx) in attachments" :key="idx" closable
                      @close="handleRemoveAttachment(idx)" style="margin-right: 8px; margin-bottom: 4px;">
                {{ att.fileName }}
              </el-tag>
            </div>
            <el-upload :auto-upload="false" :show-file-list="false"
                       :on-change="handleFileSelect" accept="*">
              <el-button type="primary" plain>
                <el-icon><Upload /></el-icon> 上传附件
              </el-button>
            </el-upload>
            <span v-if="uploading" style="margin-left: 8px; color: #409eff;">上传中...</span>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
          <el-button @click="$router.push('/pm/deliverable')">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import {
  getDeliverableDetailApi, createDeliverableApi, updateDeliverableApi,
  uploadDeliverableAttachmentApi, deleteDeliverableAttachmentApi
} from '@/api/pm/deliverable'
import { getCharterListApi } from '@/api/pm/charter'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isEdit = computed(() => !!route.params.id)
const fromProjectId = computed(() => route.query.projectId)
const projectDisabled = computed(() => !!fromProjectId.value || isEdit.value)
const formRef = ref(null)
const saving = ref(false)
const uploading = ref(false)
const projectOptions = ref([])
const attachments = ref([])

const form = reactive({
  projectId: '',
  name: '',
  description: '',
  plannedDeliveryDate: '',
  remarks: ''
})

const rules = {
  projectId: [{ required: true, message: '请选择项目', trigger: 'change' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  plannedDeliveryDate: [{ required: true, message: '请选择计划交付日期', trigger: 'change' }]
}

const loadProjectOptions = async () => {
  try {
    const res = await getCharterListApi({ pageSize: 999, status: 'APPROVED' })
    if (res.code === 200) {
      projectOptions.value = res.data.records || []
    }
  } catch { /* ignore */ }
}

const loadDetail = async () => {
  if (!isEdit.value) return
  try {
    const res = await getDeliverableDetailApi(route.params.id)
    if (res.code === 200) {
      const d = res.data
      form.projectId = d.projectId
      form.name = d.name
      form.description = d.description || ''
      form.plannedDeliveryDate = d.plannedDeliveryDate
      form.remarks = d.remarks || ''
      if (d.attachments) {
        try { attachments.value = JSON.parse(d.attachments).filter(a => !a.deleted) } catch { attachments.value = [] }
      } else {
        attachments.value = []
      }
    }
  } catch {
    ElMessage.error('加载成果物详情失败')
    router.back()
  }
}

const handleFileSelect = async (file) => {
  uploading.value = true
  try {
    const res = await uploadDeliverableAttachmentApi(route.params.id, file.raw)
    if (res.code === 200) {
      attachments.value = res.data || []
      ElMessage.success('上传成功')
    }
  } catch {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}

const handleRemoveAttachment = async (index) => {
  try {
    await ElMessageBox.confirm('确认删除该附件？', '提示', { type: 'warning' })
    const res = await deleteDeliverableAttachmentApi(route.params.id, index)
    if (res.code === 200) {
      attachments.value = res.data || []
      ElMessage.success('删除成功')
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除附件失败')
    }
  }
}

const handleSave = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (isEdit.value) {
      await updateDeliverableApi(route.params.id, {
        name: form.name,
        description: form.description,
        plannedDeliveryDate: form.plannedDeliveryDate,
        remarks: form.remarks
      })
      ElMessage.success('保存成功')
    } else {
      await createDeliverableApi({
        projectId: form.projectId,
        name: form.name,
        description: form.description,
        plannedDeliveryDate: form.plannedDeliveryDate,
        remarks: form.remarks
      })
      ElMessage.success('创建成功')
    }
    const targetId = fromProjectId.value || form.projectId
    router.push(`/pm/deliverable?refresh=true&projectId=${targetId}`)
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const initPage = () => {
  loadProjectOptions()
  if (isEdit.value) {
    loadDetail()
  } else {
    form.projectId = fromProjectId.value || ''
    form.name = ''
    form.description = ''
    form.plannedDeliveryDate = ''
    form.remarks = ''
    attachments.value = []
  }
}

onMounted(initPage)
watch(() => route.params.id, () => { if (route.path.includes('/pm/deliverable/form')) initPage() })
</script>

<style scoped>
.deliverable-form {
  padding: 8px;
}
</style>
