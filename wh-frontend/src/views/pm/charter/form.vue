<template>
  <div class="charter-form">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span style="font-weight: bold; font-size: 16px;">{{ isEdit ? '编辑项目' : '新增项目' }}</span>
          <el-button @click="$router.back()">返回</el-button>
        </div>
      </template>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <!-- Step indicator -->
        <el-steps :active="currentStep" style="margin-bottom: 24px;">
          <el-step title="基本信息" />
          <el-step title="项目目标" />
          <el-step title="干系人" />
          <el-step title="预览确认" />
        </el-steps>

        <!-- Step 1: Basic Info -->
        <div v-show="currentStep === 0">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="项目名称" prop="projectName">
                <el-input v-model="form.projectName" placeholder="请输入项目名称" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="项目简称" prop="projectShortName">
                <el-input v-model="form.projectShortName" placeholder="请输入项目简称" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="项目编号" prop="projectCode">
                <el-input v-model="form.projectCode" placeholder="自动生成或手动输入" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="预算上限" prop="budgetCap">
                <el-input v-model="form.budgetCap" placeholder="如：5000000">
                  <template #append>元人民币</template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="项目描述" prop="description">
            <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入项目描述" />
          </el-form-item>
          <el-form-item label="范围概述" prop="scopeSummary">
            <el-input v-model="form.scopeSummary" type="textarea" :rows="3" placeholder="请输入范围概述" />
          </el-form-item>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="项目发起人" prop="sponsorId">
                <el-select v-model="form.sponsorId" placeholder="搜索并选择发起人" filterable clearable style="width: 100%">
                  <el-option v-for="u in userList" :key="u.id" :label="`${u.realName} (${u.username})`" :value="u.id" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="项目经理" prop="pmId">
                <el-select v-model="form.pmId" placeholder="搜索并选择项目经理" filterable clearable style="width: 100%">
                  <el-option v-for="u in userList" :key="u.id" :label="`${u.realName} (${u.username})`" :value="u.id" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="项目进度" prop="progress">
                <el-select v-model="form.progress" placeholder="选择项目进度" style="width: 100%">
                  <el-option label="进行中" value="IN_PROGRESS" />
                  <el-option label="已验收" value="ACCEPTED" />
                  <el-option label="已完成" value="COMPLETED" />
                  <el-option label="已暂停" value="SUSPENDED" />
                  <el-option label="已取消" value="CANCELLED" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="审批状态">
                <el-tag :type="statusTagType(form.status)">{{ statusLabel(form.status) }}</el-tag>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="计划开始日期" prop="startDate">
                <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="计划结束日期" prop="endDate">
                <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" :disabledDate="endDateDisabled" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-divider>合同信息</el-divider>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="项目分类" prop="projectCategory">
                <el-select v-model="form.projectCategory" placeholder="选择项目分类" style="width: 100%">
                  <el-option label="合同项目" value="CONTRACT" />
                  <el-option label="研发项目" value="R_D" />
                  <el-option label="提前执行" value="ADVANCE" />
                  <el-option label="公共项目" value="PUBLIC" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="合同编号" :prop="isContractProject ? 'contractNo' : ''">
                <el-input v-model="form.contractNo" :disabled="!isContractProject" placeholder="请输入合同编号" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="6">
              <el-form-item label="项目含税产值" :prop="isContractProject ? 'outputValueTaxable' : ''">
                <el-input v-model="form.outputValueTaxable" :disabled="!isContractProject" placeholder="请输入含税产值" @input="calcTaxAmount">
                  <template #append>元</template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="税率" :prop="isContractProject ? 'taxRate' : ''">
                <el-input v-model="form.taxRate" :disabled="!isContractProject" placeholder="如：13" @input="calcTaxAmount">
                  <template #append>%</template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="税额">
                <el-input v-model="form.taxAmount" disabled placeholder="自动计算">
                  <template #append>元</template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="项目不含税产值">
                <el-input v-model="form.outputValueExcludingTax" disabled placeholder="自动计算">
                  <template #append>元</template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- Step 2: Objectives -->
        <div v-show="currentStep === 1">
          <el-form-item label="项目目标">
            <div v-for="(obj, idx) in objectivesList" :key="idx" style="margin-bottom: 12px; display: flex; gap: 8px; align-items: center;">
              <el-input v-model="obj.objective" placeholder="目标" style="flex: 2" />
              <el-input v-model="obj.metric" placeholder="衡量指标" style="flex: 1" />
              <el-input v-model="obj.target" placeholder="目标值" style="flex: 1" />
              <el-button type="danger" @click="objectivesList.splice(idx, 1)" :disabled="objectivesList.length <= 1">删除</el-button>
            </div>
            <el-button type="primary" link @click="objectivesList.push({ objective: '', metric: '', target: '' })">+ 添加目标</el-button>
          </el-form-item>
        </div>

        <!-- Step 3: Stakeholders -->
        <div v-show="currentStep === 2">
          <el-form-item label="关键干系人">
            <div v-for="(sh, idx) in stakeholdersList" :key="idx" style="margin-bottom: 12px; display: flex; gap: 8px; align-items: center;">
              <el-input v-model="sh.role" placeholder="角色" style="flex: 1" />
              <el-input v-model="sh.name" placeholder="姓名" style="flex: 1" />
              <el-input v-model="sh.org" placeholder="组织" style="flex: 1" />
              <el-button type="danger" @click="stakeholdersList.splice(idx, 1)" :disabled="stakeholdersList.length <= 1">删除</el-button>
            </div>
            <el-button type="primary" link @click="stakeholdersList.push({ role: '', name: '', org: '' })">+ 添加干系人</el-button>
          </el-form-item>
        </div>

        <!-- Step 4: Preview -->
        <div v-show="currentStep === 3">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="项目名称">{{ form.projectName }}</el-descriptions-item>
            <el-descriptions-item label="项目简称">{{ form.projectShortName }}</el-descriptions-item>
            <el-descriptions-item label="项目编号">{{ form.projectCode }}</el-descriptions-item>
            <el-descriptions-item label="项目描述" :span="2">{{ form.description }}</el-descriptions-item>
            <el-descriptions-item label="范围概述" :span="2">{{ form.scopeSummary }}</el-descriptions-item>
            <el-descriptions-item label="项目发起人">{{ getUserName(form.sponsorId) }}</el-descriptions-item>
            <el-descriptions-item label="项目经理">{{ getUserName(form.pmId) }}</el-descriptions-item>
            <el-descriptions-item label="预算上限">{{ formatBudget(form.budgetCap) }}</el-descriptions-item>
            <el-descriptions-item label="项目进度">{{ progressLabel(form.progress) }}</el-descriptions-item>
            <el-descriptions-item label="日期范围">{{ form.startDate }} ~ {{ form.endDate }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- Navigation buttons -->
        <el-form-item style="margin-top: 24px;">
          <el-button v-if="currentStep > 0" @click="currentStep--">上一步</el-button>
          <el-button v-if="currentStep < 3" type="primary" @click="nextStep">下一步</el-button>
          <el-button v-if="currentStep === 3" type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onActivated, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createCharterApi, updateCharterApi, getCharterDetailApi } from '@/api/pm/charter'
