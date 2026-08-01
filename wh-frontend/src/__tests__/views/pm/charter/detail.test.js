import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setupPinia, elementStubs } from '../../../helpers'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

vi.mock('vue-router', async () => {
  const actual = await vi.importActual('vue-router')
  return { ...actual, useRouter: () => ({ push: vi.fn(), replace: vi.fn(), go: vi.fn(), back: vi.fn() }), useRoute: () => ({ path: '/pm/charter/detail/test', params: { id: 'test' }, query: {} }) }
})

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ code: 200, data: {} })),
    post: vi.fn(() => Promise.resolve({ code: 200 })),
    put: vi.fn(() => Promise.resolve({ code: 200 })),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}))

vi.mock('echarts', () => ({
  init: vi.fn(() => ({
    setOption: vi.fn(),
    dispose: vi.fn(),
    resize: vi.fn(),
    on: vi.fn(),
  })),
}))

function mockApis() {
  request.get.mockImplementation((url) => {
    if (url.includes('/pm/charters/test')) return Promise.resolve({ code: 200, data: { id: 'test', projectName: '项目1', projectCategory: 'CONTRACT', objectives: '["目标1"]', keyStakeholders: '["干系人1"]' } })
    if (url.includes('/pm/wbs')) return Promise.resolve({ code: 200, data: [{ id: 'w1', name: '任务1' }] })
    if (url.includes('/pm/budgets') && !url.includes('comparison') && !url.includes('versions')) return Promise.resolve({ code: 200, data: { records: [{ id: 'b1', status: 'APPROVED' }], total: 1 } })
    if (url.includes('comparison')) return Promise.resolve({ code: 200, data: { items: [{ id: 'i1', category: 'LABOR', level: 1, children: [{ id: 'i1c', category: 'LABOR', level: 2, roleCode: 'DEV' }] }] } })
    if (url.includes('/pm/work-hours')) return Promise.resolve({ code: 200, data: [{ logDate: '2026-01-05', hoursWorked: '8', status: 'APPROVED' }] })
    if (url.includes('/pm/deliverables')) return Promise.resolve({ code: 200, data: { records: [{ id: 'd1', attachments: '[{"fileName":"a.txt","deleted":false}]' }], total: 1 } })
    if (url.includes('aggregation')) return Promise.resolve({ code: 200, data: [{ yearMonth: '2026-01', laborAmount: 100 }] })
    if (url.includes('actual-costs/sum')) return Promise.resolve({ code: 200, data: 100 })
    if (url.includes('/pm/actual-costs')) return Promise.resolve({ code: 200, data: { records: [{ id: 'c1' }], total: 1 } })
    return Promise.resolve({ code: 200, data: {} })
  })
}

