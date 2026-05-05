<template>
  <el-dialog v-model="visible" title="批量导入WBS" width="600px">
    <el-upload
      drag
      :auto-upload="false"
      :on-change="handleFileSelect"
      accept=".csv"
      :limit="1"
    >
      <el-icon class="el-icon--upload" size="48"><UploadFilled /></el-icon>
      <div class="el-upload__text">点击或拖拽文件到此区域</div>
      <template #tip>
        <div style="text-align: center; margin-top: 8px;">
          <el-link type="primary" @click.stop="handleDownloadTemplate">下载导入模板</el-link>
        </div>
      </template>
    </el-upload>

    <!-- Import result -->
    <div v-if="importResult" style="margin-top: 16px;">
      <el-row :gutter="16" style="margin-bottom: 16px;">
        <el-col :span="8">
          <el-statistic title="成功" :value="importResult.success">
            <template #prefix><span style="color: #67C23A;">✓</span></template>
          </el-statistic>
        </el-col>
        <el-col :span="8">
          <el-statistic title="降级" :value="importResult.degraded">
            <template #prefix><span style="color: #E6A23C;">⚠</span></template>
          </el-statistic>
        </el-col>
        <el-col :span="8">
          <el-statistic title="失败" :value="importResult.failed">
            <template #prefix><span style="color: #F56C6C;">✗</span></template>
          </el-statistic>
        </el-col>
      </el-row>

      <el-table v-if="importResult.details && importResult.details.length > 0" :data="importResult.details" size="small" max-height="200">
        <el-table-column prop="row" label="行号" width="60" />
        <el-table-column prop="name" label="名称" min-width="100" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : row.status === 'DEGRADED' ? 'warning' : 'danger'" size="small">
              {{ row.status === 'SUCCESS' ? '成功' : row.status === 'DEGRADED' ? '降级' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="150" />
      </el-table>
    </div>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button type="primary" :loading="uploading" @click="handleUpload" :disabled="!selectedFile">导入</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { importWbsApi, downloadWbsTemplateApi } from '@/api/pm/wbs'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'

const emit = defineEmits(['refresh'])
const visible = ref(false)
const uploading = ref(false)
const selectedFile = ref(null)
const importResult = ref(null)
const projectId = ref('')

const open = (pid) => {
  projectId.value = pid
  selectedFile.value = null
  importResult.value = null
  visible.value = true
}

defineExpose({ open })

const handleFileSelect = (file) => {
  selectedFile.value = file.raw
  importResult.value = null
}

const handleDownloadTemplate = async () => {
  try {
    const res = await downloadWbsTemplateApi()
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', 'wbs_import_template.csv')
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('下载模板失败')
  }
}

const handleUpload = async () => {
  if (!selectedFile.value) return
  uploading.value = true
  try {
    const res = await importWbsApi(selectedFile.value, projectId.value)
    importResult.value = res.data
    ElMessage.success('导入完成')
    emit('refresh')
  } catch {
    ElMessage.error('导入失败')
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.el-icon--upload {
  color: #c0c4cc;
}
</style>
