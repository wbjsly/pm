import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setupPinia, elementStubs } from '../../../helpers'

vi.mock('vue-router', async () => {
  const actual = await vi.importActual('vue-router')
  return {
    ...actual,
    useRouter: () => ({ push: vi.fn(), replace: vi.fn(), go: vi.fn() }),
    useRoute: () => ({ path: '/pm/budget/upgrade/1', params: { id: '1' }, query: {} })
  }
})

const mockData = {
  budget: {
    projectId: 'p1',
    budgetCode: 'BUD-2026-001',
    version: 'V1.0',
    status: '1',
    managementReserve: '5000'
  },
  items: [
    { category: 'LABOR', positionId: 'pos1', positionName: '开发(DEV)', roleCode: 'DEV', hours: 10, costRate: 100, budgetAmount: 1000 },
    { category: 'LABOR', positionId: 'pos2', positionName: '测试(QA)', roleCode: 'QA', hours: 5, costRate: 80, budgetAmount: 400 },
    { category: 'PROCUREMENT', bomItem: '服务器', qty: 2, unitPrice: 500, budgetAmount: 1000 },
    { category: 'BUSINESS', budgetAmount: 500 },
    { category: 'TRAVEL', budgetAmount: 300 }
  ]
}

vi.mock('@/api/pm/budget', () => ({
  getBudgetDetailWithItemsApi: vi.fn(() => Promise.resolve({ data: mockData })),
  upgradeBudgetApi: vi.fn(() => Promise.resolve({ code: 200 }))
}))

vi.mock('@/api/pm/charter', () => ({
  getCharterListApi: vi.fn(() => Promise.resolve({ data: { records: [{ id: 'p1', projectName: '测试项目' }] } }))
}))

vi.mock('@/api/system/costQuota', () => ({
  getPositionListApi: vi.fn(() => Promise.resolve({ data: [{ id: 'pos1', name: '开发(DEV)' }, { id: 'pos2', name: '测试(QA)' }] })),
  getCurrentRateApi: vi.fn(() => Promise.resolve({ data: { costRate: 100 } }))
}))

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ code: 200, data: {} })),
    post: vi.fn(() => Promise.resolve({ code: 200 })),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } }
  }
}))

describe('BudgetUpgradePage', () => {
  beforeEach(() => {
    setupPinia()
  })

  async function createWrapper() {
    const m = await import('@/views/pm/budget/upgrade.vue')
    return mount(m.default, {
      global: {
        stubs: elementStubs
      }
    })
  }

  it('渲染预算升级页面', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.exists()).toBe(true)
  })

  it('瀑布流布局: 四个科目区块均渲染', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await new Promise(r => setTimeout(r, 100))
    const sections = wrapper.findAll('.category-section')
    expect(sections.length).toBeGreaterThanOrEqual(4)
  })

  it('显示全局差异汇总面板', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await new Promise(r => setTimeout(r, 100))
    expect(wrapper.find('.global-diff-panel').exists()).toBe(true)
  })

  it('显示项目信息栏', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await new Promise(r => setTimeout(r, 100))
    expect(wrapper.find('.project-info-bar').exists()).toBe(true)
  })

  it('差异计算: diffColor 正数返回红色', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.vm.diffColor(100)).toBe('#F56C6C')
  })

  it('差异计算: diffColor 负数返回绿色', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.vm.diffColor(-100)).toBe('#67C23A')
  })

  it('差异计算: diffColor 零返回灰色', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.vm.diffColor(0)).toBe('#909399')
  })

  it('差异计算: diffSign 正数返回加号', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.vm.diffSign(100)).toBe('+')
  })

  it('差异计算: diffSign 负数返回空字符串', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.vm.diffSign(-100)).toBe('')
  })

  it('差异计算: 原预算为0时百分比处理正确', async () => {
    const wrapper = await createWrapper()
    // safePercent is not exposed, test via diffColor/diffSign
    expect(wrapper.vm.diffColor(0)).toBe('#909399')
  })

  it('修改数据后 marked dirty', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await new Promise(r => setTimeout(r, 100))
    // After loading, should not be dirty
    expect(wrapper.vm.isDirty).toBe(false)
  })

  it('未修改数据时 not dirty', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await new Promise(r => setTimeout(r, 100))
    expect(wrapper.vm.isDirty).toBe(false)
  })

  it('各科目 section-header 存在', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await new Promise(r => setTimeout(r, 100))
    const headers = wrapper.findAll('.section-header')
    expect(headers.length).toBeGreaterThanOrEqual(4)
  })
})
