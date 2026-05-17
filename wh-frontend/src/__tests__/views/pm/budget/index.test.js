import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setupPinia, elementStubs } from '../../../helpers'

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