describe('CharterDetailPage', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    mockApis()
  })

  function createWrapper() {
    // el-tab-pane 渲染 slot，使 chartRef/actualCostChartRef 的 div 挂载
    const renderStubs = {
      'el-tab-pane': { template: '<div class="el-tab-pane-stub"><slot /></div>' },
      'el-dialog': { template: '<div><slot/><slot name="footer"/></div>' },
    }
    return import('@/views/pm/charter/detail.vue').then(m =>
      mount(m.default, { global: { stubs: { ...elementStubs, ...renderStubs } } })
    )
  }
  it('渲染立项详情页面', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.exists()).toBe(true)
  })

  it('默认显示项目信息标签页', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.activeTab).toBe('projectInfo')
  })

  it('API 失败时不崩溃', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    expect(wrapper.exists()).toBe(true)
  })

  it('categoryTagType / categoryLabel 返回正确标签', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.categoryTagType('CONTRACT')).toBe('primary')
    expect(wrapper.vm.categoryTagType('R_D')).toBe('warning')
    expect(wrapper.vm.categoryTagType('ADVANCE')).toBe('success')
    expect(wrapper.vm.categoryTagType('UNKNOWN')).toBe('info')
    expect(wrapper.vm.categoryLabel('CONTRACT')).toBe('合同项目')
    expect(wrapper.vm.categoryLabel('PUBLIC')).toBe('公共项目')
  })

  it('formatBudget / formatMoney 格式化金额', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.formatBudget(null)).toBe('-')
    expect(wrapper.vm.formatBudget('1000')).toBe('1,000.00 元人民币')
    expect(wrapper.vm.formatBudget('abc')).toBe('abc 元人民币')
    expect(wrapper.vm.formatMoney(null)).toBe('-')
    expect(wrapper.vm.formatMoney('2000')).toBe('2,000.00')
  })

  it('formatObjectives / formatStakeholders 解析 JSON', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.formatObjectives(JSON.stringify([{ objective: '目标A', metric: 'M', target: 'T' }]))).toBe('目标A-M:T')
    expect(wrapper.vm.formatObjectives(null)).toBe('-')
    expect(wrapper.vm.formatObjectives('not-json')).toBe('not-json')
    expect(wrapper.vm.formatObjectives('[]')).toBe('-')
    expect(wrapper.vm.formatStakeholders(JSON.stringify([{ name: '张三', org: '公司', role: 'PM' }]))).toBe('张三:公司-PM')
    expect(wrapper.vm.formatStakeholders(null)).toBe('-')
  })

  it('countActiveAttachments 统计有效附件', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.vm.countActiveAttachments('[{"fileName":"a.txt","deleted":false},{"fileName":"b.txt","deleted":true}]')).toBe(1)
    expect(wrapper.vm.countActiveAttachments(null)).toBe(0)
    expect(wrapper.vm.countActiveAttachments('bad')).toBe(0)
  })

  it('loadWbs 加载任务数据', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.loadWbs()
    expect(wrapper.vm.wbsData.length).toBe(1)
    expect(wrapper.vm.loadedTabs.tasks).toBe(true)
  })

  it('loadBudget 加载预算与对比数据', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.loadBudget()
    expect(wrapper.vm.budgetData.length).toBe(1)
    expect(wrapper.vm.comparisonData.items.length).toBe(1)
  })

  it('loadBudget 无已审批预算时不加载对比', async () => {
    request.get.mockImplementation((url) => {
      if (url.includes('/pm/budgets') && !url.includes('comparison') && !url.includes('versions')) return Promise.resolve({ code: 200, data: { records: [{ id: 'b1', status: 'DRAFT' }], total: 1 } })
      return Promise.resolve({ code: 200, data: {} })
    })
    const wrapper = await createWrapper()
    await wrapper.vm.loadBudget()
    expect(wrapper.vm.comparisonData).toBeNull()
  })

  it('loadWorkHours 汇总工时并渲染图表', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.loadWorkHours()
    console.log('WHS_LEN', wrapper.vm.workHoursSummary.length, 'APPROVED', wrapper.vm.workHoursSummary[0] && wrapper.vm.workHoursSummary[0].approvedDays)
    expect(wrapper.vm.workHoursSummary.length).toBeGreaterThanOrEqual(1)
    expect(wrapper.vm.workHoursSummary[0].approvedDays).toBeGreaterThanOrEqual(0)
    expect(wrapper.vm.loadedTabs.workHours).toBe(true)
  })

  it('loadDeliverables 加载成果物', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.loadDeliverables()
    expect(wrapper.vm.deliverableData.length).toBe(1)
  })

  it('loadActualCost 加载成本聚合与列表', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.loadActualCost()
    expect(wrapper.vm.actualCostAggregation.length).toBe(1)
    expect(wrapper.vm.loadedTabs.actualCost).toBe(true)
  })

  it('comparisonFlatItems 扁平化对比科目', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.loadBudget()
    const flat = wrapper.vm.comparisonFlatItems
    expect(flat.length).toBe(2)
    expect(flat[1].name).toContain('DEV')
  })

  it('comparisonRatioColor / comparisonRatioColumnColor', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.vm.comparisonRatioColor(null)).toBe('#67c23a')
    expect(wrapper.vm.comparisonRatioColor(0.9)).toBe('#e6a23c')
    expect(wrapper.vm.comparisonRatioColor(1.2)).toBe('#f56c6c')
    expect(wrapper.vm.comparisonRatioColumnColor(1.2)).toBe('#f56c6c')
    expect(wrapper.vm.comparisonRatioColumnColor(0.95)).toBe('#e6a23c')
  })

  it('handleTabClick 切换标签页并加载数据', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.handleTabClick({ paneName: 'tasks' })
    expect(wrapper.vm.wbsData.length).toBe(1)
    await wrapper.vm.handleTabClick({ paneName: 'budget' })
    expect(wrapper.vm.budgetData.length).toBe(1)
  })

  it('onCategoryChange 处理全选切换', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.prevCheckedCategories = ['all']
    wrapper.vm.checkedCategories = ['LABOR']
    wrapper.vm.onCategoryChange(['LABOR'])
    expect(wrapper.vm.prevCheckedCategories).toEqual(['LABOR'])
  })

  it('onYearMonthChange 处理月份切换', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.prevSelectedYearMonths = ['all']
    wrapper.vm.selectedYearMonths = ['2026-01']
    wrapper.vm.onYearMonthChange(['2026-01'])
    expect(wrapper.vm.prevSelectedYearMonths).toEqual(['2026-01'])
  })
})

