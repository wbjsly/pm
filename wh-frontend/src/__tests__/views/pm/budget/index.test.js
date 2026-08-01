import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setupPinia, elementStubs } from '../../../helpers'

const { routerMock } = vi.hoisted(() => ({ routerMock: { push: vi.fn(), replace: vi.fn(), go: vi.fn() } }))

vi.mock('vue-router', () => ({
  useRouter: () => routerMock,
  useRoute: () => ({ path: '/', params: {}, query: {} }),
}))

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}))

vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn(), info: vi.fn() },
  ElMessageBox: { confirm: vi.fn(() => Promise.resolve()) },
}))

import request from '@/utils/request'

const mockProjects = [
  { id: '1', projectName: '项目A', pmId: 'pm1', pmName: '张三' },
  { id: '2', projectName: '项目B', pmId: 'pm2', pmName: '李四' },
]

const mockBudgetData = [
  {
    projectId: '1',
    projectName: '项目A',
    projectShortName: 'PA',
    projectStatus: 'APPROVED',
    hasAnyBudget: true,
    latestApprovedVersion: '1.0',
    latestApprovedAmount: 100000,
    latestApprovedTotalBudget: 120000,
    latestApprovedBudgetId: 'b1',
    projectActualCost: 50000,
    projectBudgetRemaining: 70000,
    projectCostRatio: 0.5,
    budgets: [
      {
        id: 'b1', budgetCode: 'BUD-001', version: '1.0',
        totalBudget: 120000, costBaseline: 100000,
        laborAmount: 40000, procurementAmount: 30000,
        otherAmount: 20000, managementReserve: 10000,
        status: 'APPROVED', createDate: '2024-01-15T00:00:00',
        projectId: '1',
      },
      {
        id: 'b2', budgetCode: 'BUD-002', version: '0.1',
        totalBudget: 100000, costBaseline: 80000,
        laborAmount: 30000, procurementAmount: 25000,
        otherAmount: 15000, managementReserve: 10000,
        status: 'DRAFT', createDate: '2024-02-01T00:00:00',
        projectId: '1',
      },
    ],
  },
  {
    projectId: '2',
    projectName: '项目B',
    projectShortName: 'PB',
    projectStatus: 'DRAFT',
    hasAnyBudget: false,
    latestApprovedVersion: null,
    latestApprovedAmount: null,
    latestApprovedTotalBudget: null,
    latestApprovedBudgetId: null,
    projectActualCost: 0,
    projectBudgetRemaining: 0,
    projectCostRatio: 0,
    budgets: [],
  },
]

function createWrapper() {
  return import('@/views/pm/budget/index.vue').then(m =>
    mount(m.default, { global: { stubs: elementStubs } })
  )
}

describe('BudgetPage', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    request.get.mockImplementation((url) => {
      if (url === '/pm/charters') {
        return Promise.resolve({ code: 200, data: { records: mockProjects, total: 2 } })
      }
      if (url === '/pm/budgets/projects') {
        return Promise.resolve({ code: 200, data: { records: mockBudgetData, total: 2 } })
      }
      return Promise.resolve({ code: 200, data: {} })
    })
  })

  it('渲染预算列表页面', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    expect(wrapper.exists()).toBe(true)
  })

  it('挂载时加载项目和预算数据', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    expect(request.get).toHaveBeenCalledWith('/pm/charters', expect.any(Object))
    expect(request.get).toHaveBeenCalledWith('/pm/budgets/projects', expect.any(Object))
  })

  it('查询按钮重置页码并重新加载数据', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    vi.clearAllMocks()

    request.get.mockImplementation((url) => {
      if (url === '/pm/budgets/projects') {
        return Promise.resolve({ code: 200, data: { records: mockBudgetData, total: 2 } })
      }
      if (url === '/pm/charters') {
        return Promise.resolve({ code: 200, data: { records: mockProjects, total: 2 } })
      }
      return Promise.resolve({ code: 200, data: {} })
    })

    const buttons = wrapper.findAll('button')
    const searchBtn = buttons.find(b => b.text().includes('查询'))
    expect(searchBtn).toBeTruthy()

    await searchBtn.trigger('click')
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    expect(request.get).toHaveBeenCalledWith('/pm/budgets/projects', expect.objectContaining({
      params: expect.objectContaining({ pageNum: 1 }),
    }))
  })

  it('重置按钮清除筛选条件并重新加载数据', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    vi.clearAllMocks()

    request.get.mockImplementation((url) => {
      if (url === '/pm/budgets/projects') {
        return Promise.resolve({ code: 200, data: { records: mockBudgetData, total: 2 } })
      }
      if (url === '/pm/charters') {
        return Promise.resolve({ code: 200, data: { records: mockProjects, total: 2 } })
      }
      return Promise.resolve({ code: 200, data: {} })
    })

    const buttons = wrapper.findAll('button')
    const resetBtn = buttons.find(b => b.text().includes('重置'))
    expect(resetBtn).toBeTruthy()

    await resetBtn.trigger('click')
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    expect(request.get).toHaveBeenCalledWith('/pm/budgets/projects', expect.objectContaining({
      params: expect.objectContaining({ projectId: '', pmId: '', status: '' }),
    }))
  })

  it('API 请求失败时显示错误消息', async () => {
    const { ElMessage } = await import('element-plus')
    request.get.mockRejectedValue(new Error('Network error'))

    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    expect(ElMessage.error).toHaveBeenCalled()
  })
})

