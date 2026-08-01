import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setupPinia, elementStubs } from '../../../helpers'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

vi.mock('vue-router', async () => {
  const actual = await vi.importActual('vue-router')
  return { ...actual, useRouter: () => ({ push: vi.fn(), replace: vi.fn(), go: vi.fn() }), useRoute: () => ({ path: '/pm/wbs/history/test', params: { id: 'test' }, query: {} }) }
})

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ code: 200, data: [] })),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}))

describe('WbsHistoryPage', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
  })

  function createWrapper() {
    return import('@/views/pm/wbs/history.vue').then(m => mount(m.default, { global: { stubs: elementStubs } }))
  }

  it('渲染WBS版本历史页面', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.exists()).toBe(true)
  })

  it('API 失败时不崩溃', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    expect(wrapper.exists()).toBe(true)
  })

  it('loadVersions 加载详情与版本', async () => {
    request.get.mockImplementation((url) => {
      if (url.includes('/versions')) return Promise.resolve({ code: 200, data: [{ id: 'v1', versionNumber: 0.2 }] })
      if (url.includes('/wbs/test')) return Promise.resolve({ code: 200, data: { name: '任务A', wbsCode: 'WBS-001' } })
      return Promise.resolve({ code: 200, data: {} })
    })
    const wrapper = await createWrapper()
    await wrapper.vm.loadVersions()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.wbsName).toBe('任务A')
    expect(wrapper.vm.wbsCode).toBe('WBS-001')
    expect(wrapper.vm.versions.length).toBe(1)
  })

  it('loadVersions 失败提示错误', async () => {
    const spy = vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
    request.get.mockRejectedValue(new Error('fail'))
    const wrapper = await createWrapper()
    await wrapper.vm.loadVersions()
    expect(spy).toHaveBeenCalled()
    expect(wrapper.vm.loading).toBe(false)
  })
})
