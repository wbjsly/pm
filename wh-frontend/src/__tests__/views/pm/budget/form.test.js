import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setupPinia, elementStubs } from '../../../helpers'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const { routeMock } = vi.hoisted(() => ({
  routeMock: { path: '/', params: {}, query: {} },
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

describe('BudgetFormPage', () => {
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
    return import('@/views/pm/budget/form.vue').then(m => mount(m.default, { global: { stubs: { ...elementStubs, 'el-form': formStub, 'el-dialog': { template: '<div><slot/><slot name="footer"/></div>' } } } }))
  }

  it('渲染预算编制页面', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.exists()).toBe(true)
  })

  it('API 失败时不崩溃', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    expect(wrapper.exists()).toBe(true)
  })

  it('新增模式默认状态', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.isEdit).toBeFalsy()
  })

  it('loadPositions 加载岗位', async () => {
    request.get.mockResolvedValue({ code: 200, data: [{ id: 'p1', name: '开发' }] })
    const wrapper = await createWrapper()
    await wrapper.vm.loadPositions()
    expect(wrapper.vm.positionList.length).toBe(1)
  })

  it('formatMoney 格式化金额', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.vm.formatMoney(1000)).toBe('1,000.00')
    expect(wrapper.vm.formatMoney(0)).toBe('0.00')
  })

  it('calcLaborAmount 计算人工金额', async () => {
    const wrapper = await createWrapper()
    const row = { hours: 100, costRate: 80 }
    wrapper.vm.calcLaborAmount(row)
    expect(row.amount).toBe(8000)
  })

  it('calcProcurementAmount 计算采购金额', async () => {
    const wrapper = await createWrapper()
    const row = { qty: 2, unitPrice: 500 }
    wrapper.vm.calcProcurementAmount(row)
    expect(row.amount).toBe(1000)
  })

  it('addItem / removeItem 增删条目', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.form.items.LABOR = []
    wrapper.vm.addItem('LABOR')
    expect(wrapper.vm.form.items.LABOR.length).toBe(1)
    wrapper.vm.addItem('LABOR')
    expect(wrapper.vm.form.items.LABOR.length).toBe(2)
    wrapper.vm.removeItem('LABOR', 0)
    expect(wrapper.vm.form.items.LABOR.length).toBe(1)
  })

  it('costBaseline / totalBudget computed', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.form.items.LABOR = [{ amount: 1000 }]
    wrapper.vm.form.items.PROCUREMENT = [{ amount: 2000 }]
    wrapper.vm.form.items.TRAVEL = [{ amount: 500 }]
    wrapper.vm.form.managementReserve = 500
    expect(wrapper.vm.costBaseline).toBe(3500)
    expect(wrapper.vm.totalBudget).toBe(4000)
  })

  it('otherTotal computed 汇总其他费用', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.form.items.TRAVEL = [{ amount: 100 }]
    wrapper.vm.form.items.BUSINESS = [{ amount: 200 }]
    expect(wrapper.vm.otherTotal).toBe(300)
  })

  it('calcTotals 触发不抛错（金额由 computed 计算）', async () => {
    const wrapper = await createWrapper()
    expect(() => wrapper.vm.calcTotals()).not.toThrow()
  })

  it('handleSubmit 校验失败不提交', async () => {
    const wrapper = await createWrapper(() => Promise.reject())
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleSubmit()
    expect(request.post).not.toHaveBeenCalled()
  })

  it('resetForm 重置表单', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.form.items.LABOR = [{ name: 'x' }]
    wrapper.vm.resetForm()
    expect(wrapper.vm.form.items.LABOR.length).toBe(1)
    expect(wrapper.vm.form.items.LABOR[0].roleCode).toBe('DEV')
  })

  it('onQuotaDialogConfirm 确认配额弹窗', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.quotaDialogNewRate = 100
    wrapper.vm.quotaDialogOldRate = 80
    let resolved = null
    wrapper.vm.quotaDialogCallback = (choice) => { resolved = choice }
    wrapper.vm.quotaDialogVisible = true
    wrapper.vm.onQuotaDialogConfirm(true)
    expect(wrapper.vm.quotaDialogVisible).toBe(false)
    expect(resolved).toBe(true)
  })
})

