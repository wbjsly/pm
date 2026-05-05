<template>
  <div class="dashboard-container">
    <!-- Stat Cards -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :span="6" v-for="card in statCards" :key="card.label">
        <el-card shadow="hover" class="stat-card" @click="handleStatClick(card)">
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="content-row">
      <!-- Recent Projects -->
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>最近项目</span>
            </div>
          </template>
          <el-table :data="recentProjects" size="small" empty-text="暂无项目">
            <el-table-column prop="charterCode" label="章程编号" width="180" />
            <el-table-column prop="projectName" label="项目名称" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createDate" label="创建日期" width="120" />
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="handleViewProject(row)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- Quick Actions -->
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <span>快捷操作</span>
          </template>
          <div class="quick-actions">
            <el-button type="primary" class="action-btn" @click="handleNewProject">
              <el-icon><Plus /></el-icon>
              <span>新增项目</span>
            </el-button>
            <el-button class="action-btn" @click="handleGoProjects">
              <el-icon><Document /></el-icon>
              <span>项目管理</span>
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Cost Warnings -->
    <el-row :gutter="16" class="warning-row" v-if="costWarnings.length > 0">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>成本预警</span>
              <el-button type="primary" link size="small" @click="handleViewWarnings">查看更多</el-button>
            </div>
          </template>
          <el-alert
            v-for="w in costWarnings"
            :key="w.id"
            :title="`${w.level === 'CRITICAL' ? '严重' : w.level === 'WARN' ? '警告' : '提醒'}: 项目成本已达预算的 ${(parseFloat(w.ratio) * 100).toFixed(1)}%`"
            :type="w.level === 'CRITICAL' ? 'error' : w.level === 'WARN' ? 'warning' : 'info'"
            :closable="false"
            show-icon
            style="margin-bottom: 8px"
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- Work Hours Stats -->
    <el-row :gutter="16" class="hours-stats-row" v-if="hoursStats">
      <el-col :span="24">
        <el-card shadow="hover" class="hours-stats-card">
          <template #header>
            <div class="card-header">
              <span>工时填报概况 — {{ year }}年{{ month }}月</span>
              <el-button type="primary" link size="small" @click="handleGoWorkHours">查看详情 →</el-button>
            </div>
          </template>
          <div class="hours-stats-grid">
            <div class="hours-stat-item">
              <div class="hours-stat-value">{{ hoursStats.actualHours }}</div>
              <div class="hours-stat-label">实际已录 / 目标 {{ hoursStats.monthTarget }} h</div>
            </div>
            <div class="hours-stat-item warning">
              <div class="hours-stat-value">{{ hoursStats.unfilledDays }}</div>
              <div class="hours-stat-label">未录入天数</div>
            </div>
            <div class="hours-stat-item danger">
              <div class="hours-stat-value">{{ hoursStats.partialDays }}</div>
              <div class="hours-stat-label">录入不足天数</div>
            </div>
            <div class="hours-stat-item highlight">
              <div class="hours-stat-value">{{ hoursStats.gapHours }}</div>
              <div class="hours-stat-label">缺口工时 (h)</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import { getStatsApi } from '@/api/pm/stats'
import { getActiveCostWarningsApi } from '@/api/pm/costWarning'
import { getWorkHoursStatsApi } from '@/api/pm/workHours'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const stats = ref({ total: 0, draft: 0, pending: 0, approved: 0, rejected: 0 })
const recentProjects = ref([])
const costWarnings = ref([])
const hoursStats = ref(null)
const now = new Date()
const year = ref(now.getFullYear())
const month = ref(now.getMonth() + 1)

const statCards = [
  { label: '项目总数', value: 0, status: null },
  { label: '草稿', value: 0, status: 'DRAFT' },
  { label: '审批中', value: 0, status: 'PENDING_APPROVAL' },
  { label: '已通过', value: 0, status: 'APPROVED' }
]

onMounted(async () => {
  const pmId = userStore.userInfo?.userId || ''
  if (!pmId) return
  const res = await getStatsApi(pmId)
  stats.value = res.data

  // Update stat cards
  statCards[0].value = stats.value.total
  statCards[1].value = stats.value.draft
  statCards[2].value = stats.value.pending
  statCards[3].value = stats.value.approved

  loadWarnings()
  loadHoursStats()
})

onActivated(() => {
  loadWarnings()
})

function handleStatClick(card) {
  if (card.status) {
    router.push({ path: '/pm/charter', query: { status: card.status } })
  } else {
    router.push('/pm/charter')
  }
}

function handleViewProject(row) {
  router.push(`/pm/charter/detail/${row.id}`)
}

function handleNewProject() {
  router.push('/pm/charter/form')
}

function handleGoProjects() {
  router.push('/pm/charter')
}

function handleViewWarnings() {
  router.push('/pm/budget')
}

async function loadWarnings() {
  try {
    const res = await getActiveCostWarningsApi(5)
    costWarnings.value = res.data || []
  } catch (e) {
    // Ignore errors
  }
}

async function loadHoursStats() {
  try {
    const res = await getWorkHoursStatsApi(String(year.value), month.value)
    hoursStats.value = res.data
  } catch (e) {
    // Ignore errors
  }
}

function handleGoWorkHours() {
  router.push('/pm/work-hours')
}

function statusTagType(status) {
  const map = { DRAFT: 'info', PENDING_APPROVAL: 'warning', APPROVED: 'success', REJECTED: 'danger', CLOSED: 'info' }
  return map[status] || ''
}

function statusLabel(status) {
  const map = { DRAFT: '草稿', PENDING_APPROVAL: '审批中', APPROVED: '已通过', REJECTED: '已驳回', CLOSED: '已关闭' }
  return map[status] || status
}
</script>

<style scoped>
.dashboard-container {
  padding: 0;
}
.stat-cards {
  margin-bottom: 16px;
}
.stat-card {
  cursor: pointer;
  text-align: center;
  transition: all 0.3s;
}
.stat-card:hover {
  transform: translateY(-2px);
}
.stat-card :deep(.el-card__body) {
  padding: 20px;
}
.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 8px;
}
.stat-label {
  font-size: 14px;
  color: #909399;
}
.card-header {
  font-weight: bold;
  font-size: 15px;
}
.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.action-btn {
  width: 100%;
  height: 48px;
  font-size: 15px;
}
.action-btn :deep(.el-icon) {
  margin-right: 8px;
}
.content-row .el-card {
  height: 100%;
}
.warning-row {
  margin-top: 16px;
}
.hours-stats-row {
  margin-top: 16px;
}
.hours-stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  text-align: center;
}
.hours-stat-item {
  padding: 12px;
}
.hours-stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #409eff;
}
.hours-stat-item.warning .hours-stat-value {
  color: #e6a23c;
}
.hours-stat-item.danger .hours-stat-value {
  color: #f56c6c;
}
.hours-stat-item.highlight .hours-stat-value {
  color: #e6a23c;
}
.hours-stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}
</style>
