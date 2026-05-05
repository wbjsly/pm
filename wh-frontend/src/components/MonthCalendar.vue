<template>
  <div class="month-calendar">
    <div class="calendar-header">
      <el-button text @click="prevMonth">&lt;</el-button>
      <span class="calendar-title">{{ year }}年{{ month }}月</span>
      <el-button text @click="nextMonth">&gt;</el-button>
      <el-button text size="small" @click="goToday">本月</el-button>
    </div>

    <div class="calendar-grid">
      <!-- Day headers -->
      <div class="calendar-row header-row">
        <div v-for="d in weekDays" :key="d" class="calendar-cell header-cell">{{ d }}</div>
        <div class="week-sum-header"></div>
      </div>

      <!-- Calendar rows -->
      <div v-for="(week, wi) in calendarWeeks" :key="wi" class="calendar-row">
        <template v-for="(day, di) in week" :key="di">
          <div
            class="calendar-cell day-cell"
            :class="{
              'other-month': day.otherMonth,
              'today': day.isToday,
              'day-workday': !day.otherMonth && day.dayType === 'WORKDAY',
              'day-weekend': !day.otherMonth && day.dayType === 'WEEKEND',
              'day-holiday': !day.otherMonth && day.dayType === 'HOLIDAY'
            }"
            @click="handleDayClick(day)"
          >
            <div class="day-header">
              <span class="day-left">
                <span class="day-number">{{ day.date }}</span>
                <span v-if="!day.otherMonth && day.dayType" class="day-type-badge">{{ dayTypeLabel(day.dayType) }}</span>
                <span v-if="day.holidayName" class="holiday-badge">{{ day.holidayName }}</span>
              </span>
              <span v-if="!day.otherMonth && dayTotal(day) > 0" class="day-hours">{{ dayTotal(day) }}h</span>
            </div>

            <!-- Work hour entries -->
            <div
              v-for="entry in day.entries"
              :key="entry.id"
              class="hour-entry"
              :class="'status-' + entry.status.toLowerCase()"
              @click.stop="handleEntryClick(entry)"
            >
              <span class="entry-project">{{ entry.projectShortName }}:{{ entry.hours }}h</span>
              <el-icon v-if="entry.status === 'REJECTED'" class="resubmit-icon" @click.stop="handleResubmit(entry)">
                <RefreshRight />
              </el-icon>
              <el-icon v-if="entry.status === 'DRAFT'" class="delete-icon" @click.stop="handleDelete(entry)">
                <Delete />
              </el-icon>
            </div>

            <el-icon v-if="!day.otherMonth && !day.isWorkDay" class="add-icon" @click.stop="handleAdd(day)">
              <Plus />
            </el-icon>
            <el-icon v-else-if="!day.otherMonth" class="add-icon" @click.stop="handleAdd(day)">
              <Plus />
            </el-icon>
          </div>
        </template>

        <!-- Week sum column -->
        <div class="week-sum-cell">
          <div class="week-sum-label">第{{ wi + 1 }}周</div>
          <div class="week-sum-value">{{ weekSum(week) }}h</div>
        </div>
      </div>
    </div>

    <!-- Month summary -->
    <div class="month-summary">
      <span>本月汇总: <strong>{{ monthTotal }}</strong> 小时</span>
      <span v-if="stats" class="summary-stats">
        | 工作日: {{ stats.workDays }}天
        | 满额: {{ stats.filledDays }}天
        | 未录入: {{ stats.unfilledDays }}天
        | 不足: {{ stats.partialDays }}天
        | 缺口: {{ stats.gapHours }}h
      </span>
    </div>
  </div>
</template>

<script setup>
import { computed, watch, ref } from 'vue'
import { Plus, Delete, RefreshRight } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteWorkHourApi,
  resubmitWorkHourApi,
  getWorkHoursStatsApi
} from '@/api/pm/workHours'

