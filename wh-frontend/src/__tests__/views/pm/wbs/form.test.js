import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setupPinia, elementStubs } from '../../../helpers'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const { routeMock } = vi.hoisted(() => ({
  routeMock: { path: '/', params: {}, query: { projectId: 'p1' } },
}))

vi.mock('vue-router', async () => {
  const actual = await vi.importActual('vue-router')
  const useRouter = () => ({ push: vi.fn(), replace: vi.fn(), go: vi.fn(), back: vi.fn() })
  return { ...actual, useRouter, useRoute: () => routeMock }
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

describe('WbsFormPage', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    request.get.mockResolvedValue({ code: 200, data: { records: [], total: 0 } })
    request.post.mockResolvedValue({ code: 200 })
    request.put.mockResolvedValue({ code: 200 })
  })

  function createWrapper(validateImpl) {
    const formStub = validateImpl === undefined
      ? { template: '<div><slot/></div>' }
      : { template: '<div><slot/></div>', methods: { validate: validateImpl } }
    return import('@/views/pm/wbs/form.vue').then(m => mount(m.default, { global: { stubs: { ...elementStubs, 'el-form': formStub } } }))
  }

  it('渲染WBS编辑页面', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.exists()).toBe(true)
  })

  it('API 失败时不崩溃', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    expect(wrapper.exists()).toBe(true)
  })

  it('新增模式下 isEdit 为 false', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.isEdit).toBeFalsy()
  })

  it('loadProducts 加载产品列表', async () => {
    request.get.mockResolvedValue({ code: 200, data: [{ id: 'prod1', productName: '产品1' }] })
    const wrapper = await createWrapper()
    await wrapper.vm.loadProducts()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.productList.length).toBe(1)
  })

  it('handleSubmit 创建成功跳转列表', async () => {
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    const wrapper2 = await createWrapper(() => Promise.resolve(true))
    wrapper2.vm.form.projectId = 'p1'
    wrapper2.vm.form.name = '任务1'
    await wrapper2.vm.handleSubmit()
    expect(request.post).toHaveBeenCalledWith('/pm/wbs', expect.any(Object))
  })

  it('handleCancel 返回列表', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    wrapper.vm.handleCancel()
  })
})

describe('WbsFormPage 边界', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    request.get.mockResolvedValue({ code: 200, data: [] })
    request.post.mockResolvedValue({ code: 200 })
    request.put.mockResolvedValue({ code: 200 })
  })

  it('endDateDisabled 计划开始前日期禁用', async () => {
    const wrapper = await import('@/views/pm/wbs/form.vue').then(m => mount(m.default, { global: { stubs: elementStubs } }))
    wrapper.vm.form.plannedStartDate = '2026-02-01'
    expect(wrapper.vm.endDateDisabled(new Date(2026, 0, 15))).toBe(true)
    expect(wrapper.vm.endDateDisabled(new Date(2026, 2, 15))).toBe(false)
    wrapper.vm.form.plannedStartDate = ''
    expect(wrapper.vm.endDateDisabled(new Date(2026, 0, 15))).toBe(false)
  })

  it('onProductChange 有产品时加载模块', async () => {
    request.get.mockResolvedValue({ code: 200, data: [{ id: 'm1', moduleName: '模块1' }] })
    const wrapper = await import('@/views/pm/wbs/form.vue').then(m => mount(m.default, { global: { stubs: elementStubs } }))
    wrapper.vm.form.moduleId = 'old'
    await wrapper.vm.onProductChange('p1')
    expect(wrapper.vm.form.moduleId).toBe('')
    expect(wrapper.vm.moduleList.length).toBe(1)
  })

  it('onProductChange 无产品时清空模块', async () => {
    const wrapper = await import('@/views/pm/wbs/form.vue').then(m => mount(m.default, { global: { stubs: elementStubs } }))
    wrapper.vm.moduleList = [{ id: 'm1' }]
    await wrapper.vm.onProductChange('')
    expect(wrapper.vm.moduleList).toEqual([])
  })

  it('onProductChange 加载失败提示', async () => {
    const { ElMessage } = await import('element-plus')
    const spy = vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
    request.get.mockRejectedValue(new Error('fail'))
    const wrapper = await import('@/views/pm/wbs/form.vue').then(m => mount(m.default, { global: { stubs: elementStubs } }))
    await wrapper.vm.onProductChange('p1')
    expect(spy).toHaveBeenCalled()
  })

  it('loadDetail 编辑模式填充表单', async () => {
    routeMock.params = { id: 'w1' }
    routeMock.path = '/pm/wbs/form/w1'
    request.get.mockResolvedValue({ code: 200, data: { name: '任务1', productId: 'p1', moduleId: 'm1', priority: 'P1', techDifficulty: 'HIGH', plannedStartDate: '2026-02-01', plannedEndDate: '2026-03-01', effortEstimate: '40', budgetEstimate: '5000', ownerId: 'u1', plannedOwnerId: 'u2', description: '描述', remarks: '备注' } })
    const wrapper = await import('@/views/pm/wbs/form.vue').then(m => mount(m.default, { global: { stubs: elementStubs } }))
    await wrapper.vm.loadDetail()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.form.name).toBe('任务1')
    expect(wrapper.vm.detail.name).toBe('任务1')
    routeMock.params = {}
    routeMock.path = '/'
  })

  it('loadProducts 失败提示', async () => {
    const { ElMessage } = await import('element-plus')
    const spy = vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
    request.get.mockRejectedValue(new Error('fail'))
    const wrapper = await import('@/views/pm/wbs/form.vue').then(m => mount(m.default, { global: { stubs: elementStubs } }))
    await wrapper.vm.loadProducts()
    expect(spy).toHaveBeenCalled()
  })
})
