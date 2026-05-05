<template>
  <div class="user-profile">
    <el-card v-loading="loading">
      <template #header>
        <span style="font-weight: bold; font-size: 16px;">个人资料</span>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="账号">{{ userData.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ userData.nickName }}</el-descriptions-item>
        <el-descriptions-item label="真实姓名" :span="2">{{ userData.realName }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ userData.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机">{{ userData.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="角色" :span="2">
          <el-tag v-for="role in userData.roles" :key="role" style="margin-right: 8px">{{ role }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { getUserDetailApi } from '@/api/system/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const loading = ref(false)
const userData = ref({})

onMounted(async () => {
  loading.value = true
  try {
    const userId = userStore.userInfo?.userId
    if (userId) {
      const res = await getUserDetailApi(userId)
      userData.value = res.data
    }
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
})
</script>