const props = defineProps({
  year: { type: Number, required: true },
  month: { type: Number, required: true },
  workLogs: { type: Array, default: () => [] },
  workDays: { type: Array, default: () => [] },
  calendarMap: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['add', 'edit', 'resubmit', 'delete', 'month-change'])

const weekDays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

function dayTypeLabel(type) {
  const map = { WORKDAY: '工', WEEKEND: '休', HOLIDAY: '假' }
  return map[type] || ''
}

const stats = ref(null)
const monthTotal = computed(() => {
  return props.workLogs.reduce((sum, log) => sum + (parseFloat(log.hoursWorked) || 0), 0).toFixed(1)
})

// Build calendar weeks
const calendarWeeks = computed(() => {
  const firstDay = new Date(props.year, props.month - 1, 1)
  const lastDay = new Date(props.year, props.month, 0)
  const today = new Date()
  today.setHours(0, 0, 0, 0)

  // Adjust for Monday as first day (0=Sunday, 1=Monday...)
  let startOffset = firstDay.getDay() - 1
  if (startOffset < 0) startOffset = 6 // Sunday -> 6

  const startDate = new Date(firstDay)
  startDate.setDate(startDate.getDate() - startOffset)

  const weeks = []
  let current = new Date(startDate)

  while (current <= lastDay || weeks.length < 1) {
    const week = []
    for (let i = 0; i < 7; i++) {
      const dateStr = formatDate(current)
      const dayNum = current.getDate()
      const isCurrentMonth = current.getMonth() === props.month - 1
      const isToday = current.getTime() === today.getTime()
      const isWorkDay = props.workDays.includes(dateStr)

      // Find work logs for this day
      const dayLogs = props.workLogs.filter(l => l.logDate === dateStr)
      const entries = dayLogs.map(l => ({
        id: l.id,
        projectId: l.projectId,
        projectShortName: l.projectShortName || '?',
        hours: parseFloat(l.hoursWorked) || 0,
        status: l.status,
        logDate: l.logDate
      }))

      const calData = props.calendarMap[dateStr] || {}
      const dayType = calData.dayType || null

      week.push({
        date: dayNum,
        dateStr,
        otherMonth: !isCurrentMonth,
        isToday,
        isWorkDay,
        dayType,
        holidayName: calData.holidayName || null,
        entries
      })
      current.setDate(current.getDate() + 1)
    }
    weeks.push(week)
    if (current > lastDay && current.getDay() === 1) break
    if (weeks.length >= 6) break
  }

  return weeks
})

function weekSum(week) {
  let total = 0
  for (const day of week) {
    if (day.entries) {
      total += day.entries.reduce((s, e) => s + e.hours, 0)
    }
  }
  return total.toFixed(1)
}

function dayTotal(day) {
  if (!day.entries || day.entries.length === 0) return 0
  return day.entries.reduce((s, e) => s + e.hours, 0).toFixed(1)
}

function formatDate(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function prevMonth() {
  let y = props.year
  let m = props.month - 1
  if (m < 1) { m = 12; y-- }
  emit('month-change', y, m)
}

function nextMonth() {
  let y = props.year
  let m = props.month + 1
  if (m > 12) { m = 1; y++ }
  emit('month-change', y, m)
}

function goToday() {
  const now = new Date()
  emit('month-change', now.getFullYear(), now.getMonth() + 1)
}

function handleAdd(day) {
  if (day.otherMonth) {
    const d = new Date(day.dateStr)
    emit('month-change', d.getFullYear(), d.getMonth() + 1)
    return
  }
  emit('add', day.dateStr)
}

function handleDayClick(day) {
  if (day.otherMonth) {
    const d = new Date(day.dateStr)
    emit('month-change', d.getFullYear(), d.getMonth() + 1)
  }
}

function handleEntryClick(entry) {
  emit('edit', entry)
}

function handleResubmit(entry) {
  emit('resubmit', entry)
}

async function handleDelete(entry) {
  try {
    await ElMessageBox.confirm('确定删除此工时记录吗？', '提示', { type: 'warning' })
    await deleteWorkHourApi(entry.id)
    ElMessage.success('删除成功')
    // Trigger reload
    emit('delete', entry.id)
  } catch (e) {
    // Cancelled
  }
}

watch(() => [props.year, props.month], async () => {
  try {
    const res = await getWorkHoursStatsApi(String(props.year), props.month)
    stats.value = res.data
  } catch (e) {
    // Ignore
  }
}, { immediate: true })
</script>

<style scoped>
.month-calendar {
  user-select: none;
}
.calendar-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 12px 0;
  font-size: 18px;
  font-weight: bold;
}
.calendar-grid {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
}
.calendar-row {
  display: flex;
  border-bottom: 1px solid #ebeef5;
}
.calendar-row:last-child {
  border-bottom: none;
}
.calendar-cell {
  flex: 1;
  min-height: 100px;
  padding: 4px;
  border-right: 1px solid #ebeef5;
  position: relative;
}
.calendar-cell:last-child {
  border-right: none;
}
.header-cell {
  min-height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  font-weight: bold;
  font-size: 13px;
  color: #606266;
}
.day-cell.other-month {
  background: #f9f9f9;
  color: #c0c4cc;
}
.day-cell.today {
  background: #ecf5ff;
}
.day-cell.day-workday {
  background: #f0f9eb;
}
.day-cell.day-weekend {
  background: #fef0f0;
}
.day-cell.day-holiday {
  background: #fdf6ec;
}
.day-header {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  margin-bottom: 4px;
}
.day-left {
  display: flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}
.day-hours {
  justify-self: center;
  font-size: 12px;
  color: #409eff;
  font-weight: bold;
}
.day-number {
  font-size: 13px;
  font-weight: bold;
}
.holiday-badge {
  font-size: 11px;
  color: #e6a23c;
  margin-left: 4px;
  font-weight: normal;
}
.day-type-badge {
  font-size: 11px;
  color: #909399;
  margin-left: 2px;
  font-weight: normal;
}
.day-hours {
  font-size: 12px;
  color: #409eff;
  font-weight: bold;
}
.hour-entry {
  font-size: 12px;
  padding: 2px 6px;
  margin: 2px 0;
  border-radius: 3px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.hour-entry:hover {
  opacity: 0.85;
}
.status-draft {
  background: #fdf6ec;
  color: #e6a23c;
  border-left: 3px solid #e6a23c;
}
.status-approved {
  background: #f0f9eb;
  color: #67c23a;
  border-left: 3px solid #67c23a;
}
.status-rejected {
  background: #fef0f0;
  color: #f56c6c;
  border-left: 3px solid #f56c6c;
}
.entry-project {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.resubmit-icon, .delete-icon {
  font-size: 14px;
  cursor: pointer;
  flex-shrink: 0;
  margin-left: 4px;
}
.resubmit-icon:hover { color: #409eff; }
.delete-icon:hover { color: #f56c6c; }
.add-icon {
  position: absolute;
  top: 4px;
  right: 4px;
  font-size: 18px;
  color: #409eff;
  cursor: pointer;
}
.add-icon:hover {
  color: #66b1ff;
}
.week-sum-cell {
  width: 80px;
  min-width: 80px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  font-size: 12px;
  border-right: none !important;
}
.week-sum-header {
  width: 80px;
  min-width: 80px;
  flex-shrink: 0;
  background: #f5f7fa;
  border-right: none !important;
}
.week-sum-label {
  color: #909399;
}
.week-sum-value {
  color: #409eff;
  font-weight: bold;
}
.month-summary {
  margin-top: 12px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.summary-stats {
  color: #606266;
}
</style>