describe('BudgetPage 补充', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    request.get.mockResolvedValue({ code: 200, data: { records: [], total: 0 } })
  })

  function createWrapper() {
    return import('@/views/pm/budget/index.vue').then(m => mount(m.default, { global: { stubs: elementStubs } }))
  }

  it('formatMoney / formatPercent / formatDate', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.vm.formatMoney('1000')).toBe('1,000.00')
    expect(wrapper.vm.formatMoney(null)).toBe('-')
    expect(wrapper.vm.formatPercent('0.5')).toBe('50.0%')
    expect(wrapper.vm.formatPercent(null)).toBe('-')
    expect(wrapper.vm.formatDate('2026-01-05T00:00:00')).toBe('2026-01-05')
    expect(wrapper.vm.formatDate(null)).toBe('')

  })

  it('handleExpand 展开最后一行', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.handleExpand({ id: 'a' }, [{ id: 'a' }, { id: 'b' }])
    expect(wrapper.vm.expandRowKeys).toEqual(['b'])
    wrapper.vm.handleExpand({ id: 'a' }, [])
    expect(wrapper.vm.expandRowKeys).toEqual([])
  })

  it('handleRowClick 切换展开', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.expandRowKeys = []
    wrapper.vm.handleRowClick({ id: 'a' })
    expect(wrapper.vm.expandRowKeys).toEqual(['a'])
    wrapper.vm.handleRowClick({ id: 'a' })
    expect(wrapper.vm.expandRowKeys).toEqual([])
  })

  it('handleUpgrade / handleProjectUpgrade 跳转升级页', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.handleUpgrade({ id: 'b1' })
    expect(routerMock.push).toHaveBeenCalledWith('/pm/budget/upgrade/b1')
    wrapper.vm.handleProjectUpgrade({ latestApprovedBudgetId: 'b2' })
    expect(routerMock.push).toHaveBeenCalledWith('/pm/budget/upgrade/b2')
  })

  it('hasDraftOrPendingBudget 判断草稿/待审批预算', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.vm.hasDraftOrPendingBudget({ children: [{ status: 'DRAFT' }] })).toBe(true)
    expect(wrapper.vm.hasDraftOrPendingBudget({ children: [{ status: 'APPROVED' }] })).toBe(false)
    expect(wrapper.vm.hasDraftOrPendingBudget({})).toBeUndefined()
  })

  it('loadProjects 加载项目与 PM 列表', async () => {
    request.get.mockResolvedValue({ code: 200, data: { records: [{ id: '1', pmId: 'pm1', pmName: '张三' }], total: 1 } })
    const wrapper = await createWrapper()
    await wrapper.vm.loadProjects()
    expect(wrapper.vm.projects.length).toBe(1)
    expect(wrapper.vm.pmList.length).toBe(1)
  })

  it('loadData 成功加载并转换树形', async () => {
    request.get.mockResolvedValue({
      code: 200,
      data: {
        records: [{ projectId: '1', projectName: '项目A', budgets: [{ id: 'b1', status: 'APPROVED' }], latestApprovedBudgetId: 'b1' }],
        total: 1,
      },
    })
    const wrapper = await createWrapper()
    await wrapper.vm.loadData()
    expect(wrapper.vm.tableData.length).toBe(1)
    expect(wrapper.vm.tableData[0].id).toBe('proj-1')
    expect(wrapper.vm.tableData[0].children.length).toBe(1)
    expect(wrapper.vm.total).toBe(1)
  })

  it('loadData 失败提示错误', async () => {
    const { ElMessage } = await import('element-plus')
    const spy = vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
    request.get.mockRejectedValue(new Error('fail'))
    const wrapper = await createWrapper()
    await wrapper.vm.loadData()
    expect(spy).toHaveBeenCalled()
    expect(wrapper.vm.loading).toBe(false)
  })

  it('handleSearch 重置页码并加载', async () => {
    const wrapper = await createWrapper()
    request.get.mockResolvedValue({ code: 200, data: { records: [], total: 0 } })
    wrapper.vm.queryParams.pageNum = 3
    wrapper.vm.handleSearch()
    expect(wrapper.vm.queryParams.pageNum).toBe(1)
    expect(request.get).toHaveBeenCalled()
  })

  it('handleReset 重置筛选', async () => {
    const wrapper = await createWrapper()
    request.get.mockResolvedValue({ code: 200, data: { records: [], total: 0 } })
    wrapper.vm.queryParams.projectId = 'x'
    wrapper.vm.handleReset()
    expect(wrapper.vm.queryParams.projectId).toBe('')
    expect(request.get).toHaveBeenCalled()
  })
})

