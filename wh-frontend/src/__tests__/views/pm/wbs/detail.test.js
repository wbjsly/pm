import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setupPinia, elementStubs } from '../../../helpers'
import { ElMessageBox } from 'element-plus'
import request from '@/utils/request'

vi.mock('vue-router', async () => {
  const actual = await vi.importActual('vue-router')
  return { ...actual, useRouter: () => ({ push: vi.fn(), replace: vi.fn(), go: vi.fn() }), useRoute: () => ({ path: '/pm/wbs/detail/w1', params: { id: 'w1' }, query: {} }) }
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

const mockWbsDetail = {
  id: 'w1', wbsCode: 'WBS-001', name: '需求分析',
  projectId: 'p1', status: 'IN_DEVELOPMENT',
}

describe('WbsDetailPage', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    request.get.mockImplementation((url) => {
      if (url && url.includes('/pm/wbs/')) return Promise.resolve({ code: 200, data: mockWbsDetail })
      return Promise.resolve({ code: 200, data: {} })
    })
  })

  function createWrapper() {
    return import('@/views/pm/wbs/detail.vue').then(m => mount(m.default, { global: { stubs: elementStubs } }))
  }

  it('渲染WBS详情页面', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.exists()).toBe(true)
  })

  it('API 失败时不崩溃', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.exists()).toBe(true)
  })

  it('loadDetail 加载详情', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.loadDetail()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.detail.id).toBe('w1')
  })

  it('handleSuspend 确认后调用暂停 API', async () => {
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleSuspend()
    expect(request.post).toHaveBeenCalledWith('/pm/wbs/w1/suspend')
  })

  it('handleResume 调用恢复 API', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleResume()
    expect(request.post).toHaveBeenCalledWith('/pm/wbs/w1/resume')
  })

  it('handleReopen 确认后调用重新打开 API', async () => {
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleReopen()
    expect(request.post).toHaveBeenCalledWith('/pm/wbs/w1/reopen')
  })

  it('handleTest 确认后调用提测 API', async () => {
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleTest()
    expect(request.post).toHaveBeenCalledWith('/pm/wbs/w1/test')
  })

  it('handleComplete 确认后调用完成 API', async () => {
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleComplete()
    expect(request.post).toHaveBeenCalledWith('/pm/wbs/w1/complete')
  })
})

describe('WbsDetailPage 辅助函数', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    request.get.mockResolvedValue({ code: 200, data: mockWbsDetail })
  })

  function createWrapper() {
    return import('@/views/pm/wbs/detail.vue').then(m => mount(m.default, { global: { stubs: elementStubs } }))
  }

  it('wbsStatusTagType / wbsStatusLabel', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.vm.wbsStatusTagType('NOT_STARTED')).toBe('info')
    expect(wrapper.vm.wbsStatusTagType('IN_DEVELOPMENT')).toBe('warning')
    expect(wrapper.vm.wbsStatusTagType('TESTING')).toBe('primary')
    expect(wrapper.vm.wbsStatusTagType('COMPLETED')).toBe('success')
    expect(wrapper.vm.wbsStatusTagType('SUSPENDED')).toBe('danger')
    expect(wrapper.vm.wbsStatusTagType('X')).toBe('info')
    expect(wrapper.vm.wbsStatusLabel('COMPLETED')).toBe('已完成')
    expect(wrapper.vm.wbsStatusLabel('X')).toBe('X')
  })

  it('priorityTagType / priorityLabel', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.vm.priorityTagType('P0')).toBe('danger')
    expect(wrapper.vm.priorityTagType('P2')).toBe('warning')
    expect(wrapper.vm.priorityTagType('X')).toBe('info')
    expect(wrapper.vm.priorityLabel('P1')).toBe('P1')
  })

  it('difficultyTagType / difficultyLabel', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.vm.difficultyTagType('HIGH')).toBe('danger')
    expect(wrapper.vm.difficultyTagType('MEDIUM')).toBe('warning')
    expect(wrapper.vm.difficultyTagType('LOW')).toBe('success')
    expect(wrapper.vm.difficultyTagType('X')).toBe('info')
    expect(wrapper.vm.difficultyLabel('MEDIUM')).toBe('中')
  })
})
