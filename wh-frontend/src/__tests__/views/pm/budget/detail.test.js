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

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), go: vi.fn(), back: vi.fn() }),
  useRoute: () => ({ path: '/pm/budget/detail/test', params: { id: 'test' }, query: {} }),
}))

import request from '@/utils/request'

const mockProjects = [
  { id: 'proj1', projectName: '测试项目A' },
  { id: 'proj2', projectName: '测试项目B' },
]

const mockBudgetDetail = {
  budget: {
    id: 'b1',
    budgetCode: 'BUD-001',
    version: '1.0',
    status: 'APPROVED',
    projectId: 'proj1',
    totalBudget: 500000,
    costBaseline: 450000,
    managementReserve: 50000,
  },
  items: [
    { id: 'i1', category: 'LABOR', positionName: '高级开发', roleCode: 'DEV', hours: 160, costRate: 200, budgetAmount: 32000 },
    { id: 'i2', category: 'LABOR', positionName: '测试工程师', roleCode: 'QA', hours: 80, costRate: 150, budgetAmount: 12000 },
    { id: 'i3', category: 'PROCUREMENT', bomItem: '服务器', qty: 2, unitPrice: 50000, budgetAmount: 100000 },
    { id: 'i4', category: 'PROCUREMENT', bomItem: '软件许可', qty: 5, unitPrice: 10000, budgetAmount: 50000 },
    { id: 'i5', category: 'TRAVEL', budgetAmount: 15000 },
    { id: 'i6', category: 'BUSINESS', budgetAmount: 8000 },
    { id: 'i7', category: 'ENTERTAINMENT', budgetAmount: 5000 },
    { id: 'i8', category: 'ACTIVITY', budgetAmount: 12000 },
    { id: 'i9', category: 'OTHER', budgetAmount: 3000 },
  ],
}

function createWrapper() {
  // 重写 el-tag stub 以渲染默认插槽内容（el-tag 在 elementStubs 中为 true，不渲染插槽）
  const detailStubs = {
    ...elementStubs,
    'el-tag': { template: '<span class="el-tag"><slot/></span>' },
  }
  return import('@/views/pm/budget/detail.vue').then(m =>
    mount(m.default, { global: { stubs: detailStubs } })
  )
}

describe('BudgetDetailPage', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    request.get.mockImplementation((url) => {
      if (url === '/pm/charters') {
        return Promise.resolve({ code: 200, data: { records: mockProjects, total: 2 } })
      }
      if (url === '/pm/budgets/test/detail') {
        return Promise.resolve({ code: 200, data: mockBudgetDetail })
      }
      return Promise.resolve({ code: 200, data: {} })
    })
  })

  it('渲染预算详情页面', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    expect(wrapper.exists()).toBe(true)
  })

  it('挂载时加载项目列表和预算详情', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    expect(request.get).toHaveBeenCalledWith('/pm/charters', expect.any(Object))
    expect(request.get).toHaveBeenCalledWith('/pm/budgets/test/detail')
  })

  it('显示项目名称', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('测试项目A')
  })

  it('显示预算编码', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('BUD-001')
  })

  it('显示版本号', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('1.0')
  })

  it('显示状态', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('APPROVED')
  })

  it('显示人工合计金额（卡片头部，非表格插槽）', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('44,000.00')
  })

  it('显示采购合计金额（卡片头部）', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('150,000.00')
  })

  it('显示其他费用科目标签', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('差旅')
    expect(wrapper.text()).toContain('商务费用')
    expect(wrapper.text()).toContain('客户招待费')
    expect(wrapper.text()).toContain('活动费')
    expect(wrapper.text()).toContain('其他')
  })

  it('显示预算汇总数据', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('总预算')
    expect(wrapper.text()).toContain('项目直接预算')
    expect(wrapper.text()).toContain('项目管理预算')
  })

  it('显示汇总计算金额', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    // labor(44000) + proc(150000) + other(43000) = 237000
    expect(wrapper.text()).toContain('237,000.00')
    // 237000 + managementReserve(50000) = 287000
    expect(wrapper.text()).toContain('287,000.00')
    // managementReserve
    expect(wrapper.text()).toContain('50,000.00')
  })

  it('显示返回按钮', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    const buttons = wrapper.findAll('button')
    const backBtn = buttons.find(b => b.text().includes('返回'))
    expect(backBtn).toBeTruthy()
  })

  it('API 失败时组件不崩溃', async () => {
    request.get.mockRejectedValue(new Error('Network error'))

    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    expect(wrapper.exists()).toBe(true)
  })
})
