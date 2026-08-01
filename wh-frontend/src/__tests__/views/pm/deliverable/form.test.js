import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setupPinia, elementStubs } from '../../../helpers'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const { routeMock } = vi.hoisted(() => ({
  routeMock: { path: '/pm/deliverable/form', params: {}, query: { projectId: 'p1' } },
}))

vi.mock('vue-router', async () => {
  const actual = await vi.importActual('vue-router')
  return { ...actual, useRouter: () => ({ push: vi.fn(), replace: vi.fn(), go: vi.fn(), back: vi.fn() }), useRoute: () => routeMock }
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

describe('DeliverableFormPage', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    request.get.mockResolvedValue({ code: 200, data: { records: [{ id: 'p1', projectName: '项目1' }], total: 1 } })
    request.post.mockResolvedValue({ code: 200 })
    request.put.mockResolvedValue({ code: 200 })
    request.delete.mockResolvedValue({ code: 200 })
  })

  function createWrapper(validateImpl) {
    const formStub = validateImpl === undefined
      ? { template: '<div><slot/></div>' }
      : { template: '<div><slot/></div>', methods: { validate: validateImpl } }
    return import('@/views/pm/deliverable/form.vue').then(m => mount(m.default, { global: { stubs: { ...elementStubs, 'el-form': formStub } } }))
  }

  it('渲染交付物创建页面', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.exists()).toBe(true)
  })

  it('API 失败时不崩溃', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    expect(wrapper.exists()).toBe(true)
  })

  it('loadProjectOptions 加载已审批项目', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.loadProjectOptions()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.projectOptions.length).toBe(1)
    expect(request.get).toHaveBeenCalledWith('/pm/charters', expect.any(Object))
  })

  it('initPage 非编辑模式初始化空表单', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.initPage()
    expect(wrapper.vm.form.projectId).toBe('p1')
    expect(wrapper.vm.form.name).toBe('')
  })

  it('handleFileSelect 上传成功', async () => {
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    request.post.mockResolvedValue({ code: 200, data: [{ fileName: 'a.txt' }] })
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleFileSelect({ raw: new Blob(['x']) })
    expect(wrapper.vm.attachments.length).toBe(1)
    expect(wrapper.vm.uploading).toBe(false)
  })

  it('handleFileSelect 上传失败提示', async () => {
    vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
    request.post.mockRejectedValue(new Error('fail'))
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleFileSelect({ raw: new Blob(['x']) })
    expect(ElMessage.error).toHaveBeenCalled()
  })

  it('handleRemoveAttachment 确认后删除', async () => {
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    request.delete.mockResolvedValue({ code: 200, data: [] })
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    wrapper.vm.attachments = [{ fileName: 'a.txt' }]
    await wrapper.vm.handleRemoveAttachment(0)
    expect(request.delete).toHaveBeenCalledWith('/pm/deliverables/undefined/attachments/0')
  })

  it('handleSave 创建成功跳转列表', async () => {
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    wrapper.vm.form.projectId = 'p1'
    wrapper.vm.form.name = '新成果'
    wrapper.vm.form.projectId = 'p1'
    wrapper.vm.form.name = '新成果'
    const wrapper2 = await createWrapper(() => Promise.resolve(true))
    wrapper2.vm.form.projectId = 'p1'
    wrapper2.vm.form.name = '新成果'
    await wrapper2.vm.handleSave()
    expect(request.post).toHaveBeenCalledWith('/pm/deliverables', expect.objectContaining({ name: '新成果' }))
  })

  it('handleSave 校验失败不提交', async () => {
    const wrapper = await createWrapper(() => Promise.reject())
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleSave()
    expect(request.post).not.toHaveBeenCalled()
  })
})

describe('DeliverableFormPage 编辑模式', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    request.get.mockResolvedValue({ code: 200, data: {} })
    request.put.mockResolvedValue({ code: 200 })
  })

  it('loadDetail 编辑模式填充表单', async () => {
    routeMock.params = { id: 'd1' }
    routeMock.path = '/pm/deliverable/form/d1'
    request.get.mockImplementation((url) => {
      if (url === '/pm/deliverables/d1') return Promise.resolve({ code: 200, data: { projectId: 'p1', name: '成果1', description: '描述', plannedDeliveryDate: '2026-12-31', remarks: '备注', attachments: '[{"fileName":"a.txt","deleted":false}]' } })
      if (url === '/pm/charters') return Promise.resolve({ code: 200, data: { records: [], total: 0 } })
      return Promise.resolve({ code: 200, data: {} })
    })
    const wrapper = await import('@/views/pm/deliverable/form.vue').then(m => mount(m.default, { global: { stubs: elementStubs } }))
    await wrapper.vm.loadDetail()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.form.name).toBe('成果1')
    expect(wrapper.vm.form.projectId).toBe('p1')
    expect(wrapper.vm.attachments.length).toBe(1)
    routeMock.params = {}
    routeMock.path = '/pm/deliverable/form'
  })

  it('loadDetail 非法附件 JSON 容错', async () => {
    routeMock.params = { id: 'd1' }
    routeMock.path = '/pm/deliverable/form/d1'
    request.get.mockImplementation((url) => {
      if (url === '/pm/deliverables/d1') return Promise.resolve({ code: 200, data: { projectId: 'p1', name: '成果1', attachments: 'not-json' } })
      if (url === '/pm/charters') return Promise.resolve({ code: 200, data: { records: [], total: 0 } })
      return Promise.resolve({ code: 200, data: {} })
    })
    const wrapper = await import('@/views/pm/deliverable/form.vue').then(m => mount(m.default, { global: { stubs: elementStubs } }))
    await wrapper.vm.loadDetail()
    expect(wrapper.vm.attachments).toEqual([])
    routeMock.params = {}
    routeMock.path = '/pm/deliverable/form'
  })
})
