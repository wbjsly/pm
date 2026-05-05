<template>
  <div class="wbs-form">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span style="font-weight: bold; font-size: 16px;">{{ isEdit ? '编辑WBS' : '新增WBS' }}</span>
          <el-button @click="handleCancel">返回</el-button>
        </div>
      </template>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入WBS名称" />
        </el-form-item>

        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>

        <el-form-item v-if="isEdit" label="WBS编码">
          <el-input :value="detail?.wbsCode" disabled placeholder="系统自动生成" />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="产品" prop="productId">
              <el-select v-model="form.productId" placeholder="选择产品" filterable clearable style="width: 100%" @change="onProductChange">
                <el-option v-for="p in productList" :key="p.id" :label="p.productName" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模块" prop="moduleId">
              <el-select v-model="form.moduleId" placeholder="选择模块" filterable clearable style="width: 100%">
                <el-option v-for="m in moduleList" :key="m.id" :label="m.moduleName" :value="m.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="优先级" prop="priority">
              <el-select v-model="form.priority" placeholder="选择优先级" style="width: 100%">
                <el-option v-for="p in priorityOptions" :key="p.value" :label="p.label" :value="p.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="技术难度" prop="techDifficulty">
              <el-select v-model="form.techDifficulty" placeholder="选择技术难度" style="width: 100%">
                <el-option label="高" value="HIGH" />
                <el-option label="中" value="MEDIUM" />
                <el-option label="低" value="LOW" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="估算工时" prop="effortEstimate">
              <el-input-number v-model.number="form.effortEstimate" :min="0" :precision="2" style="width: 180px" />
              <span style="margin-left: 8px; color: #999;">小时</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="估算成本" prop="budgetEstimate">
              <el-input-number v-model.number="form.budgetEstimate" :min="0" :precision="2" style="width: 180px" />
              <span style="margin-left: 8px; color: #999;">元</span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="计划开始日期">
              <el-date-picker v-model="form.plannedStartDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计划完成日期">
              <el-date-picker v-model="form.plannedEndDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" :disabledDate="endDateDisabled" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item style="margin-top: 24px;">
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
          <el-button @click="handleCancel">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onActivated } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createWbsApi, updateWbsApi, getWbsDetailApi, getAllProductsApi, getModulesByProductApi } from '@/api/pm/wbs'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const submitLoading = ref(false)
const isEdit = computed(() => !!route.params.id)
const detail = ref(null)
const productList = ref([])
const moduleList = ref([])

const priorityOptions = [
  { label: 'P0', value: 'P0' },
  { label: 'P1', value: 'P1' },
  { label: 'P2', value: 'P2' },
  { label: 'P3', value: 'P3' },
  { label: 'P4', value: 'P4' },
  { label: 'P5', value: 'P5' }
]

const form = reactive({
  name: '',
  productId: '',
  moduleId: '',
  priority: '',
  techDifficulty: '',
  effortEstimate: null,
  budgetEstimate: null,
  plannedStartDate: '',
  plannedEndDate: '',
  description: '',
  projectId: '',
  parentId: ''
})

const rules = {
  name: [{ required: true, message: '请输入WBS名称', trigger: 'blur' }],
  productId: [{ required: true, message: '请选择产品', trigger: 'change' }],
  effortEstimate: [{ required: true, message: '请输入估算工时', trigger: 'blur' }],
  budgetEstimate: [{ required: true, message: '请输入估算成本', trigger: 'blur' }]
}

const endDateDisabled = (date) => {
  if (!form.plannedStartDate) return false
  return date.getTime() < new Date(form.plannedStartDate + 'T00:00:00').getTime()
}

watch(() => form.plannedStartDate, () => {
  if (form.plannedEndDate) {
    formRef.value?.validateField('plannedEndDate')
  }
})

const onProductChange = async (productId) => {
  form.moduleId = ''
  moduleList.value = []
  if (productId) {
    try {
      const res = await getModulesByProductApi(productId)
      moduleList.value = res.data || []
    } catch {
      ElMessage.error('加载模块列表失败')
    }
  }
}

const loadProducts = async () => {
  try {
    const res = await getAllProductsApi()
    productList.value = res.data || []
  } catch {
    ElMessage.error('加载产品列表失败')
  }
}

const loadDetail = async () => {
  if (!route.params.id) return
  const res = await getWbsDetailApi(route.params.id)
  detail.value = res.data
  Object.assign(form, {
    name: res.data.name,
    productId: res.data.productId,
    moduleId: res.data.moduleId,
    priority: res.data.priority,
    techDifficulty: res.data.techDifficulty,
    effortEstimate: res.data.effortEstimate ? parseFloat(res.data.effortEstimate) : null,
    budgetEstimate: res.data.budgetEstimate ? parseFloat(res.data.budgetEstimate) : null,
    plannedStartDate: res.data.latestPlannedEndDate,
    plannedEndDate: res.data.latestPlannedEndDate,
    description: res.data.description
  })
  // Load modules for selected product
  if (res.data.productId) {
    onProductChange(res.data.productId)
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateWbsApi(route.params.id, form)
      ElMessage.success('更新成功')
    } else {
      // Set project/parent from query params
      form.projectId = route.query.projectId || form.projectId
      form.parentId = route.query.parentId || form.parentId
      await createWbsApi(form)
      ElMessage.success('创建成功')
    }
    router.push({ path: '/pm/wbs', query: { refresh: Date.now() } })
  } catch {
    ElMessage.error('保存失败')
  } finally {
    submitLoading.value = false
  }
}

const handleCancel = () => {
  router.back()
}

const initForm = () => {
  if (route.params.id) {
    loadDetail()
  } else {
    form.projectId = route.query.projectId || ''
    form.parentId = route.query.parentId || ''
  }
}

onMounted(() => {
  loadProducts()
  initForm()
})
onActivated(initForm)
</script>
