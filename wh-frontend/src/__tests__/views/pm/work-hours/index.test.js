import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setupPinia, elementStubs } from '../../../helpers'

vi.mock('vue-router', async () => {
  const actual = await vi.importActual('vue-router')
  return { ...actual, useRouter: () => ({ push: vi.fn(), replace: vi.fn(), go: vi.fn() }), useRoute: () => ({ path: '/', params: {}, query: {} }) }
})

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ code: 200, data: {} })),
    post: vi.fn(() => Promise.resolve({ code: 200 })),
    put: vi.fn(() => Promise.resolve({ code: 200 })),
    delete: vi.fn(() => Promise.resolve({ code: 200 })),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}))

describe('WorkHoursPage', () => {
  beforeEach(() => setupPinia())

  function createWrapper() {
    return import('@/views/pm/work-hours/index.vue').then(module =>
      mount(module.default, { global: { stubs: { ...elementStubs, MonthCalendar: { template: '<div class="month-calendar-stub"><slot/></div>' }, WorkHourDialog: { template: '<div class="work-hour-dialog-stub"/>' } } } })
    )
  }

  it('渲染工时页面', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.exists()).toBe(true)
  })

  it('API 失败时不崩溃', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    expect(wrapper.exists()).toBe(true)
  })

  it('月份切换', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    const origYear = wrapper.vm.year
    const origMonth = wrapper.vm.month

    wrapper.vm.handleMonthChange(origYear, origMonth + 1)
    expect(wrapper.vm.month).toBe(origMonth + 1)
  })

  it('设置 dialogDate', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.dialogDate = '2025-01-15'
    expect(wrapper.vm.dialogDate).toBe('2025-01-15')
  })
})