import { getUserListApi } from '@/api/system/user'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const currentStep = ref(0)
const submitLoading = ref(false)
const isEdit = computed(() => !!route.params.id)
const userList = ref([])

const form = reactive({
  projectName: '',
  projectCode: '',
  projectShortName: '',
  description: '',
  scopeSummary: '',
  sponsorId: '',
  pmId: '',
  budgetCap: '',
  startDate: '',
  endDate: '',
  contractNo: '',
  projectCategory: '',
  outputValueTaxable: '',
  outputValueExcludingTax: '',
  taxRate: '',
  taxAmount: '',
  progress: 'IN_PROGRESS',
  status: ''
})

const objectivesList = ref([{ objective: '', metric: '', target: '' }])
const stakeholdersList = ref([{ role: '', name: '', org: '' }])

const rules = {
  projectName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  projectShortName: [{ required: true, message: '请输入项目简称', trigger: 'blur' }],
  projectCode: [{ required: true, message: '请输入项目编号', trigger: 'blur' }],
  sponsorId: [{ required: true, message: '请选择项目发起人', trigger: 'change' }],
  pmId: [{ required: true, message: '请选择项目经理', trigger: 'change' }],
  endDate: [{
    validator: (rule, value, callback) => {
      if (value && form.startDate && value < form.startDate) {
        callback(new Error('结束日期不能早于开始日期'))
      } else {
        callback()
      }
    },
    trigger: 'change'
  }],
  projectCategory: [{ required: true, message: '请选择项目分类', trigger: 'change' }],
  contractNo: [{ required: true, message: '请输入合同编号', trigger: 'blur' }],
  outputValueTaxable: [{ required: true, message: '请输入项目含税产值', trigger: 'blur' }],
  taxRate: [{ required: true, message: '请输入税率', trigger: 'blur' }]
}

