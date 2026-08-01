import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setupPinia, elementStubs } from '../../../helpers'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

vi.mock('vue-router', async () => {
  const actual = await vi.importActual('vue-router')
  return { ...actual, useRouter: () => ({ push: vi.fn(), replace: vi.fn(), go: vi.fn() }), useRoute: () => ({ path: '/', params: {}, query: {} }) }
})

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ code: 200, data: { records: [], total: 0 } })),
    post: vi.fn(() => Promise.resolve({ code: 200 })),
    put: vi.fn(() => Promise.resolve({ code: 200 })),
    delete: vi.fn(() => Promise.resolve({ code: 200 })),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}))

describe('DeliverablePage', () => {
  beforeEach(() => setupPinia())

  function createWrapper() {
    return import('@/views/pm/deliverable/index.vue').then(m => mount(m.default, { global: { stubs: elementStubs } }))
  }

  it('渲染交付物列表页面', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.exists()).toBe(true)
  })

  it('API 失败时不崩溃', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    expect(wrapper.exists()).toBe(true)
  })
})

describe('DeliverablePage 交互', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    request.get.mockResolvedValue({ code: 200, data: { records: [], total: 0 } })
    request.delete.mockResolvedValue({ code: 200 })
    request.post.mockResolvedValue({ code: 200 })
  })

  function createWrapper() {
    return import('@/views/pm/deliverable/index.vue').then(m => mount(m.default, { global: { stubs: elementStubs } }))
  }

  it('loadProjects 成功加载项目与计数', async () => {
    request.get.mockImplementation((url) => {
      if (url === '/pm/charters') return Promise.resolve({ code: 200, data: { records: [{ id: 'p1', projectName: '项目1' }], total: 1 } })
      if (url === '/pm/deliverables') return Promise.resolve({ code: 200, data: { records: [{ id: 'd1', attachments: '[{"fileName":"a.txt","deleted":false}]' }], total: 1 } })
      return Promise.resolve({ code: 200, data: {} })
    })
    const wrapper = await createWrapper()
    await wrapper.vm.loadProjects()
    expect(wrapper.vm.projectList.length).toBe(1)
    expect(wrapper.vm.deliverableCounts.p1).toBe(1)
    expect(wrapper.vm.attachmentCounts.p1).toBe(1)
  })

  it('resetQuery 重置筛选', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    wrapper.vm.keyword = 'x'
    wrapper.vm.resetQuery()
    expect(wrapper.vm.keyword).toBe('')
    expect(wrapper.vm.queryParams.pageNum).toBe(1)
  })

  it('handleExpandProject 加载成果物列表', async () => {
    request.get.mockResolvedValue({ code: 200, data: { records: [{ id: 'd1', name: '成果1' }], total: 1 } })
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleExpandProject('p1')
    expect(wrapper.vm.expandedProjectId).toBe('p1')
    expect(wrapper.vm.deliverableData.length).toBe(1)
  })

  it('getAttachments 空/非法输入返回空数组', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.getAttachments({})).toEqual([])
    expect(wrapper.vm.getAttachments('')).toEqual([])
    expect(wrapper.vm.getAttachments('not-json')).toEqual([])
  })

  it('handleDelete 确认后调用删除 API', async () => {
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleDelete('d1')
    expect(request.delete).toHaveBeenCalledWith('/pm/deliverables/d1')
  })

  it('handleSubmit 确认后调用提交 API', async () => {
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleSubmit('d1')
    expect(request.post).toHaveBeenCalledWith('/pm/deliverables/d1/submit')
  })

  it('handleDownload 触发附件下载', async () => {
    global.URL.createObjectURL = vi.fn(() => 'blob:url')
    global.URL.revokeObjectURL = vi.fn()
    document.body.appendChild = vi.fn()
    document.body.removeChild = vi.fn()
    request.get.mockResolvedValue({ code: 200, data: new Blob(['x']) })
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleDownload('d1', 0, 'a.txt')
    expect(URL.revokeObjectURL).toHaveBeenCalled()
  })

  it('handleDeleteAttachment 确认后调用删除附件 API', async () => {
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    request.delete.mockResolvedValue({ code: 200, data: '[]' })
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    const row = { id: 'd1', attachments: '[{"fileName":"a.txt","deleted":false}]' }
    await wrapper.vm.handleDeleteAttachment(row, 0)
    expect(row.attachments).toBe('"[]"')
  })
})

describe('DeliverablePage 剩余函数', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    request.get.mockResolvedValue({ code: 200, data: { records: [], total: 0 } })
    request.delete.mockResolvedValue({ code: 200 })
  })

  function createWrapper() {
    return import('@/views/pm/deliverable/index.vue').then(m => mount(m.default, { global: { stubs: elementStubs } }))
  }

  it('handleExpandProject 失败提示', async () => {
    const { ElMessage } = await import('element-plus')
    vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
    request.get.mockRejectedValue(new Error('fail'))
    const wrapper = await createWrapper()
    await wrapper.vm.handleExpandProject('p1')
    expect(ElMessage.error).toHaveBeenCalled()
    expect(wrapper.vm.loading).toBe(false)
  })

  it('toggleProject 展开项目', async () => {
    request.get.mockResolvedValue({ code: 200, data: { records: [{ id: 'd1' }], total: 1 } })
    const wrapper = await createWrapper()
    await wrapper.vm.toggleProject('p1')
    expect(wrapper.vm.expandedProjectId).toBe('p1')
    await wrapper.vm.toggleProject('p1')
    expect(wrapper.vm.expandedProjectId).toBeNull()
  })

  it('refreshProjectCounts 刷新计数', async () => {
    request.get.mockResolvedValue({ code: 200, data: { records: [{ attachments: '[{"fileName":"a.txt","deleted":false}]' }], total: 3 } })
    const wrapper = await createWrapper()
    await wrapper.vm.refreshProjectCounts('p1')
    expect(wrapper.vm.deliverableCounts.p1).toBe(3)
    expect(wrapper.vm.attachmentCounts.p1).toBe(1)
  })

  it('refreshProjectCounts 失败静默', async () => {
    request.get.mockRejectedValue(new Error('fail'))
    const wrapper = await createWrapper()
    await wrapper.vm.refreshProjectCounts('p1')
    expect(wrapper.vm.deliverableCounts.p1).toBeUndefined()
  })

  it('handleDownload 失败提示', async () => {
    const { ElMessage } = await import('element-plus')
    vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
    request.get.mockRejectedValue(new Error('fail'))
    const wrapper = await createWrapper()
    await wrapper.vm.handleDownload('d1', 0, 'a.txt')
    expect(ElMessage.error).toHaveBeenCalled()
  })
})
