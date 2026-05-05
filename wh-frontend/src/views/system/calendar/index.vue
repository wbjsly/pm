<template>
  <div class="work-calendar-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>工作日历管理</span>
          <div class="header-actions">
            <el-select v-model="selectedYear" style="width: 100px" @change="loadData">
              <el-option v-for="y in yearOptions" :key="y" :label="y + '年'" :value="y" />
            </el-select>
            <el-button type="primary" @click="handleGenerate" :loading="generating">
              <el-icon><RefreshRight /></el-icon>
              一键生成
            </el-button>
            <el-button @click="handleSave">保存</el-button>
          </div>
        </div>
      </template>

      <div v-loading="loading" class="calendar-grid">
        <div v-for="m in 12" :key="m" class="month-block">
          <div class="month-title">
            <span>{{ selectedYear }}年{{ m }}月</span>
            <span class="month-stats">
              <template v-for="(item, idx) in monthStatItems(m)" :key="idx">
                <span :class="'stat-' + item.type">{{ item.text }}</span>
              </template>
            </span>
          </div>

          <!-- Day headers -->
          <div class="week-header">
            <span v-for="d in ['一','二','三','四','五','六','日']" :key="d">{{ d }}</span>
          </div>

          <!-- Days grid -->
          <div class="days-grid">
            <template v-for="(week, wi) in getMonthWeeks(selectedYear, m)" :key="wi">
              <div
                v-for="(day, di) in week"
                :key="di"
                class="day-cell"
                :class="{
                  'empty-cell': day.otherMonth,
                  'workday': !day.otherMonth && day.type === 'WORKDAY',
                  'weekend': !day.otherMonth && day.type === 'WEEKEND',
                  'holiday': !day.otherMonth && day.type === 'HOLIDAY'
                }"
                @click="!day.otherMonth && handleDayClick(day)"
              >
                <template v-if="!day.otherMonth">
                  <span class="day-num">{{ day.date }}</span>
                  <span class="day-type-tag">{{ typeLabel(day.type) }}</span>
                  <span v-if="day.holidayName" class="holiday-name">{{ day.holidayName }}</span>
                </template>
              </div>
            </template>
          </div>
        </div>
      </div>
    </el-card>

    <!-- Day edit dialog -->
    <el-dialog v-model="editVisible" title="编辑日期" width="360px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="日期">
          <span>{{ editForm.date }}</span>
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="editForm.dayType">
            <el-radio label="WORKDAY">工作日</el-radio>
            <el-radio label="WEEKEND">周末</el-radio>
            <el-radio label="HOLIDAY">节假日</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="名称" v-if="editForm.dayType === 'HOLIDAY'">
          <el-input v-model="editForm.holidayName" placeholder="如: 春节" />
        </el-form-item>
        <el-form-item label="标准工时">
          <el-input-number v-model="editForm.standardHours" :min="0" :max="24" :step="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDay">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { RefreshRight } from '@element-plus/icons-vue'
import {
  getWorkCalendarApi,
  generateWorkCalendarApi,
  updateWorkCalendarDayApi
} from '@/api/system/workCalendar'

const selectedYear = ref(new Date().getFullYear())
const yearOptions = computed(() => {
  const now = new Date().getFullYear()
  return [now - 1, now, now + 1, now + 2]
})
const loading = ref(false)
const generating = ref(false)
const editVisible = ref(false)
const editForm = ref({ date: '', dayType: 'WORKDAY', holidayName: '', standardHours: 8 })
const calendarData = ref({})

function typeLabel(type) {
  const map = { WORKDAY: '工', WEEKEND: '休', HOLIDAY: '假' }
  return map[type] || ''
}