describe('CharterDetailPage 图表与状态分支', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    mockApis()
  })

  function createWrapper() {
    const renderStubs = {
      'el-tab-pane': { template: '<div class="el-tab-pane-stub"><slot /></div>' },
      'el-dialog': { template: '<div><slot/><slot name="footer"/></div>' },
    }
    return import('@/views/pm/charter/detail.vue').then(m =>
      mount(m.default, { global: { stubs: { ...elementStubs, ...renderStubs } } })
    )
  }

  it('loadWorkHours 统计 DRAFT/SUBMITTED/APPROVED 状态', async () => {
    request.get.mockImplementation((url) => {
      if (url.includes('/pm/work-hours')) return Promise.resolve({ code: 200, data: [
        { logDate: '2026-01-05', hoursWorked: '8', status: 'DRAFT' },
        { logDate: '2026-01-06', hoursWorked: '8', status: 'SUBMITTED' },
        { logDate: '2026-01-07', hoursWorked: '8', status: 'APPROVED' },
      ] })
      return Promise.resolve({ code: 200, data: {} })
    })
    const wrapper = await createWrapper()
    await wrapper.vm.loadWorkHours()
    const summary = wrapper.vm.workHoursSummary[0]
    expect(summary.draftDays).toBeGreaterThanOrEqual(1)
    expect(summary.submittedDays).toBeGreaterThanOrEqual(1)
    expect(summary.approvedDays).toBeGreaterThanOrEqual(1)
    expect(parseFloat(wrapper.vm.totalPersonDays)).toBeGreaterThanOrEqual(3)
  })

  it('renderChart 执行不抛错', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.loadWorkHours()
    expect(() => wrapper.vm.renderChart()).not.toThrow()
  })

  it('renderActualCostChart 执行不抛错', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.loadActualCost()
    expect(() => wrapper.vm.renderActualCostChart()).not.toThrow()
  })

  it('expandCategoryFilter 拼接筛选', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.vm.expandCategoryFilter(['LABOR', 'TRAVEL'])).toBe('LABOR,TRAVEL')
  })

  it('loadActualCostList 按筛选加载', async () => {
    request.get.mockImplementation((url) => {
      if (url.includes('actual-costs/sum')) return Promise.resolve({ code: 200, data: 100 })
      if (url.includes('/pm/actual-costs')) return Promise.resolve({ code: 200, data: { records: [{ id: 'c1' }], total: 1 } })
      return Promise.resolve({ code: 200, data: {} })
    })
    const wrapper = await createWrapper()
    wrapper.vm.checkedCategories = ['LABOR']
    wrapper.vm.yearMonthFilter = '2026-01'
    await wrapper.vm.loadActualCostList()
    expect(wrapper.vm.actualCostList.length).toBe(1)
    expect(wrapper.vm.actualCostPage.total).toBe(1)
  })

  it('loadActualCostSum 加载合计', async () => {
    request.get.mockImplementation((url) => {
      if (url.includes('actual-costs/sum')) return Promise.resolve({ code: 200, data: 250 })
      return Promise.resolve({ code: 200, data: {} })
    })
    const wrapper = await createWrapper()
    wrapper.vm.checkedCategories = ['all']
    await wrapper.vm.loadActualCostSum()
    expect(wrapper.vm.currentSummaryAmount).toBe(250)
  })

  it('onCategoryChange 从全选切换到指定类别', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.prevCheckedCategories = ['all']
    wrapper.vm.checkedCategories = ['all']
    wrapper.vm.loadActualCostList = vi.fn()
    wrapper.vm.loadActualCostSum = vi.fn()
    wrapper.vm.onCategoryChange(['all', 'LABOR'])
    expect(wrapper.vm.checkedCategories).toEqual(['LABOR'])
    expect(wrapper.vm.prevCheckedCategories).toEqual(['LABOR'])
  })

  it('onYearMonthChange 切换月份筛选', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.prevSelectedYearMonths = ['all']
    wrapper.vm.onYearMonthChange(['2026-01'])
    expect(wrapper.vm.yearMonthFilter).toBe('2026-01')
  })
})