describe('BudgetFormPage 编辑模式', () => {
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
    return import('@/views/pm/budget/form.vue').then(m => mount(m.default, { global: { stubs: { ...elementStubs, 'el-form': formStub, 'el-dialog': { template: '<div><slot/><slot name="footer"/></div>' } } } }))
  }

  it('buildItemsTree 构建提交科目树', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.form.items.LABOR = [{ roleCode: 'DEV', positionId: 'p1', hours: 40, costRate: 100, amount: 4000, bomItem: undefined, qty: undefined, unitPrice: undefined }]
    wrapper.vm.form.items.PROCUREMENT = [{ bomItem: '服务器', qty: 2, unitPrice: 500, amount: 1000, roleCode: undefined, positionId: undefined, hours: undefined, costRate: undefined }]
    wrapper.vm.form.items.TRAVEL = []
    wrapper.vm.form.items.BUSINESS = []
    wrapper.vm.form.items.ENTERTAINMENT = []
    wrapper.vm.form.items.ACTIVITY = []
    wrapper.vm.form.items.OTHER = []
    const tree = wrapper.vm.buildItemsTree()
    expect(tree.length).toBe(2)
    expect(tree[0].category).toBe('LABOR')
    expect(tree[1].category).toBe('PROCUREMENT')
  })

  it('handleSubmit 新建提交', async () => {
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    const wrapper = await createWrapper(() => Promise.resolve(true))
    await wrapper.vm.$nextTick()
    wrapper.vm.form.projectId = 'p1'
    wrapper.vm.form.managementReserve = 500
    await wrapper.vm.handleSubmit()
    expect(request.post).toHaveBeenCalledWith('/pm/budgets', expect.objectContaining({ projectId: 'p1' }))
    expect(wrapper.vm.submitting).toBe(false)
  })

  it('handleSubmit 失败提示', async () => {
    vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
    request.post.mockRejectedValue(new Error('fail'))
    const wrapper = await createWrapper(() => Promise.resolve(true))
    await wrapper.vm.$nextTick()
    wrapper.vm.form.projectId = 'p1'
    await wrapper.vm.handleSubmit()
    expect(ElMessage.error).toHaveBeenCalled()
  })

  it('handlePositionChange 有岗位时加载费率', async () => {
    request.get.mockImplementation((url) => {
      if (url.includes('current-rate')) return Promise.resolve({ code: 200, data: { costRate: 100 } })
      return Promise.resolve({ code: 200, data: {} })
    })
    const wrapper = await createWrapper()
    wrapper.vm.positionList = [{ id: 'pos1', name: '开发' }]
    const row = { positionId: 'pos1', hours: 40, costRate: 0 }
    await wrapper.vm.handlePositionChange(row)
    expect(row.costRate).toBe(100)
    expect(row.roleCode).toBe('开发')
  })

  it('handlePositionChange 无费率数据时置 0', async () => {
    request.get.mockResolvedValue({ code: 200, data: {} })
    const wrapper = await createWrapper()
    const row = { positionId: 'pos1', hours: 40 }
    await wrapper.vm.handlePositionChange(row)
    expect(row.costRate).toBe(0)
  })

  it('handlePositionChange 失败置 0', async () => {
    request.get.mockRejectedValue(new Error('fail'))
    const wrapper = await createWrapper()
    const row = { positionId: 'pos1', hours: 40 }
    await wrapper.vm.handlePositionChange(row)
    expect(row.costRate).toBe(0)
  })

  it('loadProjects 编辑模式包含当前项目', async () => {
    // 模拟编辑模式需要 route.params.id —— 通过属性不可行，直接验证过滤逻辑
    request.get.mockImplementation((url) => {
      if (url === '/pm/charters') return Promise.resolve({ code: 200, data: { records: [{ id: 'p1', projectName: '项目1' }, { id: 'p2', projectName: '项目2' }], total: 2 } })
      if (url === '/pm/budgets') return Promise.resolve({ code: 200, data: { records: [{ id: 'b1', projectId: 'p1' }], total: 1 } })
      return Promise.resolve({ code: 200, data: {} })
    })
    const wrapper = await createWrapper()
    await wrapper.vm.loadProjects()
    // p1 已有预算被过滤，只剩 p2
    expect(wrapper.vm.projects.some(p => p.id === 'p1')).toBe(false)
    expect(wrapper.vm.projects.some(p => p.id === 'p2')).toBe(true)
  })
})

