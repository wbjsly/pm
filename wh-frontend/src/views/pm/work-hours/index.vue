<template>
  <div class="work-hours-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>工时管理</span>
          <el-button v-if="canApprove" type="primary" size="small" @click="$router.push('/pm/work-hours/approval')">
            工时审批
          </el-button>
        </div>
      </template>

      <MonthCalendar
        :year="year"
        :month="month"
        :work-logs="workLogs"
        :work-days="workDays"
        :calendar-map="calendarMap"
        @add="handleAdd"
        @edit="handleEdit"
        @delete="loadData"
        @resubmit="handleResubmit"
        @month-change="handleMonthChange"
      />
    </el-card>

    <WorkHourDialog
      ref="dialogRef"
      :date-str="dialogDate"
      :edit-entry="editEntry"
      @save="loadData"
      @delete="loadData"
      @resubmit="loadData"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onActivated } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import MonthCalendar from '@/components/MonthCalendar.vue'
import WorkHourDialog from '@/components/WorkHourDialog.vue'
import { getWorkHoursApi, resubmitWorkHourApi } from '@/api/pm/workHours'
import { getCharterListApi } from '@/api/pm/charter'
import { getWorkCalendarMonthApi } from '@/api/system/workCalendar'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const now = new Date()
const year = ref(route.query.year ? parseInt(route.query.year) : now.getFullYear())
const month = ref(route.query.month ? parseInt(route.query.month) : now.getMonth() + 1)
const workLogs = ref([])
const workDays = ref([])
const calendarMap = ref({})
const dialogRef = ref(null)
const dialogDate = ref('')
const editEntry = ref(null)
const myProjects = ref([])

const canApprove = computed(() => {
  const userId = userStore.userInfo?.userId
  return myProjects.value.some(p => p.pmId === userId)
})

function handleAdd(dateStr) {
  dialogDate.value = dateStr
  editEntry.value = null
  dialogRef.value.open()
}

function handleEdit(entry) {
  dialogDate.value = entry.logDate
  editEntry.value = entry
  dialogRef.value.open()
}

function handleResubmit(entry) {
  resubmitWorkHourApi(entry.id).then(() => {
    ElMessage.success('重新提交成功')
    loadData()
  }).catch(e => {
    ElMessage.error('重新提交失败')
  })
}

async function handleMonthChange(y, m) {
  year.value = y
  month.value = m
  await loadCalendar()
  loadWorkDays()
  loadData()
}

async function loadData() {
  try {
    const res = await getWorkHoursApi(String(year.value), month.value)
    workLogs.value = res.data || []
  } catch (e) {
    // Ignore
  }
}

async function loadWorkDays() {
  // Use work calendar data: WORKDAY types are work days
  const y = year.value
  const m = month.value
  const days = []
  for (const [dateStr, data] of Object.entries(calendarMap.value)) {
    if (dateStr.startsWith(`${y}-${String(m).padStart(2, '0')}`) && data.dayType === 'WORKDAY') {
      days.push(dateStr)
    }
  }
  workDays.value = days
}

async function loadCalendar() {
  try {
    const res = await getWorkCalendarMonthApi(year.value, month.value)
    // Backend returns a Map<dateStr, calendarData>, already keyed by date
    calendarMap.value = res.data || {}
  } catch (e) {
    calendarMap.value = {}
  }
}

async function loadMyProjects() {
  try {
    const res = await getCharterListApi({ pageNum: 1, pageSize: 100 })
    myProjects.value = res.data?.records || []
  } catch (e) {
    // Ignore
  }
}

onMounted(async () => {
  await loadCalendar()
  loadWorkDays()
  await Promise.all([loadData(), loadMyProjects()])
})

onActivated(async () => {
  await loadCalendar()
  loadWorkDays()
  await Promise.all([loadData(), loadMyProjects()])
})
</script>

<style scoped>
.work-hours-page {
  padding: 0;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
