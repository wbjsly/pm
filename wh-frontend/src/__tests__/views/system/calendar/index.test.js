import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setupPinia, elementStubs } from '../../../helpers'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

vi.mock('vue-router', async () => {
  const actual = await vi.importActual('vue-router')
  return { ...actual, useRouter: () => ({ push: vi.fn(), replace: vi.fn(), go: vi.fn() }), useRoute: () => ({ path: '/', params: {}, query: {} }) }
})

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ code: 200, data: [] })),
    post: vi.fn(() => Promise.resolve({ code: 200 })),
    put: vi.fn(() => Promise.resolve({ code: 200 })),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}))

describe('CalendarPage', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
  })

  function createWrapper() {
    return import('@/views/system/calendar/index.vue').then(m => mount(m.default, { global: { stubs: elementStubs } }))
  }

  it('渲染工作日历页面', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.exists()).toBe(true)
  })

  it('loadData 加载日历数据并转为 map', async () => {
    request.get.mockResolvedValue({ code: 200, data: [{ calendarDate: '2026-01-05', dayType: 'WORKDAY' }] })
    const wrapper = await createWrapper()
    await wrapper.vm.loadData()
    expect(wrapper.vm.calendarData['2026-01-05'].dayType).toBe('WORKDAY')
    expect(wrapper.vm.loading).toBe(false)
  })

  it('loadData 失败不崩溃', async () => {
    request.get.mockRejectedValue(new Error('fail'))
    const wrapper = await createWrapper()
    await wrapper.vm.loadData()
    expect(wrapper.vm.loading).toBe(false)
  })

  it('formatDate 格式化日期', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.vm.formatDate(new Date(2026, 0, 5))).toBe('2026-01-05')
    expect(wrapper.vm.formatDate(new Date(2026, 11, 31))).toBe('2026-12-31')
  })

  it('getMonthWeeks 返回月份周结构', async () => {
    const wrapper = await createWrapper()
    const weeks = wrapper.vm.getMonthWeeks(2026, 1)
    expect(weeks.length).toBeGreaterThan(0)
    const totalDays = weeks.flat().length
    expect(totalDays).toBeGreaterThanOrEqual(31)
  })

  it('monthStatItems 统计工作日/周末/节假日', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.calendarData = {
      '2026-01-05': { dayType: 'HOLIDAY' },
    }
    const items = wrapper.vm.monthStatItems(1)
    expect(items).toBeTruthy()
  })

  it('handleDayClick 打开编辑弹窗', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.handleDayClick({ dateStr: '2026-01-05', type: 'WORKDAY', standardHours: '8', otherMonth: false })
    expect(wrapper.vm.editVisible).toBe(true)
    expect(wrapper.vm.editForm.date).toBe('2026-01-05')
  })

  it('handleDayClick 其他月份日期忽略', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.handleDayClick({ otherMonth: true })
    expect(wrapper.vm.editVisible).toBe(false)
  })

  it('saveDay 成功保存', async () => {
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    request.post.mockResolvedValue({ code: 200 })
    const wrapper = await createWrapper()
    wrapper.vm.editForm = { date: '2026-01-05', dayType: 'WORKDAY', holidayName: '', standardHours: 8 }
    await wrapper.vm.saveDay()
    expect(request.post).toHaveBeenCalledWith('/system/work-calendar', expect.any(Object))
    expect(wrapper.vm.editVisible).toBe(false)
  })

  it('saveDay 失败提示', async () => {
    vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
    request.post.mockRejectedValue(new Error('fail'))
    const wrapper = await createWrapper()
    wrapper.vm.editForm = { date: '2026-01-05', dayType: 'WORKDAY', holidayName: '', standardHours: 8 }
    await wrapper.vm.saveDay()
    expect(ElMessage.error).toHaveBeenCalled()
  })

  it('handleGenerate 成功生成', async () => {
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    request.post.mockResolvedValue({ code: 200 })
    const wrapper = await createWrapper()
    await wrapper.vm.handleGenerate()
    expect(request.post).toHaveBeenCalledWith('/system/work-calendar/generate', expect.any(Object))
    expect(wrapper.vm.generating).toBe(false)
  })

  it('handleGenerate 失败提示', async () => {
    vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
    request.post.mockRejectedValue(new Error('fail'))
    const wrapper = await createWrapper()
    await wrapper.vm.handleGenerate()
    expect(ElMessage.error).toHaveBeenCalled()
    expect(wrapper.vm.generating).toBe(false)
  })

  it('handleSave 提示已保存', async () => {
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    const wrapper = await createWrapper()
    wrapper.vm.handleSave()
    expect(ElMessage.success).toHaveBeenCalled()
  })

  it('yearOptions 包含当前年份', async () => {
    const wrapper = await createWrapper()
    const now = new Date().getFullYear()
    expect(wrapper.vm.yearOptions).toContain(now)
  })
})
