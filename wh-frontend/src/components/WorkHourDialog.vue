<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑工时' : '录入工时 - ' + dateStr"
    width="500px"
    @close="handleClose"
  >
    <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
      <el-form-item label="项目" prop="projectId">
        <el-select v-model="form.projectId" filterable placeholder="请选择项目" style="width: 100%">
          <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="工时" prop="hoursWorked">
        <el-input-number v-model="form.hoursWorked" :min="0.5" :max="24" :step="0.5" placeholder="小时" />
      </el-form-item>
      <el-form-item label="描述" prop="workDescription">
        <el-input v-model="form.workDescription" type="textarea" :rows="3" placeholder="工作内容描述" />
      </el-form-item>
    </el-form>

    <!-- Existing entries for this day -->
    <div v-if="existingEntries.length > 0" class="existing-entries">
      <div class="existing-title">当日已录工时:</div>
      <div v-for="entry in existingEntries" :key="entry.id" class="existing-item" :class="'status-' + entry.status.toLowerCase()">
        <span class="existing-project">{{ entry.projectShortName }}: {{ entry.hoursWorked }}h</span>
        <span class="existing-status">{{ statusLabel(entry.status) }}</span>
        <el-button v-if="entry.status === 'DRAFT' || entry.status === 'REJECTED'"
          link type="danger" size="small" @click="handleDelete(entry)">删除</el-button>
        <el-button v-if="entry.status === 'REJECTED'"
          link type="primary" size="small" @click="handleResubmit(entry)">重新提交</el-button>
      </div>
      <div class="daily-total">当日累计: {{ dailyTotal }}h / 24h</div>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createWorkHourApi, updateWorkHourApi, deleteWorkHourApi, resubmitWorkHourApi, getWorkHoursApi } from '@/api/pm/workHours'
import { getCharterListApi } from '@/api/pm/charter'
import { useUserStore } from '@/store/user'

const props = defineProps({
  dateStr: { type: String, default: '' },
  editEntry: { type: Object, default: null }
})

const emit = defineEmits(['save', 'delete', 'resubmit'])

const visible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const userStore = useUserStore()
const projects = ref([])
const existingEntries = ref([])

const isEdit = computed(() => !!props.editEntry)

const form = ref({
  projectId: '',
  hoursWorked: 8,
  workDescription: ''
})

const rules = {
  projectId: [{ required: true, message: '请选择项目', trigger: 'change' }],
  hoursWorked: [{ required: true, message: '请输入工时', trigger: 'blur' }],
  workDescription: [{ required: true, message: '请输入工作描述', trigger: 'blur' }]
}

const dailyTotal = computed(() => {
  let total = existingEntries.value.reduce((s, e) => s + (parseFloat(e.hoursWorked) || 0), 0)
  if (isEdit.value) {
    total -= parseFloat(props.editEntry.hours) || 0
  }
  return total.toFixed(1)
})

const statusLabel = (status) => {
  const map = { DRAFT: '待审批', APPROVED: '已通过', REJECTED: '已驳回' }
  return map[status] || status
}

function open() {
  visible.value = true
}

function handleClose() {
  form.value = { projectId: '', hoursWorked: 8, workDescription: '' }
  existingEntries.value = []
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
    submitting.value = true

    if (isEdit.value) {
      await updateWorkHourApi(props.editEntry.id, {
        projectId: form.value.projectId,
        logDate: props.dateStr,
        hoursWorked: String(form.value.hoursWorked),
        workDescription: form.value.workDescription
      })
      ElMessage.success('更新成功')
    } else {
      await createWorkHourApi({
        projectId: form.value.projectId,
        logDate: props.dateStr,
        hoursWorked: String(form.value.hoursWorked),
        workDescription: form.value.workDescription
      })
      ElMessage.success('保存成功')
    }
    visible.value = false
    emit('save')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('保存失败: ' + (e.message || ''))
  } finally {
    submitting.value = false
  }
}

async function handleDelete(entry) {
  try {
    await ElMessageBox.confirm('确定删除此工时记录吗？', '提示', { type: 'warning' })
    await deleteWorkHourApi(entry.id)
    ElMessage.success('删除成功')
    emit('delete')
    visible.value = false
  } catch (e) {
    // Cancelled
  }
}

async function handleResubmit(entry) {
  await resubmitWorkHourApi(entry.id)
  ElMessage.success('重新提交成功')
  emit('resubmit')
  visible.value = false
}

async function loadProjects() {
  try {
    const res = await getCharterListApi({ pageNum: 1, pageSize: 100 })
    // For now, show all active projects. Backend will enforce permissions.
    projects.value = (res.data?.records || []).filter(p => p.status === 'APPROVED')
  } catch (e) {
    // Ignore
  }
}

async function loadExisting() {
  if (!props.dateStr) return
  try {
    const y = parseInt(props.dateStr.substring(0, 4))
    const m = parseInt(props.dateStr.substring(5, 7))
    const res = await getWorkHoursApi(String(y), m)
    existingEntries.value = (res.data || []).filter(l => l.logDate === props.dateStr)
  } catch (e) {
    // Ignore
  }
}

watch(() => visible.value, async (val) => {
  if (val) {
    await loadProjects()
    await loadExisting()
    if (isEdit.value) {
      form.value = {
        projectId: props.editEntry.projectId || '',
        hoursWorked: parseFloat(props.editEntry.hours) || 8,
        workDescription: ''
      }
    } else {
      form.value = { projectId: '', hoursWorked: 8, workDescription: '' }
    }
  }
})

defineExpose({ open })
</script>

<style scoped>
.existing-entries {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}
.existing-title {
  font-size: 13px;
  font-weight: bold;
  color: #606266;
  margin-bottom: 8px;
}
.existing-item {
  display: flex;
  align-items: center;
  padding: 6px 10px;
  margin: 4px 0;
  border-radius: 4px;
  font-size: 13px;
}
.existing-item.status-draft {
  background: #fdf6ec;
  color: #e6a23c;
}
.existing-item.status-approved {
  background: #f0f9eb;
  color: #67c23a;
}
.existing-item.status-rejected {
  background: #fef0f0;
  color: #f56c6c;
}
.existing-project {
  flex: 1;
}
.existing-status {
  margin-right: 12px;
  font-size: 12px;
}
.daily-total {
  text-align: right;
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