function monthStatItems(month) {
  const weeks = getMonthWeeks(selectedYear.value, month)
  const counts = { WORKDAY: 0, WEEKEND: 0, HOLIDAY: 0 }
  for (const week of weeks) {
    for (const day of week) {
      if (!day.otherMonth && day.type && counts[day.type] !== undefined) {
        counts[day.type]++
      }
    }
  }
  const items = []
  if (counts.WORKDAY) items.push({ type: 'workday', text: `工${counts.WORKDAY}` })
  if (counts.WEEKEND) items.push({ type: 'weekend', text: `休${counts.WEEKEND}` })
  if (counts.HOLIDAY) items.push({ type: 'holiday', text: `假${counts.HOLIDAY}` })
  return items
}

function getMonthWeeks(year, month) {
  const firstDay = new Date(year, month - 1, 1)
  const lastDay = new Date(year, month, 0)

  let startOffset = firstDay.getDay() - 1
  if (startOffset < 0) startOffset = 6

  const startDate = new Date(firstDay)
  startDate.setDate(startDate.getDate() - startOffset)

  const weeks = []
  let current = new Date(startDate)

  while (current <= lastDay || weeks.length < 1) {
    const week = []
    for (let i = 0; i < 7; i++) {
      const dateStr = formatDate(current)
      const isCurrentMonth = current.getMonth() === month - 1
      const calData = calendarData.value[dateStr] || {}

      week.push({
        date: current.getDate(),
        dateStr,
        otherMonth: !isCurrentMonth,
        type: calData.dayType || null,
        holidayName: calData.holidayName || null,
        standardHours: calData.standardHours || '8'
      })
      current.setDate(current.getDate() + 1)
    }
    weeks.push(week)
    if (current > lastDay && current.getDay() === 1) break
    if (weeks.length >= 6) break
  }

  return weeks
}

function formatDate(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function handleDayClick(day) {
  if (day.otherMonth) return
  editForm.value = {
    date: day.dateStr,
    dayType: day.type || 'WORKDAY',
    holidayName: day.holidayName || '',
    standardHours: parseInt(day.standardHours) || 8
  }
  editVisible.value = true
}

async function saveDay() {
  try {
    await updateWorkCalendarDayApi({
      calendarDate: editForm.value.date,
      dayType: editForm.value.dayType,
      holidayName: editForm.value.holidayName,
      standardHours: String(editForm.value.standardHours)
    })
    ElMessage.success('保存成功')
    editVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

async function handleGenerate() {
  try {
    generating.value = true
    await generateWorkCalendarApi(String(selectedYear.value))
    ElMessage.success('生成成功')
    loadData()
  } catch (e) {
    ElMessage.error('生成失败')
  } finally {
    generating.value = false
  }
}

async function handleSave() {
  ElMessage.success('所有修改已保存')
}

async function loadData() {
  loading.value = true
  try {
    const res = await getWorkCalendarApi(String(selectedYear.value))
    const map = {}
    for (const item of (res.data || [])) {
      map[item.calendarDate] = item
    }
    calendarData.value = map
  } catch (e) {
    // Ignore
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.calendar-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.month-block {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
}
.month-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  font-weight: bold;
  background: #f5f7fa;
  font-size: 14px;
}
.month-stats {
  font-size: 12px;
  font-weight: normal;
  display: flex;
  gap: 8px;
}
.stat-workday {
  color: #67c23a;
}
.stat-weekend {
  color: #f56c6c;
}
.stat-holiday {
  color: #e6a23c;
}
.week-header {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  background: #fafafa;
  font-size: 12px;
  color: #909399;
}
.week-header span {
  text-align: center;
  padding: 4px 0;
}
.days-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
}
.day-cell {
  min-height: 36px;
  padding: 2px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: 1px solid #f0f0f0;
  font-size: 12px;
}
.day-cell:hover {
  background: #ecf5ff;
}
.day-cell.empty-cell {
  background: #fafafa;
  cursor: default;
  border-color: transparent;
}
.day-cell.workday {
  background: #f0f9eb;
}
.day-cell.weekend {
  background: #fef0f0;
}
.day-cell.holiday {
  background: #fdf6ec;
}
.day-num {
  font-size: 13px;
}
.day-type-tag {
  font-size: 10px;
  color: #909399;
}
.holiday-name {
  font-size: 10px;
  color: #e6a23c;
}
</style>
