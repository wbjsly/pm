<template>
  <div class="user-edit">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span style="font-weight: bold; font-size: 16px;">编辑用户</span>
          <el-button @click="handleBack">返回</el-button>
        </div>
      </template>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px" v-loading="loading">
        <el-form-item label="账号">
          <span>{{ form.username }}</span>
        </el-form-item>
        <el-form-item label="昵称" prop="nickName">
          <el-input v-model="form.nickName" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="1">启用</el-radio>
            <el-radio label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="form.roleIds" multiple filterable placeholder="请选择角色" style="width: 100%">
            <el-option v-for="role in allRoles" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="成本岗位" prop="positionId">
          <el-select v-model="form.positionId" filterable placeholder="请选择成本岗位" style="width: 100%">
            <el-option v-for="p in positionList" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
          <el-button @click="handleBack">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onActivated } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getUserDetailApi, updateUserApi } from '@/api/system/user'
import { getPositionListApi } from '@/api/system/costQuota'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const allRoles = ref([])
const positionList = ref([])

const form = reactive({
  username: '',
  nickName: '',
  realName: '',
  email: '',
  phone: '',
  status: '1',
  roleIds: [],
  positionId: ''
})

const rules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  email: [
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号码', trigger: 'blur' }
  ],
  positionId: [
    { required: true, message: '请选择成本岗位', trigger: 'change' }
  ]
}

const loadUserData = async () => {
  loading.value = true
  try {
    const [userRes, rolesRes, posRes] = await Promise.all([
      getUserDetailApi(route.params.id),
      request.get('/system/roles'),
      getPositionListApi()
    ])
    const data = userRes.data
    form.username = data.username || ''
    form.nickName = data.nickName || ''
    form.realName = data.realName || ''
    form.email = data.email || ''
    form.phone = data.phone || ''
    form.status = data.status || '1'
    form.positionId = data.positionId || ''

    allRoles.value = rolesRes.data || []
    positionList.value = posRes.data || []
    if (data.roles && allRoles.value.length > 0) {
      form.roleIds = data.roles.map(code => {
        const role = allRoles.value.find(r => r.roleCode === code)
        return role ? role.id : ''
      }).filter(Boolean)
    }
  } finally {
    loading.value = false
  }
}

onMounted(loadUserData)
onActivated(loadUserData)

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await updateUserApi(route.params.id, {
      nickName: form.nickName,
      realName: form.realName,
      email: form.email,
      phone: form.phone,
      status: form.status,
      roleIds: form.roleIds,
      positionId: form.positionId
    })
    ElMessage.success('保存成功')
    router.push('/system/user')
  } finally {
    submitting.value = false
  }
}

function handleBack() {
  router.push('/system/user')
}
</script>
