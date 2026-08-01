import { describe, it, expect, vi, beforeEach } from 'vitest'
import { nextTick } from 'vue'
import { setupPinia } from '../../../../helpers'

const { routeLeaveCallback } = vi.hoisted(() => ({ routeLeaveCallback: { cb: null } }))

vi.mock('vue-router', async () => {
  const actual = await vi.importActual('vue-router')
  return {
    ...actual,
    useRouter: () => ({ push: vi.fn(), replace: vi.fn(), go: vi.fn() }),
    useRoute: () => ({ path: '/pm/budget/upgrade/1', params: { id: '1' }, query: {} }),
    onBeforeRouteLeave: (cb) => { routeLeaveCallback.cb = cb },
  }
})

const mockData = {
  budget: { projectId: 'p1', budgetCode: 'BUD-1', version: 'V1.0', managementReserve: '5000' },
  items: [
    { id: 'l1', category: 'LABOR', roleCode: 'DEV', positionId: 'pos1', hours: 10, costRate: 100, budgetAmount: 1000, actualAmount: 800 },
    { id: 'p1i', category: 'PROCUREMENT', bomItem: '服务器', qty: 2, unitPrice: 500, budgetAmount: 1000, actualAmount: 600 },
    { id: 't1', category: 'TRAVEL', budgetAmount: 300, actualAmount: 100 },
    { id: 'b1', category: 'BUSINESS', budgetAmount: 500, actualAmount: 0 },
  ],
}

vi.mock('@/api/pm/budget', () => ({
  getBudgetDetailWithItemsApi: vi.fn(() => Promise.resolve({ code: 200, data: mockData })),
  upgradeBudgetApi: vi.fn(() => Promise.resolve({ code: 200 })),
}))

vi.mock('@/api/pm/charter', () => ({
  getCharterListApi: vi.fn(() => Promise.resolve({ code: 200, data: { records: [{ id: 'p1', projectName: '测试项目' }] } })),
}))

vi.mock('@/api/system/costQuota', () => ({
  getPositionListApi: vi.fn(() => Promise.resolve({ code: 200, data: [{ id: 'pos1', name: '开发(DEV)' }] })),
  getCurrentRateApi: vi.fn(() => Promise.resolve({ code: 200, data: { costRate: 100 } })),
}))

import { useBudgetUpgrade } from '@/views/pm/budget/upgrade/useBudgetUpgrade'
import { getBudgetDetailWithItemsApi, upgradeBudgetApi } from '@/api/pm/budget'

describe('useBudgetUpgrade', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    getBudgetDetailWithItemsApi.mockResolvedValue({ code: 200, data: mockData })
    upgradeBudgetApi.mockResolvedValue({ code: 200 })
  })

  async function setup() {
    const u = useBudgetUpgrade()
    await u.loadOriginalData()
    await nextTick()
    return u
  }

  it('loadOriginalData 加载预算与调整项', async () => {
    const u = await setup()
    expect(u.originalData.value.budget.projectId).toBe('p1')
    expect(u.originalVersion.value).toBe('V1.0')
    expect(u.adjustedItems.LABOR.length).toBe(1)
    expect(u.adjustedItems.PROCUREMENT.length).toBe(1)
  })

  it('leftTotalBudget / costBaseline / totalBudget computed', async () => {
    const u = await setup()
    expect(u.leftCostBaseline.value).toBe(2800)
    expect(u.leftTotalBudget.value).toBe(7800)
    expect(u.costBaseline.value).toBe(2800)
    expect(u.totalBudget.value).toBe(2800 + (u.form.value.managementReserve || 0))
  })

  it('projectName 解析项目名', async () => {
    const u = await setup()
    await u.loadProjects()
    await nextTick()
    expect(u.projects.value.length).toBe(1)
    expect(u.projectName.value).toBe('测试项目')
  })

  it('addItem 添加各类别条目', async () => {
    const u = await setup()
    u.addItem('LABOR')
    expect(u.adjustedItems.LABOR.length).toBe(2)
    u.addItem('PROCUREMENT')
    expect(u.adjustedItems.PROCUREMENT.length).toBe(2)
    u.addItem('TRAVEL')
    expect(u.adjustedItems.TRAVEL.length).toBe(2)
  })

  it('removeFromAdjust 删除条目', async () => {
    const u = await setup()
    const before = u.adjustedItems.LABOR.length
    u.removeFromAdjust('LABOR', 0)
    expect(u.adjustedItems.LABOR.length).toBe(before - 1)
  })

  it('calcLaborAmount / calcProcurementAmount 计算金额', async () => {
    const u = await setup()
    const labor = { hours: 20, costRate: 50 }
    u.calcLaborAmount(labor)
    expect(labor.amount).toBe(1000)
    const proc = { qty: 3, unitPrice: 200 }
    u.calcProcurementAmount(proc)
    expect(proc.amount).toBe(600)
  })

  it('mergedCategoryTotal 汇总调整金额', async () => {
    const u = await setup()
    const total = u.mergedCategoryTotal('LABOR')
    expect(total).toBe(1000)
  })

  it('diffData 计算差异', async () => {
    const u = await setup()
    const d = u.diffData.value
    expect(d.labor.diff).toBe(0)
    expect(d.total.percent).toBeGreaterThanOrEqual(0)
  })

  it('waterfallRows 瀑布图数据', async () => {
    const u = await setup()
    expect(u.waterfallRows.value.length).toBeGreaterThan(0)
  })

  it('修改管理储备后 isDirty 为 true', async () => {
    const u = await setup()
    u.form.value.managementReserve = 999
    await nextTick()
    await nextTick()
    expect(u.isDirty.value).toBe(true)
  })

  it('populateAllAdjustItems 填充全部调整项', async () => {
    const u = await setup()
    u.adjustedItems.LABOR = []
    u.populateAllAdjustItems()
    expect(u.adjustedItems.LABOR.length).toBeGreaterThan(0)
  })

  it('buildItemsTree 构建提交树', async () => {
    const u = await setup()
    const tree = u.buildItemsTree()
    expect(tree.length).toBeGreaterThan(0)
  })

  it('handleUpgrade 提交成功', async () => {
    const u = await setup()
    u.form.value.projectId = 'p1'
    const formEl = { validate: () => Promise.resolve(true) }
    await u.handleUpgrade(formEl)
    expect(upgradeBudgetApi).toHaveBeenCalled()
    expect(u.submitting.value).toBe(false)
  })

  it('handleUpgrade 校验失败不提交', async () => {
    const u = await setup()
    const formEl = { validate: () => Promise.reject('invalid') }
    await u.handleUpgrade(formEl)
    expect(upgradeBudgetApi).not.toHaveBeenCalled()
  })

  it('handlePositionChange 无岗位时跳过', async () => {
    const u = await setup()
    const row = { positionId: '', hours: 10 }
    await u.handlePositionChange(row)
    expect(row.costRate).toBe(0)
    expect(row.roleCode).toBe('')
  })
})

