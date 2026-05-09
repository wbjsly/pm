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
        <el-descriptions-item label="项目编号">{{ detail.projectCode }}</el-descriptions-item>
        <el-descriptions-item label="项目名称">{{ detail.projectName }}<span v-if="detail.projectShortName" style="color: #909399;">（简称：{{ detail.projectShortName }}）</span></el-descriptions-item>
        <el-descriptions-item label="项目经理">{{ detail.pmName || detail.pmId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="项目进度">
          <el-tag :type="progressTagType(detail.progress)">{{ progressLabel(detail.progress) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="项目分类">
          <el-tag v-if="detail.projectCategory" :type="categoryTagType(detail.projectCategory)">{{ categoryLabel(detail.projectCategory) }}</el-tag>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="合同编号">{{ detail.contractNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="计划开始日期">{{ detail.startDate }}</el-descriptions-item>
        <el-descriptions-item label="计划结束日期">{{ detail.endDate }}</el-descriptions-item>
      </el-descriptions>

      <el-tabs v-model="activeTab" style="margin-top: 20px;" @tab-click="handleTabClick">
        <el-tab-pane label="项目信息" name="projectInfo">
          <el-descriptions :column="4" border>
            <el-descriptions-item label="章程编号">{{ detail.charterCode }}</el-descriptions-item>
            <el-descriptions-item label="项目发起人">{{ detail.sponsorName || detail.sponsorId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="预算上限">{{ formatBudget(detail.budgetCap) }}</el-descriptions-item>
            <el-descriptions-item label="审批状态">
              <el-tag :type="statusTagType(detail.status)">{{ statusLabel(detail.status) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="项目含税产值">{{ formatMoney(detail.outputValueTaxable) }}</el-descriptions-item>
            <el-descriptions-item label="项目不含税产值">{{ formatMoney(detail.outputValueExcludingTax) }}</el-descriptions-item>
            <el-descriptions-item label="税率">{{ detail.taxRate ? detail.taxRate + '%' : '-' }}</el-descriptions-item>
            <el-descriptions-item label="税额">{{ formatMoney(detail.taxAmount) }}</el-descriptions-item>
            <el-descriptions-item label="项目描述" :span="4">{{ detail.description }}</el-descriptions-item>
            <el-descriptions-item label="项目目标" :span="4">{{ formatObjectives(detail.objectives) }}</el-descriptions-item>
            <el-descriptions-item label="范围概述" :span="4">{{ detail.scopeSummary }}</el-descriptions-item>
            <el-descriptions-item label="关键干系人" :span="4">{{ formatStakeholders(detail.keyStakeholders) }}</el-descriptions-item>
            <el-descriptions-item label="审批意见" :span="4">{{ detail.approvalComment || '-' }}</el-descriptions-item>
            <el-descriptions-item label="备注" :span="4">{{ detail.remarks }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <el-tab-pane label="项目预算" name="budget">
          <el-table :data="budgetData" v-loading="tabLoading.budget" stripe size="small">
            <el-table-column prop="budgetCode" label="预算编码" width="180" />
            <el-table-column prop="version" label="版本" width="60" />
            <el-table-column label="总预算(元)" width="140">
              <template #default="{ row }">{{ formatMoney(row.totalBudget) }}</template>
            </el-table-column>
            <el-table-column label="直接预算(元)" width="140">
              <template #default="{ row }">{{ formatMoney(row.costBaseline) }}</template>
            </el-table-column>
            <el-table-column label="管理预算(元)" width="140">
              <template #default="{ row }">{{ formatMoney(row.managementReserve) }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="budgetStatusTagType(row.status)">{{ budgetStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createByName" label="创建人" width="100" />
            <el-table-column prop="createDate" label="创建日期" width="110">
              <template #default="{ row }">{{ row.createDate ? row.createDate.substring(0, 10) : '-' }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="项目任务" name="tasks">
          <el-table :data="wbsData" row-key="id" :tree-props="{ children: 'children' }" v-loading="tabLoading.tasks" stripe size="small">
            <el-table-column prop="wbsCode" label="编码" width="140" />
            <el-table-column prop="name" label="名称" min-width="180" />
            <el-table-column prop="productName" label="产品" width="100" />
            <el-table-column prop="moduleName" label="模块" width="80" />
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="wbsStatusTagType(row.status)">{{ wbsStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="effortHours" label="工时(h)" width="90" />
            <el-table-column prop="ownerName" label="负责人" width="100" />
            <el-table-column prop="planStartDate" label="计划开始" width="110" />
            <el-table-column prop="planEndDate" label="计划结束" width="110" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane name="workHours">
          <template #label>
            <span>项目工时</span>
          </template>
          <div ref="chartRef" style="width: 100%; height: 400px;" v-loading="tabLoading.workHours"></div>
        </el-tab-pane>

        <el-tab-pane label="项目成果物" name="deliverables">
          <el-table :data="deliverableData" v-loading="tabLoading.deliverables" stripe size="small">
            <el-table-column prop="deliverableCode" label="编号" width="200" />
            <el-table-column prop="name" label="名称" min-width="160" />
            <el-table-column label="附件" width="80" align="center">
              <template #default="{ row }">
                <span v-if="countActiveAttachments(row.attachments) > 0">{{ countActiveAttachments(row.attachments) }}</span>
                <span v-else style="color: #999;">0</span>
              </template>
            </el-table-column>
            <el-table-column prop="plannedDeliveryDate" label="计划交付" width="110" />
            <el-table-column prop="actualDeliveryDate" label="实际交付" width="110">
              <template #default="{ row }">
                <span v-if="row.actualDeliveryDate">{{ row.actualDeliveryDate }}</span>
                <span v-else style="color: #999;">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="createByName" label="创建人" width="100" />
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="deliverableStatusTagType(row.status)">{{ deliverableStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onActivated, onBeforeUnmount, watch, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { getCharterDetailApi } from '@/api/pm/charter'
import { getWbsListApi } from '@/api/pm/wbs'
import { getBudgetListApi } from '@/api/pm/budget'
import { getWorkHoursByProjectApi } from '@/api/pm/workHours'
import { getDeliverableListApi } from '@/api/pm/deliverable'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const detail = ref({})
const loading = ref(false)
const activeTab = ref('projectInfo')
const chartRef = ref(null)
let chartInstance = null
let resizeHandler = null

// Tab data
const wbsData = ref([])
const budgetData = ref([])
const workHoursSummary = ref([])
const deliverableData = ref([])

const tabLoading = reactive({
  tasks: false,
  budget: false,
  workHours: false,
  deliverables: false
})

const loadedTabs = reactive({
  tasks: false,
  budget: false,
  workHours: false,
  deliverables: false
})

// WBS helpers
const wbsStatusTagType = (s) => {
  const map = { NOT_STARTED: 'info', IN_DEVELOPMENT: 'warning', TESTING: 'primary', COMPLETED: 'success', SUSPENDED: 'danger', CANCELLED: 'info' }
  return map[s] || 'info'
}
const wbsStatusLabel = (s) => {
  const map = { NOT_STARTED: '未开始', IN_DEVELOPMENT: '开发中', TESTING: '已提测', COMPLETED: '已完成', SUSPENDED: '已暂停', CANCELLED: '已取消' }
  return map[s] || s
}

// Budget helpers
const budgetStatusTagType = (s) => {
  const map = { DRAFT: 'info', PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }
  return map[s] || 'info'
}
const budgetStatusLabel = (s) => {
  const map = { DRAFT: '草稿', PENDING: '审批中', APPROVED: '已审批', REJECTED: '已驳回' }
  return map[s] || s
}

// Deliverable helpers
const deliverableStatusTagType = (s) => {
  const map = { DRAFT: 'info', PENDING_APPROVAL: 'warning', APPROVED: 'success', DELIVERED: '', REJECTED: 'danger' }
  return map[s] || 'info'
}
const deliverableStatusLabel = (s) => {
  const map = { DRAFT: '草稿', PENDING_APPROVAL: '审批中', APPROVED: '已通过', DELIVERED: '已交付', REJECTED: '已驳回' }
  return map[s] || s
}

const countActiveAttachments = (attachments) => {
  if (!attachments) return 0
  try {
    return JSON.parse(attachments).filter(a => !a.deleted).length
  } catch { return 0 }
}

const loadWbs = async () => {
  if (loadedTabs.tasks) return
  tabLoading.tasks = true
  try {
    const res = await getWbsListApi({ projectId: route.params.id })
    wbsData.value = res.data || []
    loadedTabs.tasks = true
  } catch { ElMessage.error('加载任务失败') }
  finally { tabLoading.tasks = false }
}

const loadBudget = async () => {
  if (loadedTabs.budget) return
  tabLoading.budget = true
  try {
    const res = await getBudgetListApi({ projectId: route.params.id, pageNum: 1, pageSize: 100 })
    budgetData.value = res.data?.records || []
    loadedTabs.budget = true
  } catch { ElMessage.error('加载预算失败') }
  finally { tabLoading.budget = false }
}

const totalPersonDays = computed(() => {
  if (!workHoursSummary.value.length) return ''
  const total = workHoursSummary.value.reduce((s, r) => s + (r.totalPersonDays || 0), 0)
  return total.toFixed(1)
})

const loadWorkHours = async () => {
  if (loadedTabs.workHours) return
  tabLoading.workHours = true
  try {
    const now = new Date()
    const months = []
    for (let i = 0; i < 12; i++) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
      months.push({ year: d.getFullYear(), month: d.getMonth() + 1 })
    }
    const results = await Promise.allSettled(
      months.map(m => getWorkHoursByProjectApi(route.params.id, String(m.year), m.month))
    )
    const summaryMap = {}
    results.forEach((r) => {
      if (r.status !== 'fulfilled') return
      const logs = r.value.data || []
      logs.forEach(log => {
        const date = log.logDate || ''
        const ym = date.substring(0, 7)
        if (!ym) return
        if (!summaryMap[ym]) {
          summaryMap[ym] = { yearMonth: ym, year: parseInt(ym.substring(0, 4)), month: parseInt(ym.substring(5, 7)), draftDays: 0, submittedDays: 0, approvedDays: 0, totalPersonDays: 0 }
        }
        const days = (parseFloat(log.hoursWorked) || 0) / 8
        summaryMap[ym].totalPersonDays += days
        const status = log.status || ''
        if (status === 'DRAFT') summaryMap[ym].draftDays += days
        else if (status === 'SUBMITTED') summaryMap[ym].submittedDays += days
        else if (status === 'APPROVED') summaryMap[ym].approvedDays += days
      })
    })
    workHoursSummary.value = Object.values(summaryMap)
      .sort((a, b) => a.yearMonth.localeCompare(b.yearMonth))
    loadedTabs.workHours = true
    await nextTick()
    renderChart()
  } catch { ElMessage.error('加载工时失败') }
  finally { tabLoading.workHours = false }
}

const renderChart = () => {
  if (!chartRef.value) return
  if (chartInstance) chartInstance.dispose()
  chartInstance = echarts.init(chartRef.value)
  const data = workHoursSummary.value
  const xData = data.map(d => d.yearMonth)
  const draftSeries = data.map(d => +d.draftDays.toFixed(1))
  const submittedSeries = data.map(d => +d.submittedDays.toFixed(1))
  const approvedSeries = data.map(d => +d.approvedDays.toFixed(1))
  const personDayLabels = data.map(d => d.totalPersonDays.toFixed(1))

  chartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        let html = `<b>${params[0].axisValue}</b><br/>`
        let total = 0
        params.forEach(p => { html += `${p.marker} ${p.seriesName}: ${p.value} 人天<br/>`; total += p.value })
        html += `<b>合计: ${total.toFixed(1)} 人天</b>`
        return html
      }
    },
    title: {
      text: `合计 ${totalPersonDays.value} 人天`,
      left: 'center',
      top: 0,
      textStyle: { fontSize: 14, fontWeight: 'bold', color: '#409eff' }
    },
    legend: { data: ['草稿', '已提交', '已审批'], top: 0, right: 0 },
    grid: { left: 60, right: 40, top: 40, bottom: 40 },
    xAxis: { type: 'category', data: xData, axisLabel: { rotate: 45 } },
    yAxis: { type: 'value', name: '人天' },
    series: [
      { name: '草稿', type: 'bar', stack: 'total', data: draftSeries, itemStyle: { color: '#909399' } },
      { name: '已提交', type: 'bar', stack: 'total', data: submittedSeries, itemStyle: { color: '#e6a23c' } },
      { name: '已审批', type: 'bar', stack: 'total', data: approvedSeries, itemStyle: { color: '#67c23a' },
        label: {
          show: true,
          position: 'top',
          formatter: (p) => personDayLabels[p.dataIndex] + ' 人天',
          color: '#333',
          fontWeight: 'bold',
          fontSize: 12
        }
      }
    ]
  })

  resizeHandler = () => chartInstance?.resize()
  window.addEventListener('resize', resizeHandler)
}

const loadDeliverables = async () => {
  if (loadedTabs.deliverables) return
  tabLoading.deliverables = true
  try {
    const res = await getDeliverableListApi({ projectId: route.params.id, pageSize: 999 })
    deliverableData.value = res.data?.records || []
    loadedTabs.deliverables = true
  } catch { ElMessage.error('加载成果物失败') }
  finally { tabLoading.deliverables = false }
}

const handleTabClick = async ({ paneName }) => {
  const loaders = { tasks: loadWbs, budget: loadBudget, workHours: loadWorkHours, deliverables: loadDeliverables }
  await loaders[paneName]?.()
  if (paneName === 'workHours') {
    await nextTick()
    chartInstance?.resize()
  }
}

onMounted(() => {
  loadDetail()
})

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

const formatObjectives = (val) => {
  if (!val) return '-'
  try {
    const arr = JSON.parse(val)
    if (!Array.isArray(arr) || !arr.length) return '-'
    return arr.map(o => `${o.objective || ''}-${o.metric || ''}:${o.target || ''}`).join(', ')
  } catch { return val }
}

const formatStakeholders = (val) => {
  if (!val) return '-'
  try {
    const arr = JSON.parse(val)
    if (!Array.isArray(arr) || !arr.length) return '-'
    return arr.map(s => `${s.name || ''}:${s.org || ''}-${s.role || ''}`).join(', ')
  } catch { return val }
}

const progressTagType = (progress) => {
  const map = { IN_PROGRESS: 'warning', ACCEPTED: 'success', COMPLETED: '', SUSPENDED: 'info', CANCELLED: 'danger' }
  return map[progress] || 'info'
}

const progressLabel = (progress) => {
  const map = { IN_PROGRESS: '进行中', ACCEPTED: '已验收', COMPLETED: '已完成', SUSPENDED: '已暂停', CANCELLED: '已取消' }
  return map[progress] || progress
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

onActivated(() => {
  loadDetail()
})

// When project changes, reset all tab caches and reload active tab
watch(() => route.params.id, (newId, oldId) => {
  if (!route.path.startsWith('/pm/charter/detail')) return
  if (!newId || newId === oldId) return
  if (chartInstance) { chartInstance.dispose(); chartInstance = null }
  if (resizeHandler) { window.removeEventListener('resize', resizeHandler); resizeHandler = null }
  wbsData.value = []
  budgetData.value = []
  workHoursSummary.value = []
  deliverableData.value = []
  Object.keys(loadedTabs).forEach(k => loadedTabs[k] = false)
  loadDetail()
})

onBeforeUnmount(() => {
  if (chartInstance) { chartInstance.dispose(); chartInstance = null }
  if (resizeHandler) { window.removeEventListener('resize', resizeHandler); resizeHandler = null }
})
</script>