describe('BudgetPage 操作函数', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    request.get.mockResolvedValue({ code: 200, data: { records: [], total: 0 } })
    request.delete.mockResolvedValue({ code: 200 })
    request.post.mockResolvedValue({ code: 200 })
  })

  function createWrapper() {
    return import('@/views/pm/budget/index.vue').then(m => mount(m.default, { global: { stubs: elementStubs } }))
  }

  it('handleCreate / handleProjectCreate 跳转表单', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.handleCreate()
    expect(routerMock.push).toHaveBeenCalledWith('/pm/budget/form')
    wrapper.vm.handleProjectCreate({ projectId: 'p1' })
    expect(routerMock.push).toHaveBeenCalledWith('/pm/budget/form?projectId=p1')
  })

  it('handleEdit 版本高于 0.5 跳升级页', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.handleEdit({ id: 'b1', version: '1.0' })
    expect(routerMock.push).toHaveBeenCalledWith('/pm/budget/upgrade/b1')
    wrapper.vm.handleEdit({ id: 'b1', version: '0.5' })
    expect(routerMock.push).toHaveBeenCalledWith('/pm/budget/form/b1')
  })

  it('handleView / handleComparison / handleProjectComparison 跳转', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.handleView({ id: 'b1' })
    expect(routerMock.push).toHaveBeenCalledWith('/pm/budget/detail/b1')
    wrapper.vm.handleComparison({ id: 'b1', projectId: 'p1' })
    expect(routerMock.push).toHaveBeenCalledWith('/pm/budget/comparison/p1?budgetId=b1')
    wrapper.vm.handleProjectComparison({ projectId: 'p1', latestApprovedBudgetId: 'b2' })
    expect(routerMock.push).toHaveBeenCalledWith('/pm/budget/comparison/p1?budgetId=b2')
  })

  it('handleDelete 确认后删除', async () => {
    const { ElMessageBox, ElMessage } = await import('element-plus')
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    const wrapper = await createWrapper()
    wrapper.vm.loadData = vi.fn()
    await wrapper.vm.handleDelete({ id: 'b1' })
    expect(request.delete).toHaveBeenCalledWith('/pm/budgets/b1')
  })

  it('handleSubmit 确认后提交审批', async () => {
    const { ElMessageBox, ElMessage } = await import('element-plus')
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    const wrapper = await createWrapper()
    wrapper.vm.loadData = vi.fn()
    await wrapper.vm.handleSubmit({ id: 'b1' })
    expect(request.post).toHaveBeenCalledWith('/pm/budgets/b1/submit')
  })
})