describe('useBudgetUpgrade 边界', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    getBudgetDetailWithItemsApi.mockResolvedValue({ code: 200, data: mockData })
    upgradeBudgetApi.mockResolvedValue({ code: 200 })
    routeLeaveCallback.cb = null
  })

  async function setup() {
    const u = useBudgetUpgrade()
    await u.loadOriginalData()
    await nextTick()
    return u
  }

  it('getActualAmount 按 originalId 取实际成本', async () => {
    const u = await setup()
    const amount = u.getActualAmount('l1')
    expect(amount).toBe(800)
    expect(u.getActualAmount('nonexistent')).toBe(0)
  })

  it('findItemsBelowActual 检测低于实际成本的调整项', async () => {
    const u = await setup()
    u.adjustedItems.LABOR[0].amount = 100 // 低于实际 800
    const below = u.findItemsBelowActual()
    expect(below.length).toBeGreaterThan(0)
  })

  it('handleUpgrade 调整后低于实际成本时报错', async () => {
    const { ElMessage } = await import('element-plus')
    vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
    const u = await setup()
    u.adjustedItems.LABOR[0].amount = 100
    const formEl = { validate: () => Promise.resolve(true) }
    await u.handleUpgrade(formEl)
    expect(ElMessage.error).toHaveBeenCalled()
    expect(upgradeBudgetApi).not.toHaveBeenCalled()
  })

  it('onBeforeRouteLeave 调整项存在时弹确认并放行', async () => {
    const { ElMessageBox } = await import('element-plus')
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('ok')
    await setup()
    const next = vi.fn()
    routeLeaveCallback.cb(null, null, next)
    await nextTick()
    await nextTick()
    expect(next).toHaveBeenCalled()
  })

  it('onBeforeRouteLeave 有修改时弹确认', async () => {
    const { ElMessageBox } = await import('element-plus')
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('ok')
    const u = await setup()
    u.form.value.managementReserve = 999
    await nextTick()
    const next = vi.fn()
    routeLeaveCallback.cb(null, null, next)
    await nextTick()
    await nextTick()
    expect(next).toHaveBeenCalled()
  })

  it('handlePositionChange 有岗位时更新费率', async () => {
    const u = await setup()
    u.positionList.value = [{ id: 'pos1', name: '开发(DEV)' }]
    const row = { positionId: 'pos1', hours: 10, costRate: 0 }
    const { getCurrentRateApi } = await import('@/api/system/costQuota')
    getCurrentRateApi.mockResolvedValue({ code: 200, data: { costRate: 120 } })
    await u.handlePositionChange(row)
    expect(row.costRate).toBe(120)
    expect(row.roleCode).toBe('开发(DEV)')
  })

  it('watch route.params.id 变化重新加载', async () => {
    const u = await setup()
    getBudgetDetailWithItemsApi.mockClear()
    // 模拟 route 变化（直接调用 loadOriginalData 验证幂等逻辑）
    await u.loadOriginalData()
    expect(getBudgetDetailWithItemsApi).toHaveBeenCalled()
  })
})