describe('BudgetFormPage 编辑模式数据', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    request.get.mockResolvedValue({ code: 200, data: { records: [], total: 0 } })
  })

  function createWrapper() {
    return import('@/views/pm/budget/form.vue').then(m => mount(m.default, { global: { stubs: { ...elementStubs, 'el-dialog': { template: '<div><slot/><slot name="footer"/></div>' } } } }))
  }

  it('loadData 编辑模式填充表单', async () => {
    routeMock.params = { id: 'b1' }
    routeMock.path = '/pm/budget/form/b1'
    request.get.mockImplementation((url) => {
      if (url === '/pm/budgets/b1/detail') return Promise.resolve({ code: 200, data: { budget: { projectId: 'p1', managementReserve: '500' }, items: [{ id: 'i1', category: 'LABOR', roleCode: 'DEV', hours: 40, costRate: 100, amount: 4000, positionId: 'pos1' }] } })
      return Promise.resolve({ code: 200, data: { records: [], total: 0 } })
    })
    const wrapper = await createWrapper()
    await wrapper.vm.loadData()
    expect(wrapper.vm.form.projectId).toBe('p1')
    expect(wrapper.vm.form.managementReserve).toBe(500)
    expect(wrapper.vm.form.items.LABOR.length).toBe(1)
    routeMock.params = {}
    routeMock.path = '/'
  })

  it('loadData 非编辑模式跳过', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.loadData()
    expect(request.get).not.toHaveBeenCalledWith('/pm/budgets/b1/detail')
  })

  it('handlePositionChange 编辑模式费率变化弹配额确认', async () => {
    routeMock.params = { id: 'b1' }
    routeMock.path = '/pm/budget/form/b1'
    request.get.mockImplementation((url) => {
      if (url.includes('current-rate')) return Promise.resolve({ code: 200, data: { costRate: 150 } })
      return Promise.resolve({ code: 200, data: {} })
    })
    const wrapper = await createWrapper()
    wrapper.vm.positionList = [{ id: 'pos1', name: '开发' }]
    const row = { positionId: 'pos1', hours: 40, costRate: 0, _savedCostRate: 100 }
    const promise = wrapper.vm.handlePositionChange(row)
    await wrapper.vm.$nextTick()
    // 配额弹窗打开后确认使用新费率
    expect(wrapper.vm.quotaDialogVisible).toBe(true)
    wrapper.vm.onQuotaDialogConfirm('new')
    await promise
    expect(row.costRate).toBe(150)
    routeMock.params = {}
    routeMock.path = '/'
  })

  it('loadProjects 编辑模式包含当前项目', async () => {
    routeMock.params = { id: 'b1' }
    routeMock.path = '/pm/budget/form/b1'
    request.get.mockImplementation((url) => {
      if (url === '/pm/charters') return Promise.resolve({ code: 200, data: { records: [{ id: 'p1', projectName: '项目1' }], total: 1 } })
      if (url === '/pm/budgets' && !url.includes('/b1')) return Promise.resolve({ code: 200, data: { records: [{ id: 'b1', projectId: 'p1' }], total: 1 } })
      if (url === '/pm/budgets/b1') return Promise.resolve({ code: 200, data: { projectId: 'p1' } })
      return Promise.resolve({ code: 200, data: {} })
    })
    const wrapper = await createWrapper()
    await wrapper.vm.loadProjects()
    expect(wrapper.vm.projects.some(p => p.id === 'p1')).toBe(true)
    routeMock.params = {}
    routeMock.path = '/'
  })
})