const endDateDisabled = (date) => {
  if (!form.startDate) return false
  return date.getTime() < new Date(form.startDate + 'T00:00:00').getTime()
}

const isContractProject = computed(() => form.projectCategory === 'CONTRACT')

const calcTaxAmount = () => {
  const taxable = parseFloat(form.outputValueTaxable) || 0
  const rate = parseFloat(form.taxRate) || 0
  const tax = (taxable * rate / 100).toFixed(2)
  const excludingTax = (taxable - parseFloat(tax)).toFixed(2)
  form.taxAmount = tax
  form.outputValueExcludingTax = excludingTax
}

watch(() => form.startDate, () => {
  if (form.endDate) {
    formRef.value?.validateField('endDate')
  }
})

const formatBudget = (val) => {
  if (!val) return ''
  const num = parseFloat(val)
  return isNaN(num) ? val + ' 元人民币' : num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' 元人民币'
}

const getUserName = (userId) => {
  if (!userId) return '-'
  const user = userList.value.find(u => u.id === userId)
  return user ? `${user.realName} (${user.username})` : userId
}

const statusTagType = (status) => {
  const map = { DRAFT: 'info', PENDING_APPROVAL: 'warning', APPROVED: 'success', REJECTED: 'danger', CLOSED: '' }
  return map[status] || 'info'
}

const statusLabel = (status) => {
  const map = { DRAFT: '草稿', PENDING_APPROVAL: '审批中', APPROVED: '已通过', REJECTED: '已驳回', CLOSED: '已关闭' }
  return map[status] || status
}

const progressLabel = (progress) => {
  const map = { IN_PROGRESS: '进行中', ACCEPTED: '已验收', COMPLETED: '已完成', SUSPENDED: '已暂停', CANCELLED: '已取消' }
  return map[progress] || progress
}

const nextStep = async () => {
  if (currentStep.value === 0) {
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) return
  }
  currentStep.value++
}

const handleSubmit = async () => {
  // Serialize objectives and stakeholders
  form.objectives = JSON.stringify(objectivesList.value.filter(o => o.objective))
  form.keyStakeholders = JSON.stringify(stakeholdersList.value.filter(s => s.name))

  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateCharterApi(route.params.id, form)
      ElMessage.success('更新成功')
    } else {
      await createCharterApi(form)
      ElMessage.success('创建成功')
    }
    router.push('/pm/charter')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    submitLoading.value = false
  }
}

const loadUserList = async () => {
  try {
    const res = await getUserListApi()
    userList.value = res.data.records || []
  } catch {
    ElMessage.error('加载用户列表失败')
  }
}

const loadDetail = async () => {
  if (!route.params.id) return
  const res = await getCharterDetailApi(route.params.id)
  Object.assign(form, res.data)
  if (res.data.objectives) {
    try { objectivesList.value = JSON.parse(res.data.objectives) } catch { /* ignore */ }
  }
  if (res.data.keyStakeholders) {
    try { stakeholdersList.value = JSON.parse(res.data.keyStakeholders) } catch { /* ignore */ }
  }
}

const resetForm = () => {
  currentStep.value = 0
  Object.assign(form, {
    projectName: '',
    projectCode: '',
    projectShortName: '',
    description: '',
    scopeSummary: '',
    sponsorId: '',
    pmId: '',
    budgetCap: '',
    startDate: '',
    endDate: '',
    contractNo: '',
    projectCategory: '',
    outputValueTaxable: '',
    outputValueExcludingTax: '',
    taxRate: '',
    taxAmount: '',
    progress: 'IN_PROGRESS',
    status: ''
  })
  objectivesList.value = [{ objective: '', metric: '', target: '' }]
  stakeholdersList.value = [{ role: '', name: '', org: '' }]
}

const initForm = async () => {
  if (route.params.id) {
    await loadDetail()
  } else {
    resetForm()
  }
}

onMounted(() => {
  loadUserList()
  initForm()
})
onActivated(initForm)
</script>
