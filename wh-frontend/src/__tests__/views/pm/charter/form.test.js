import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setupPinia, elementStubs } from '../../../helpers'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const { routeMock } = vi.hoisted(() => ({
  routeMock: { path: '/pm/charter/form', params: {}, query: {} },
}))

vi.mock('vue-router', async () => {
  const actual = await vi.importActual('vue-router')
  return { ...actual, useRouter: () => ({ push: vi.fn(), replace: vi.fn(), go: vi.fn() }), useRoute: () => routeMock }
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

describe('CharterFormPage', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    request.get.mockResolvedValue({ code: 200, data: { records: [], total: 0 } })
    request.post.mockResolvedValue({ code: 200 })
  })

  function createWrapper(validateImpl) {
    const formStub = validateImpl === undefined
      ? { template: '<div><slot/></div>' }
      : { template: '<div><slot/></div>', methods: { validate: validateImpl } }
    return import('@/views/pm/charter/form.vue').then(m => mount(m.default, { global: { stubs: { ...elementStubs, 'el-form': formStub } } }))
  }

  it('渲染立项创建页面', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.exists()).toBe(true)
  })

  it('API 加载时组件不崩溃', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    expect(wrapper.exists()).toBe(true)
  })

  it('API 失败时不崩溃', async () => {
    request.get.mockRejectedValue(new Error('fail'))
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    expect(wrapper.exists()).toBe(true)
  })

  it('表单包含输入框', async () => {
    const wrapper = await createWrapper()
    const inputs = wrapper.findAll('input')
    expect(inputs.length).toBeGreaterThan(0)
  })

  it('calcTaxAmount 计算税额', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.form.outputValueTaxable = '1000'
    wrapper.vm.form.taxRate = '13'
    wrapper.vm.calcTaxAmount()
    expect(wrapper.vm.form.taxAmount).toBe('130.00')
    expect(wrapper.vm.form.outputValueExcludingTax).toBe('870.00')
  })

  it('formatBudget 格式化金额', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.vm.formatBudget('1000')).toBe('1,000.00 元人民币')
    expect(wrapper.vm.formatBudget(null)).toBe('')
  })

  it('getUserName / statusTagType / statusLabel / progressLabel', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.userList = [{ id: 'u1', realName: '张三', username: 'admin' }]
    expect(wrapper.vm.getUserName('u1')).toBe('张三 (admin)')
    expect(wrapper.vm.getUserName('x')).toBe('x')
    expect(wrapper.vm.statusTagType('DRAFT')).toBe('info')
    expect(wrapper.vm.statusTagType('APPROVED')).toBe('success')
    expect(wrapper.vm.statusTagType('X')).toBe('info')
    expect(wrapper.vm.statusLabel('DRAFT')).toBe('草稿')
    expect(wrapper.vm.progressLabel('IN_PROGRESS')).toBe('进行中')
  })

  it('endDateDisabled 计划开始前禁用', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.form.startDate = '2026-02-01'
    expect(wrapper.vm.endDateDisabled(new Date(2026, 0, 15))).toBe(true)
    expect(wrapper.vm.endDateDisabled(new Date(2026, 2, 15))).toBe(false)
    wrapper.vm.form.startDate = ''
    expect(wrapper.vm.endDateDisabled(new Date(2026, 0, 15))).toBe(false)
  })

  it('isContractProject 判断合同项目', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.form.projectCategory = 'CONTRACT'
    expect(wrapper.vm.isContractProject).toBe(true)
    wrapper.vm.form.projectCategory = 'R_D'
    expect(wrapper.vm.isContractProject).toBe(false)
  })

  it('loadUserList 加载用户', async () => {
    request.get.mockResolvedValue({ code: 200, data: { records: [{ id: 'u1', realName: '张三' }], total: 1 } })
    const wrapper = await createWrapper()
    await wrapper.vm.loadUserList()
    expect(wrapper.vm.userList.length).toBe(1)
  })

  it('handleSubmit 创建成功提交', async () => {
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    wrapper.vm.form.projectName = '新项目'
    await wrapper.vm.handleSubmit()
    expect(request.post).toHaveBeenCalledWith('/pm/charters', expect.any(Object))
    expect(wrapper.vm.submitLoading).toBe(false)
  })

  it('handleSubmit 失败提示', async () => {
    vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
    request.post.mockRejectedValue(new Error('fail'))
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleSubmit()
    expect(ElMessage.error).toHaveBeenCalled()
  })

  it('nextStep 校验失败不前进', async () => {
    const wrapper = await createWrapper(() => Promise.reject())
    await wrapper.vm.$nextTick()
    await wrapper.vm.nextStep()
    expect(wrapper.vm.currentStep).toBe(0)
  })

  it('resetForm 重置表单', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.form.projectName = 'x'
    wrapper.vm.resetForm()
    expect(wrapper.vm.form.projectName).toBe('')
  })

  it('initForm 新增模式初始化', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.initForm()
    expect(wrapper.vm.currentStep).toBe(0)
  })
})

describe('CharterFormPage 步骤与校验', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    request.get.mockResolvedValue({ code: 200, data: { records: [], total: 0 } })
    request.post.mockResolvedValue({ code: 200 })
  })

  function createWrapper(validateImpl) {
    const formStub = validateImpl === undefined
      ? { template: '<div><slot/></div>' }
      : { template: '<div><slot/></div>', methods: { validate: validateImpl } }
    return import('@/views/pm/charter/form.vue').then(m => mount(m.default, { global: { stubs: { ...elementStubs, 'el-form': formStub } } }))
  }

  it('nextStep 校验通过前进', async () => {
    const wrapper = await createWrapper(() => Promise.resolve(true))
    await wrapper.vm.$nextTick()
    await wrapper.vm.nextStep()
    expect(wrapper.vm.currentStep).toBe(1)
  })

  it('endDate 校验器验证结束日期早于开始', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    const validator = wrapper.vm.rules.endDate[0].validator
    wrapper.vm.form.startDate = '2026-02-01'
    const cbError = vi.fn()
    const cbOk = vi.fn()
    validator(null, '2026-01-01', cbError)
    expect(cbError).toHaveBeenCalled()
    validator(null, '2026-03-01', cbOk)
    expect(cbOk).toHaveBeenCalled()
  })

  it('objectivesList 过滤空目标序列化', async () => {
    const wrapper = await createWrapper(() => Promise.resolve(true))
    await wrapper.vm.$nextTick()
    wrapper.vm.objectivesList = [{ objective: '目标1', metric: 'M', target: 'T' }, { objective: '', metric: '', target: '' }]
    wrapper.vm.stakeholdersList = [{ name: '张三', role: 'PM', org: '公司' }, { name: '', role: '', org: '' }]
    request.post.mockResolvedValue({ code: 200 })
    await wrapper.vm.handleSubmit()
    expect(request.post).toHaveBeenCalled()
    const sent = request.post.mock.calls[0][1]
    expect(sent.objectives).toContain('目标1')
    expect(sent.keyStakeholders).toContain('张三')
  })
})
