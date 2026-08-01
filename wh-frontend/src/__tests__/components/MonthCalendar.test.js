import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'

vi.mock('@/api/pm/workHours', () => ({
  deleteWorkHourApi: vi.fn(),
  resubmitWorkHourApi: vi.fn(),
  getWorkHoursStatsApi: vi.fn(),
}))

vi.mock('@element-plus/icons-vue', () => ({
  Plus: { name: 'Plus', template: '<i class="el-icon-plus" />' },
  Delete: { name: 'Delete', template: '<i class="el-icon-delete" />' },
  RefreshRight: { name: 'RefreshRight', template: '<i class="el-icon-refresh-right" />' },
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn(), info: vi.fn() },
    ElMessageBox: { confirm: vi.fn(() => Promise.resolve()) },
  }
})

import * as workHoursApi from '@/api/pm/workHours'
import { useDictStore } from '@/store/dict'

describe('MonthCalendar', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    workHoursApi.getWorkHoursStatsApi.mockResolvedValue({ code: 200, data: null })
    workHoursApi.deleteWorkHourApi.mockResolvedValue({ code: 200 })
    workHoursApi.resubmitWorkHourApi.mockResolvedValue({ code: 200 })
  })

  async function createWrapper(props = {}) {
    setActivePinia(createPinia())
    const dictStore = useDictStore()
    dictStore.dictMap = {
      CALENDAR_DAY_TYPE: {
        WORKDAY: { label: '工作日', tagType: '' },
        WEEKEND: { label: '周末', tagType: 'warning' },
        HOLIDAY: { label: '节假日', tagType: 'danger' },
      },
    }

    const m = await import('@/components/MonthCalendar.vue')
    return mount(m.default, {
      global: {
        stubs: {
          'el-button': { template: '<button @click="$emit(\'click\')"><slot/></button>' },
          'el-icon': { template: '<i class="el-icon"><slot/></i>' },
        },
      },
      props: {
        year: 2025,
        month: 1,
        workLogs: [
          { id: 'wl1', logDate: '2025-01-15', projectId: 'p1', projectShortName: 'TP', hoursWorked: '8', status: 'APPROVED', workDescription: '开发' },
        ],
        workDays: ['2025-01-15'],
        calendarMap: {
          '2025-01-15': { dayType: 'WORKDAY' },
          '2025-01-01': { dayType: 'HOLIDAY', holidayName: '元旦' },
        },
        ...props,
      },
    })
  }

  it('渲染日历组件', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.exists()).toBe(true)
  })

  it('显示年份和月份', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.text()).toContain('2025年1月')
  })

  it('月总工时计算', async () => {
    const wrapper = await createWrapper()
    expect(parseFloat(wrapper.vm.monthTotal)).toBeGreaterThan(0)
  })

  it('上月按钮触发 prevMonth', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.prevMonth()
    expect(wrapper.emitted('month-change')).toBeTruthy()
  })

  it('下月按钮触发 nextMonth', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.nextMonth()
    expect(wrapper.emitted('month-change')).toBeTruthy()
  })

  it('本月按钮触发 goToday', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.goToday()
    expect(wrapper.emitted('month-change')).toBeTruthy()
  })

  it('点击日期触发 add', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.handleAdd({ dateStr: '2025-01-15', otherMonth: false })
    expect(wrapper.emitted('add')).toBeTruthy()
  })

  it('点击非本月日期切换月份', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.handleAdd({ dateStr: '2024-12-25', otherMonth: true })
    expect(wrapper.emitted('month-change')).toBeTruthy()
  })

  it('点击条目触发 edit', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.handleEntryClick({ id: 'wl1', logDate: '2025-01-15' })
    expect(wrapper.emitted('edit')).toBeTruthy()
  })

  it('重新提交', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.handleResubmit({ id: 'wl1', status: 'REJECTED' })
    expect(wrapper.emitted('resubmit')).toBeTruthy()
  })

  it('删除工时', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.handleDelete({ id: 'wl1' })
    expect(workHoursApi.deleteWorkHourApi).toHaveBeenCalledWith('wl1')
  })

  it('dayTotal 计算', async () => {
    const wrapper = await createWrapper()
    const day = { otherMonth: false, entries: [{ hours: 8 }, { hours: 4 }] }
    expect(wrapper.vm.dayTotal(day)).toBe('12.0')
  })

  it('空条目 dayTotal 返回 0', async () => {
    const wrapper = await createWrapper()
    const day = { otherMonth: false, entries: [] }
    expect(wrapper.vm.dayTotal(day)).toBe(0)
  })

  it('weekSum 计算', async () => {
    const wrapper = await createWrapper()
    const week = [{ entries: [{ hours: 8 }] }, { entries: [{ hours: 4 }] }]
    expect(wrapper.vm.weekSum(week)).toBe('12.0')
  })
})
