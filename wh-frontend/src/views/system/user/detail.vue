<template>
  <div class="user-detail">
    <el-card v-loading="loading">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span style="font-weight: bold; font-size: 16px;">用户详情</span>
          <el-button @click="handleBack">返回</el-button>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="账号">{{ userData.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ userData.nickName }}</el-descriptions-item>
        <el-descriptions-item label="真实姓名" :span="2">{{ userData.realName }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ userData.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机">{{ userData.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="userData.status === '1' ? 'success' : 'danger'">{{ userData.status === '1' ? '启用' : '禁用' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="角色" :span="2">
          <el-tag v-for="role in userData.roles" :key="role" style="margin-right: 8px">{{ role }}</el-tag>
          <span v-if="!userData.roles || userData.roles.length === 0">无</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onActivated } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getUserDetailApi } from '@/api/system/user'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const userData = ref({})

const loadUserData = async () => {
  loading.value = true
  try {
    const res = await getUserDetailApi(route.params.id)
    userData.value = res.data
  } catch {
    ElMessage.error('加载详情失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadUserData)
onActivated(loadUserData)

function handleBack() {
  router.push('/system/user')
}
</script>
