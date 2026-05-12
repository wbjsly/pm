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
          <el-tag :type="dictStore.getTagType('CHARTER_PROGRESS', detail.progress)">{{ dictStore.getLabel('CHARTER_PROGRESS', detail.progress) }}</el-tag>
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
              <el-tag :type="dictStore.getTagType('CHARTER_STATUS', detail.status)">{{ dictStore.getLabel('CHARTER_STATUS', detail.status) }}</el-tag>
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
          <div v-loading="tabLoading.budget">
            <el-table :data="budgetData" stripe size="small">
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
                  <el-tag size="small" :type="dictStore.getTagType('BUDGET_STATUS', row.status)">{{ dictStore.getLabel('BUDGET_STATUS', row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createByName" label="创建人" width="100" />
              <el-table-column prop="createDate" label="创建日期" width="110">
                <template #default="{ row }">{{ row.createDate ? row.createDate.substring(0, 10) : '-' }}</template>
              </el-table-column>
            </el-table>

            <!-- 预实对比 -->
            <template v-if="comparisonData">
              <el-divider>预实对比</el-divider>

              <el-row :gutter="16" style="margin-bottom: 16px">
                <el-col :span="6">
                  <el-card shadow="never">
                    <div class="compare-stat">
                      <div class="compare-stat-label">项目直接预算</div>
                      <div class="compare-stat-value">¥ {{ formatMoney(comparisonData.directBudget) }}</div>
                    </div>
                  </el-card>
                </el-col>
                <el-col :span="6">
                  <el-card shadow="never">
                    <div class="compare-stat">
                      <div class="compare-stat-label">实际成本</div>
                      <div class="compare-stat-value" :class="{ 'over-budget': comparisonData.totalRatio > 1 }">
                        ¥ {{ formatMoney(comparisonData.totalActual) }}
                      </div>
                    </div>
                  </el-card>
                </el-col>
                <el-col :span="6">
                  <el-card shadow="never">
                    <div class="compare-stat">
                      <div class="compare-stat-label">剩余预算</div>
                      <div class="compare-stat-value" :style="{ color: (comparisonData.directBudget || 0) - (comparisonData.totalActual || 0) < 0 ? '#f56c6c' : '' }">¥ {{ formatMoney((comparisonData.directBudget || 0) - (comparisonData.totalActual || 0)) }}</div>
                    </div>
                  </el-card>
                </el-col>
                <el-col :span="6">
                  <el-card shadow="never">
                    <div class="compare-stat">
                      <div class="compare-stat-label">执行比率</div>
                      <div class="compare-stat-value" :style="{ color: comparisonRatioColor(comparisonData.totalRatio) }">
                        {{ ((comparisonData.totalRatio || 0) * 100).toFixed(1) }}%
                      </div>
                    </div>
                  </el-card>
                </el-col>
              </el-row>

              <el-table :data="comparisonFlatItems" border stripe size="small" row-key="id">
                <el-table-column prop="name" label="预算科目" min-width="150" />
                <el-table-column label="层级" width="80">
                  <template #default="{ row: r }">
                    <el-tag size="small" :type="r.level === 1 ? '' : 'info'">{{ r.level === 1 ? '一级' : '二级' }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="项目直接预算" width="140" align="right">
                  <template #default="{ row: r }"><span class="amount-cell">¥ {{ formatMoney(r.budgetAmount) }}</span></template>
                </el-table-column>
                <el-table-column label="实际金额" width="140" align="right">
                  <template #default="{ row: r }"><span class="amount-cell">¥ {{ formatMoney(r.actualAmount) }}</span></template>
                </el-table-column>
                <el-table-column label="剩余预算" width="140" align="right">
                  <template #default="{ row: r }">
                    <span class="amount-cell" :style="{ color: r.remaining < 0 ? '#f56c6c' : '' }">
                      ¥ {{ formatMoney(r.remaining) }}
                    </span>
                  </template>
                </el-table-column>
                <el-table-column label="比率" width="100" align="center">
                  <template #default="{ row: r }">
                    <span :style="{ color: comparisonRatioColumnColor(r.ratio), fontWeight: 'bold' }">
                      {{ ((r.ratio || 0) * 100).toFixed(1) }}%
                    </span>
                  </template>
                </el-table-column>
              </el-table>
            </template>
            <div v-else-if="!tabLoading.budget && budgetData.length > 0" style="text-align: center; color: #909399; padding: 20px;">
              暂无已审批通过的预算，无法生成预实对比
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="项目成本" name="actualCost">
          <div v-loading="tabLoading.actualCost">
            <div ref="actualCostChartRef" style="width: 100%; height: 400px;"></div>

            <div style="margin: 16px 0;">
              <div style="display: flex; align-items: center; flex-wrap: wrap; gap: 8px;">
                <span style="color: #606266; font-size: 14px;">成本分类：</span>
                <el-checkbox-group v-model="checkedCategories" @change="onCategoryChange">
                  <el-checkbox label="all">全部</el-checkbox>
                  <el-checkbox label="LABOR">人工</el-checkbox>
                  <el-checkbox label="PROCUREMENT">采购</el-checkbox>
                  <el-checkbox label="TRAVEL">差旅</el-checkbox>
                  <el-checkbox label="BUSINESS">商务费用</el-checkbox>
                  <el-checkbox label="ENTERTAINMENT">客户招待费</el-checkbox>
                  <el-checkbox label="ACTIVITY">活动费</el-checkbox>
                  <el-checkbox label="OTHER">其他</el-checkbox>
                </el-checkbox-group>
              </div>
              <div style="display: flex; align-items: center; flex-wrap: wrap; gap: 8px; margin-top: 8px;">
                <span style="color: #606266; font-size: 14px;">成本所属年月：</span>
                <el-select
                  v-model="selectedYearMonths"
                  multiple
                  filterable
                  collapse-tags
                  collapse-tags-tooltip
                  placeholder="全部"
                  style="width: 280px;"
                  @change="onYearMonthChange"
                >
                  <el-option label="全部" value="all" />
                  <el-option
                    v-for="ym in yearMonthOptions"
                    :key="ym"
                    :label="ym"
                    :value="ym"
                  />
                </el-select>
              </div>
            </div>

            <div style="display: flex; margin-top: 0; padding: 8px 0; font-weight: bold; font-size: 13px;">
              <div style="width: 120px;"></div>
              <div style="width: 100px;"></div>
              <div style="width: 140px; text-align: right;">汇总：¥ {{ formatMoney(currentSummaryAmount) }}</div>
              <div style="flex: 1;"></div>
            </div>

            <el-table :data="actualCostList" stripe size="small">
              <el-table-column prop="costDate" label="成本日期" width="120" />
              <el-table-column label="成本分类" width="100">
                <template #default="{ row }">{{ comparisonCategoryLabels[row.costType] || row.costType }}</template>
              </el-table-column>
              <el-table-column label="金额" width="140" align="right">
                <template #default="{ row }">¥ {{ formatMoney(row.amount) }}</template>
              </el-table-column>
              <el-table-column prop="description" label="描述" min-width="200">
                <template #default="{ row }">{{ row.description || '-' }}</template>
              </el-table-column>
            </el-table>

            <el-pagination
              v-model:current-page="actualCostPage.pageNum"
              v-model:page-size="actualCostPage.pageSize"
              :page-sizes="[100]"
              :total="actualCostPage.total"
              layout="total, prev, pager, next"
              style="margin-top: 16px; justify-content: flex-end;"
              @current-change="loadActualCostList"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="项目任务" name="tasks">
          <el-table :data="wbsData" row-key="id" :tree-props="{ children: 'children' }" v-loading="tabLoading.tasks" stripe size="small">
            <el-table-column prop="wbsCode" label="编码" width="140" />
            <el-table-column prop="name" label="名称" min-width="180" />
            <el-table-column prop="productName" label="产品" width="100" />
            <el-table-column prop="moduleName" label="模块" width="80" />
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="dictStore.getTagType('WBS_ELEMENT_STATUS', row.status)">{{ dictStore.getLabel('WBS_ELEMENT_STATUS', row.status) }}</el-tag>
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

        <el-tab-pane label="项目成果" name="deliverables">
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
                <el-tag size="small" :type="dictStore.getTagType('DELIVERABLE_STATUS', row.status)">{{ dictStore.getLabel('DELIVERABLE_STATUS', row.status) }}</el-tag>
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
import { getBudgetListApi, getBudgetComparisonApi } from '@/api/pm/budget'
import { getWorkHoursByProjectApi } from '@/api/pm/workHours'
import { getDeliverableListApi } from '@/api/pm/deliverable'
import { getActualCostListApi, getActualCostAggregationApi, getActualCostSumApi } from '@/api/pm/actualCost'
import { ElMessage } from 'element-plus'

import { useDictStore } from '@/store/dict'

const route = useRoute()
const router = useRouter()
const dictStore = useDictStore()
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
  deliverables: false,
  actualCost: false
})

const loadedTabs = reactive({
  tasks: false,
  budget: false,
  workHours: false,
  deliverables: false,
  actualCost: false
})

// Budget comparison data
const comparisonData = ref(null)
const comparisonLoading = ref(false)

// Actual cost tab data
const actualCostChartRef = ref(null)
let actualCostChartInstance = null
let actualCostResizeHandler = null
const checkedCategories = ref(['all'])
const prevCheckedCategories = ref(['all'])
const selectedYearMonths = ref(['all'])
const prevSelectedYearMonths = ref(['all'])
const yearMonthFilter = ref(null)
const currentSummaryAmount = ref(0)
const actualCostAggregation = ref([])

const yearMonthOptions = computed(() => {
  return actualCostAggregation.value.map(d => d.yearMonth).filter(Boolean)
})
const actualCostList = ref([])
const actualCostPage = reactive({ pageNum: 1, pageSize: 100, total: 0 })

const comparisonCategoryLabels = {
  LABOR: '人工',
  PROCUREMENT: '采购',
  TRAVEL: '差旅',
  BUSINESS: '商务费用',
  ENTERTAINMENT: '客户招待费',
  ACTIVITY: '活动费',
  OTHER: '其他'
}

const comparisonFlatItems = computed(() => {
  if (!comparisonData.value || !comparisonData.value.items) return []
  const items = []
  for (const item of comparisonData.value.items) {
    items.push({
      id: item.id,
      name: comparisonCategoryLabels[item.category] || item.category,
      level: item.level,
      budgetAmount: item.budgetAmount,
      actualAmount: item.actualAmount,
      ratio: item.ratio,
      remaining: (item.budgetAmount || 0) - (item.actualAmount || 0)
    })
    for (const child of item.children || []) {
      let childName = comparisonCategoryLabels[child.category] || child.category
      if (child.roleCode) childName += ` - ${child.roleCode}`
      if (child.bomItem) childName += ` - ${child.bomItem}`
      items.push({
        id: child.id,
        name: childName,
        level: child.level,
        budgetAmount: child.budgetAmount,
        actualAmount: child.actualAmount,
        ratio: child.ratio,
        remaining: (child.budgetAmount || 0) - (child.actualAmount || 0)
      })
    }
  }
  return items
})

const comparisonRatioColor = (ratio) => {
  if (!ratio) return '#67c23a'
  if (ratio < 0.8) return '#67c23a'
  if (ratio < 0.95) return '#e6a23c'
  if (ratio <= 1) return '#f56c6c'
  return '#f56c6c'
}

const comparisonRatioColumnColor = (ratio) => {
  if (!ratio) return '#67c23a'
  if (ratio >= 1) return '#f56c6c'
  if (ratio > 0.9) return '#e6a23c'
  return '#67c23a'
}

// Budget helpers: migrated to dictStore (BUDGET_STATUS)

// Deliverable helpers: migrated to dictStore (DELIVERABLE_STATUS)
// WBS helpers: migrated to dictStore (WBS_ELEMENT_STATUS)

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

    // Load comparison for latest approved budget
    const approved = budgetData.value.find(b => b.status === 'APPROVED')
    if (approved) {
      comparisonLoading.value = true
      try {
        const compRes = await getBudgetComparisonApi(approved.id)
        comparisonData.value = compRes.data
      } catch { comparisonData.value = null }
      finally { comparisonLoading.value = false }
    }
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

const expandCategoryFilter = (checked) => {
  return checked.join(',')
}

const loadActualCost = async () => {
  if (loadedTabs.actualCost) return
  tabLoading.actualCost = true
  try {
    const aggRes = await getActualCostAggregationApi(route.params.id)
    actualCostAggregation.value = aggRes.data || []
    await Promise.all([loadActualCostList(), loadActualCostSum()])
    loadedTabs.actualCost = true
    await nextTick()
    renderActualCostChart()
  } catch { ElMessage.error('加载实际成本失败') }
  finally { tabLoading.actualCost = false }
}

const loadActualCostList = async () => {
  try {
    const costTypes = checkedCategories.value.includes('all')
      ? null
      : expandCategoryFilter(checkedCategories.value)
    const res = await getActualCostListApi({
      projectId: route.params.id,
      costTypes: costTypes || undefined,
      yearMonth: yearMonthFilter.value || undefined,
      pageNum: actualCostPage.pageNum,
      pageSize: actualCostPage.pageSize
    })
    actualCostList.value = res.data?.records || []
    actualCostPage.total = res.data?.total || 0
  } catch { /* handled by caller */ }
}

const loadActualCostSum = async () => {
  try {
    const costTypes = checkedCategories.value.includes('all')
      ? null
      : expandCategoryFilter(checkedCategories.value)
    const res = await getActualCostSumApi({
      projectId: route.params.id,
      costTypes: costTypes || undefined,
      yearMonth: yearMonthFilter.value || undefined
    })
    currentSummaryAmount.value = res.data || 0
  } catch { /* handled by caller */ }
}

const onCategoryChange = (val) => {
  const hadAll = prevCheckedCategories.value.includes('all')
  const hasAll = val.includes('all')

  if (hasAll && !hadAll) {
    checkedCategories.value = ['all']
  } else if (hadAll && hasAll && val.length > 1) {
    checkedCategories.value = val.filter(v => v !== 'all')
  } else if (!hasAll && val.length === 0) {
    checkedCategories.value = ['all']
  }

  prevCheckedCategories.value = [...checkedCategories.value]
  actualCostPage.pageNum = 1
  loadActualCostList()
  loadActualCostSum()
}

const onYearMonthChange = (val) => {
  const hadAll = prevSelectedYearMonths.value.includes('all')
  const hasAll = val.includes('all')

  if (hasAll && !hadAll) {
    selectedYearMonths.value = ['all']
    yearMonthFilter.value = null
  } else if (hadAll && hasAll && val.length > 1) {
    selectedYearMonths.value = val.filter(v => v !== 'all')
    yearMonthFilter.value = selectedYearMonths.value.join(',')
  } else if (!hasAll && val.length === 0) {
    selectedYearMonths.value = ['all']
    yearMonthFilter.value = null
  } else {
    yearMonthFilter.value = val.join(',')
  }

  prevSelectedYearMonths.value = [...selectedYearMonths.value]
  actualCostPage.pageNum = 1
  loadActualCostList()
  loadActualCostSum()
}

const renderActualCostChart = () => {
  if (!actualCostChartRef.value) return
  if (actualCostChartInstance) actualCostChartInstance.dispose()
  actualCostChartInstance = echarts.init(actualCostChartRef.value)
  const data = actualCostAggregation.value
  const sumFields = ['laborAmount', 'procurementAmount', 'travelAmount', 'businessAmount', 'entertainmentAmount', 'activityAmount', 'otherAmount']
  const totalCost = data.reduce((s, d) => s + sumFields.reduce((sum, f) => sum + (d[f] || 0), 0), 0)
  const barTotals = data.map(d => sumFields.reduce((sum, f) => sum + (d[f] || 0), 0))

  const categorySeries = [
    { name: '人工', key: 'laborAmount', color: '#409eff' },
    { name: '采购', key: 'procurementAmount', color: '#67c23a' },
    { name: '差旅', key: 'travelAmount', color: '#e6a23c' },
    { name: '商务费用', key: 'businessAmount', color: '#f56c6c' },
    { name: '客户招待费', key: 'entertainmentAmount', color: '#909399' },
    { name: '活动费', key: 'activityAmount', color: '#b37feb' },
    { name: '其他', key: 'otherAmount', color: '#6b7b8d' }
  ]

  actualCostChartInstance.setOption({
    title: {
      text: `实际成本总额：¥ ${totalCost.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`,
      left: 'center',
      top: 0,
      triggerEvent: true,
      textStyle: { fontSize: 14, fontWeight: 'bold', color: '#409eff' }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        let html = `<b>${params[0].axisValue}</b><br/>`
        params.forEach(p => {
          if (p.seriesName !== '合计') {
            html += `${p.marker} ${p.seriesName}: ¥ ${p.value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}<br/>`
          }
        })
        const total = barTotals[params[0].dataIndex]
        html += `<b>合计: ¥ ${total.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</b>`
        return html
      }
    },
    legend: { data: categorySeries.map(s => s.name), top: 0, right: 0 },
    grid: { left: 60, right: 40, top: 40, bottom: 40 },
    xAxis: { type: 'category', data: data.map(d => d.yearMonth), axisLabel: { rotate: 45 } },
    yAxis: { type: 'value', name: '元' },
    series: [
      ...categorySeries.map(s => ({
        name: s.name,
        type: 'bar',
        stack: 'total',
        data: data.map(d => d[s.key] || 0),
        itemStyle: { color: s.color }
      })),
      {
        name: '合计',
        type: 'bar',
        stack: 'total',
        data: new Array(data.length).fill(0),
        itemStyle: { color: 'transparent' },
        label: {
          show: true,
          position: 'top',
          formatter: (p) => `¥ ${barTotals[p.dataIndex].toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`,
          color: '#303133',
          fontWeight: 'bold',
          fontSize: 12
        }
      }
    ]
  })

  actualCostChartInstance.on('click', (params) => {
    if (params.componentType === 'series') {
      selectedYearMonths.value = [params.name]
      yearMonthFilter.value = params.name
      actualCostPage.pageNum = 1
      loadActualCostList()
      loadActualCostSum()
    }
  })

  actualCostChartInstance.on('click', (params) => {
    if (params.componentType === 'title') {
      selectedYearMonths.value = ['all']
      yearMonthFilter.value = null
      actualCostPage.pageNum = 1
      loadActualCostList()
      loadActualCostSum()
    }
  })

  actualCostResizeHandler = () => actualCostChartInstance?.resize()
  window.addEventListener('resize', actualCostResizeHandler)
}

const handleTabClick = async ({ paneName }) => {
  const loaders = { tasks: loadWbs, budget: loadBudget, workHours: loadWorkHours, deliverables: loadDeliverables, actualCost: loadActualCost }
  await loaders[paneName]?.()
  if (paneName === 'workHours') {
    await nextTick()
    chartInstance?.resize()
  }
  if (paneName === 'actualCost') {
    await nextTick()
    actualCostChartInstance?.resize()
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

// Charter status: migrated to dictStore (CHARTER_STATUS)

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

// Charter progress: migrated to dictStore (CHARTER_PROGRESS)

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
  if (actualCostChartInstance) { actualCostChartInstance.dispose(); actualCostChartInstance = null }
  if (actualCostResizeHandler) { window.removeEventListener('resize', actualCostResizeHandler); actualCostResizeHandler = null }
  wbsData.value = []
  budgetData.value = []
  comparisonData.value = null
  workHoursSummary.value = []
  deliverableData.value = []
  actualCostAggregation.value = []
  actualCostList.value = []
  currentSummaryAmount.value = 0
  yearMonthFilter.value = null
  selectedYearMonths.value = ['all']
  checkedCategories.value = ['all']
  prevCheckedCategories.value = ['all']
  actualCostPage.pageNum = 1
  actualCostPage.total = 0
  Object.keys(loadedTabs).forEach(k => loadedTabs[k] = false)
  loadDetail()
})

onBeforeUnmount(() => {
  if (chartInstance) { chartInstance.dispose(); chartInstance = null }
  if (resizeHandler) { window.removeEventListener('resize', resizeHandler); resizeHandler = null }
  if (actualCostChartInstance) { actualCostChartInstance.dispose(); actualCostChartInstance = null }
  if (actualCostResizeHandler) { window.removeEventListener('resize', actualCostResizeHandler); actualCostResizeHandler = null }
})
</script>

<style scoped>
.compare-stat {
  text-align: center;
  padding: 8px 0;
}
.compare-stat-label {
  color: #909399;
  font-size: 13px;
}
.compare-stat-value {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
  margin-top: 6px;
}
.compare-stat-value.over-budget {
  color: #f56c6c;
}
.amount-cell {
  font-variant-numeric: tabular-nums;
}
</style>
