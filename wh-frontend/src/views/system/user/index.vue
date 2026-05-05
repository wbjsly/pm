<template>
  <div class="user-list">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span style="font-weight: bold; font-size: 16px;">用户管理</span>
          <div style="display: flex; align-items: center; gap: 8px;">
            <el-input v-model="queryParams.keyword" placeholder="用户名/姓名" clearable style="width: 160px" />
            <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 100px">
              <el-option label="启用" value="1" />
              <el-option label="禁用" value="0" />
            </el-select>
            <el-button type="primary" @click="loadData">查询</el-button>
            <el-button @click="resetQuery">重置</el-button>
          </div>
        </div>
      </template>

      <!-- Table -->
      <el-table :data="tableData" :key="tableKey" row-key="id" v-loading="loading" stripe>
        <el-table-column prop="username" label="账号" width="130" />
        <el-table-column prop="nickName" label="昵称" width="120" />
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column prop="email" label="邮箱" width="180" />
        <el-table-column prop="phone" label="手机" width="140" />
        <el-table-column label="角色" width="160">
          <template #default="{ row }">
            <el-tag v-for="role in row.roles" :key="role" size="small" style="margin-right: 4px">{{ role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '1' ? 'success' : 'danger'" size="small">{{ row.status === '1' ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-tooltip content="查看" placement="top">
              <el-button link type="primary" @click="handleView(row)">
                <el-icon><View /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="编辑" placement="top">
              <el-button link type="primary" @click="handleEdit(row)">
                <el-icon><Edit /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="重置密码" placement="top">
              <el-button link type="info" @click="handleResetPassword(row)">
                <el-icon><RefreshRight /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip :content="row.status === '1' ? '停用' : '启用'" placement="top">
              <el-button link :type="row.status === '1' ? 'warning' : 'success'" @click="handleToggleStatus(row)">
                <el-icon><SwitchButton /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button link type="danger" @click="handleDelete(row)">
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

    <!-- Reset Password Dialog -->
    <el-dialog v-model="passwordDialogVisible" title="重置密码" width="400px">
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="80px">
        <el-form-item label="新密码" prop="password">
          <el-input v-model="passwordForm.password" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordLoading" @click="confirmResetPassword">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import { getUserListApi, deleteUserApi, resetPasswordApi, updateUserApi } from '@/api/system/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { View, Edit, RefreshRight, Delete, SwitchButton } from '@element-plus/icons-vue'

const router = useRouter()

const loading = ref(false)
const tableData = ref([])
const tableKey = ref(0)
const total = ref(0)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: ''
})

const passwordDialogVisible = ref(false)
const passwordLoading = ref(false)
const passwordFormRef = ref(null)
const currentUserId = ref('')
const passwordForm = reactive({ password: '', confirmPassword: '' })
const passwordRules = {
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

onMounted(loadData)
onActivated(loadData)

async function loadData() {
  loading.value = true
  try {
    const res = await getUserListApi(queryParams)
    tableData.value = res.data.records || []
    tableKey.value++
    total.value = res.data.total || 0
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  queryParams.keyword = ''
  queryParams.status = ''
  queryParams.pageNum = 1
  loadData()
}

function handleView(row) {
  router.push(`/system/user/detail/${row.id}`)
}

function handleEdit(row) {
  router.push(`/system/user/edit/${row.id}`)
}

function handleResetPassword(row) {
  currentUserId.value = row.id
  passwordForm.password = ''
  passwordForm.confirmPassword = ''
  passwordDialogVisible.value = true
}

async function confirmResetPassword() {
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return
  passwordLoading.value = true
  try {
    await resetPasswordApi(currentUserId.value, passwordForm.password)
    ElMessage.success('密码重置成功')
    passwordDialogVisible.value = false
  } catch {
    // handled by interceptor
  } finally {
    passwordLoading.value = false
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定要删除用户 "${row.realName || row.username}" 吗？`, '提示', { type: 'warning' })
  await deleteUserApi(row.id)
  ElMessage.success('删除成功')
  loadData()
}

async function handleToggleStatus(row) {
  const newStatus = row.status === '1' ? '0' : '1'
  const action = newStatus === '1' ? '启用' : '停用'
  await ElMessageBox.confirm(`确定要${action}用户 "${row.realName || row.username}" 吗？`, '提示', { type: 'warning' })
  await updateUserApi(row.id, { status: newStatus })
  ElMessage.success(`${action}成功`)
  loadData()
}
</script>
