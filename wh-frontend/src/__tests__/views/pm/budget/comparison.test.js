import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setupPinia, elementStubs } from '../../../helpers'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const { routeMock } = vi.hoisted(() => ({
  routeMock: { path: '/pm/budget/comparison', params: { projectId: 'p1' }, query: { budgetId: 'b1' } },
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), go: vi.fn() }),
  useRoute: () => routeMock,
}))

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ code: 200, data: {} })),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}))

describe('BudgetComparisonPage', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
  })

  function createWrapper() {
    return import('@/views/pm/budget/comparison.vue').then(m => mount(m.default, { global: { stubs: elementStubs } }))
  }

  it('渲染预实对比页面', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.exists()).toBe(true)
  })

  it('formatAmount 格式化金额', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.vm.formatAmount('1000')).toBe('1,000.00')
    expect(wrapper.vm.formatAmount('0')).toBe('0.00')
    expect(wrapper.vm.formatAmount(null)).toBe('0.00')
    expect(wrapper.vm.formatAmount('1234.5')).toBe('1,234.50')
  })

  it('ratioColor 返回颜色', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.vm.ratioColor(null)).toBe('#67c23a')
    expect(wrapper.vm.ratioColor(0.5)).toBe('#67c23a')
    expect(wrapper.vm.ratioColor(0.9)).toBe('#e6a23c')
    expect(wrapper.vm.ratioColor(1.1)).toBe('#f56c6c')
  })

  it('ratioColumnColor 返回颜色', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.vm.ratioColumnColor(null)).toBe('#67c23a')
    expect(wrapper.vm.ratioColumnColor(1.2)).toBe('#f56c6c')
    expect(wrapper.vm.ratioColumnColor(0.95)).toBe('#e6a23c')
    expect(wrapper.vm.ratioColumnColor(0.5)).toBe('#67c23a')
  })

  it('flatItems 扁平化科目树', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.data = {
      items: [
        { id: 'i1', category: 'LABOR', level: 1, budgetAmount: 100, actualAmount: 50, ratio: 0.5,
          children: [{ id: 'i1c', category: 'LABOR', level: 2, roleCode: 'DEV', budgetAmount: 60, actualAmount: 30, ratio: 0.5 }] },
        { id: 'i2', category: 'TRAVEL', level: 1, budgetAmount: 0, actualAmount: 0, ratio: 0 },
      ],
    }
    const flat = wrapper.vm.flatItems
    expect(flat.length).toBe(3)
    expect(flat[1].name).toContain('DEV')
    expect(flat[0].remaining).toBe(50)
  })

  it('hasDetail 检测二级科目', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.data = { items: [{ id: 'i1', category: 'LABOR', level: 1, children: [{ id: 'c1', category: 'LABOR', level: 2 }] }] }
    expect(wrapper.vm.hasDetail).toBe(true)
    wrapper.vm.data = { items: [{ id: 'i1', category: 'LABOR', level: 1 }] }
    expect(wrapper.vm.hasDetail).toBe(false)
  })

  it('showDetail 提示查看科目', async () => {
    const spy = vi.spyOn(ElMessage, 'info').mockImplementation(() => {})
    const wrapper = await createWrapper()
    wrapper.vm.showDetail({ name: '人工' })
    expect(spy).toHaveBeenCalled()
  })

  it('loadData 加载对比数据', async () => {
    request.get.mockResolvedValue({ code: 200, data: { projectName: '项目1', items: [] } })
    const wrapper = await createWrapper()
    await wrapper.vm.loadData()
    expect(wrapper.vm.data.projectName).toBe('项目1')
    expect(wrapper.vm.projectName).toBe('项目1')
    expect(wrapper.vm.loading).toBe(false)
  })

  it('loadData 缺 budgetId 提示', async () => {
    vi.spyOn(ElMessage, 'warning').mockImplementation(() => {})
    routeMock.query = {}
    const wrapper = await createWrapper()
    await wrapper.vm.loadData()
    expect(ElMessage.warning).toHaveBeenCalled()
    routeMock.query = { budgetId: 'b1' }
  })

  it('loadData 失败提示', async () => {
    vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
    request.get.mockRejectedValue(new Error('fail'))
    const wrapper = await createWrapper()
    await wrapper.vm.loadData()
    expect(ElMessage.error).toHaveBeenCalled()
    expect(wrapper.vm.loading).toBe(false)
  })

  it('loadVersions 加载版本', async () => {
    request.get.mockResolvedValue({ code: 200, data: [{ id: 'v1', version: 'v1.0' }] })
    const wrapper = await createWrapper()
    await wrapper.vm.loadVersions()
    expect(wrapper.vm.versions.length).toBe(1)
  })

  it('loadVersions 失败静默', async () => {
    request.get.mockRejectedValue(new Error('fail'))
    const wrapper = await createWrapper()
    await wrapper.vm.loadVersions()
    expect(wrapper.vm.versions).toEqual([])
  })
})
