<template>
  <div class="charter-detail">
    <el-card v-loading="loading">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span style="font-weight: bold; font-size: 16px;">项目详情</span>
          <el-button @click="$router.back()">返回</el-button>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="章程编号">{{ detail.charterCode }}</el-descriptions-item>
        <el-descriptions-item label="预算上限">{{ formatBudget(detail.budgetCap) }}</el-descriptions-item>
        <el-descriptions-item label="项目编号">{{ detail.projectCode }}</el-descriptions-item>
        <el-descriptions-item label="项目简称">{{ detail.projectShortName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(detail.status)">{{ statusLabel(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="项目分类">
          <el-tag v-if="detail.projectCategory" :type="categoryTagType(detail.projectCategory)">{{ categoryLabel(detail.projectCategory) }}</el-tag>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="合同编号">{{ detail.contractNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="项目含税产值">{{ formatMoney(detail.outputValueTaxable) }}</el-descriptions-item>
        <el-descriptions-item label="项目不含税产值">{{ formatMoney(detail.outputValueExcludingTax) }}</el-descriptions-item>
        <el-descriptions-item label="税率">{{ detail.taxRate ? detail.taxRate + '%' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="税额">{{ formatMoney(detail.taxAmount) }}</el-descriptions-item>
        <el-descriptions-item label="项目名称" :span="2">{{ detail.projectName }}</el-descriptions-item>
        <el-descriptions-item label="项目描述" :span="2">{{ detail.description }}</el-descriptions-item>
        <el-descriptions-item label="项目目标" :span="2">{{ detail.objectives }}</el-descriptions-item>
        <el-descriptions-item label="范围概述" :span="2">{{ detail.scopeSummary }}</el-descriptions-item>
        <el-descriptions-item label="项目发起人">{{ detail.sponsorName || detail.sponsorId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="项目经理">{{ detail.pmName || detail.pmId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="计划开始日期">{{ detail.startDate }}</el-descriptions-item>
        <el-descriptions-item label="计划结束日期">{{ detail.endDate }}</el-descriptions-item>
        <el-descriptions-item label="关键干系人" :span="2">{{ detail.keyStakeholders }}</el-descriptions-item>
        <el-descriptions-item label="审批意见" :span="2">{{ detail.approvalComment || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remarks }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onActivated } from 'vue'
import { useRoute } from 'vue-router'
import { getCharterDetailApi } from '@/api/pm/charter'
import { ElMessage } from 'element-plus'

const route = useRoute()
const detail = ref({})
const loading = ref(false)

const formatBudget = (val) => {
  if (!val) return '-'
  const num = parseFloat(val)
  return isNaN(num) ? val + ' 元人民币' : num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' 元人民币'
}

const formatMoney = (val) => {
  if (!val) return '-'
  const num = parseFloat(val)
  return isNaN(num) ? val : num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const categoryTagType = (cat) => {
  const map = { CONTRACT: 'primary', R_D: 'warning', ADVANCE: 'success', PUBLIC: 'info' }
  return map[cat] || 'info'
}
const categoryLabel = (cat) => {
  const map = { CONTRACT: '合同项目', R_D: '研发项目', ADVANCE: '提前执行', PUBLIC: '公共项目' }
  return map[cat] || cat
}

const statusTagType = (status) => {
  const map = { DRAFT: 'info', PENDING_APPROVAL: 'warning', APPROVED: 'success', REJECTED: 'danger', CLOSED: '' }
  return map[status] || 'info'
}

const statusLabel = (status) => {
  const map = { DRAFT: '草稿', PENDING_APPROVAL: '审批中', APPROVED: '已通过', REJECTED: '已驳回', CLOSED: '已关闭' }
  return map[status] || status
}

const loadDetail = async () => {
  loading.value = true
  try {
    const res = await getCharterDetailApi(route.params.id)
    detail.value = res.data
  } catch {
    ElMessage.error('加载详情失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadDetail)
onActivated(loadDetail)
</script>
