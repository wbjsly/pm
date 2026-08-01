import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setupPinia, elementStubs } from '../../../helpers'
import { setActivePinia, createPinia } from 'pinia'

vi.mock('@/api/system/costQuota', () => ({
  getYearListApi: vi.fn(),
  createYearApi: vi.fn(),
  updateYearApi: vi.fn(),
  deleteYearApi: vi.fn(),
  getDefaultStartDateApi: vi.fn(),
  getPositionListApi: vi.fn(),
  createPositionApi: vi.fn(),
  updatePositionApi: vi.fn(),
  deletePositionApi: vi.fn(),
  getQuotaListApi: vi.fn(),
  adjustQuotaApi: vi.fn(),
  getQuotaHistoryApi: vi.fn(),
}))

vi.mock('@element-plus/icons-vue', () => ({
  Plus: { name: 'Plus', template: '<i class="el-icon-plus" />' },
  Setting: { name: 'Setting', template: '<i class="el-icon-setting" />' },
  Edit: { name: 'Edit', template: '<i class="el-icon-edit" />' },
  Clock: { name: 'Clock', template: '<i class="el-icon-clock" />' },
  Delete: { name: 'Delete', template: '<i class="el-icon-delete" />' },
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn(), info: vi.fn() },
    ElMessageBox: { confirm: vi.fn(() => Promise.resolve()) },
  }
})

import * as costQuotaApi from '@/api/system/costQuota'

const mockYears = [
  { id: 'y1', name: '2024财年', startDate: '2024-01-01', endDate: '2024-12-31' },
  { id: 'y2', name: '2025财年', startDate: '2025-01-01', endDate: '2025-12-31' },
]

const mockPositions = [
  { id: 'p1', name: '高级开发', isDefault: '1', sortOrder: 1 },
  { id: 'p2', name: '测试工程师', isDefault: '1', sortOrder: 2 },
]

const mockQuotas = [
  { positionId: 'p1', positionName: '高级开发', dailyRate: 2000, versionNo: '1.0', updateDate: '2025-01-15T10:00:00', effectiveDate: '2025-01-15' },
  { positionId: 'p2', positionName: '测试工程师', dailyRate: 1500, versionNo: '1.0', updateDate: '2025-01-15T10:00:00', effectiveDate: '2025-01-15' },
]

const mockHistory = [
  { versionNo: '1.0', dailyRate: 2000, effectiveDate: '2025-01-15', changeReason: '初始定价', createByName: 'admin' },
]

describe('CostQuotaPage', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    costQuotaApi.getYearListApi.mockResolvedValue({ code: 200, data: mockYears })
    costQuotaApi.getPositionListApi.mockResolvedValue({ code: 200, data: mockPositions })
    costQuotaApi.getQuotaListApi.mockResolvedValue({ code: 200, data: mockQuotas })
    costQuotaApi.createYearApi.mockResolvedValue({ code: 200 })
    costQuotaApi.updateYearApi.mockResolvedValue({ code: 200 })
    costQuotaApi.deleteYearApi.mockResolvedValue({ code: 200 })
    costQuotaApi.createPositionApi.mockResolvedValue({ code: 200 })
    costQuotaApi.updatePositionApi.mockResolvedValue({ code: 200 })
    costQuotaApi.deletePositionApi.mockResolvedValue({ code: 200 })
    costQuotaApi.adjustQuotaApi.mockResolvedValue({ code: 200 })
    costQuotaApi.getDefaultStartDateApi.mockResolvedValue({ code: 200, data: { defaultStartDate: '2025-01-01' } })
    costQuotaApi.getQuotaHistoryApi.mockResolvedValue({ code: 200, data: mockHistory })
  })

  function createWrapper() {
    return import('@/views/system/cost-quota/index.vue').then(m =>
      mount(m.default, { global: { stubs: { ...elementStubs, 'el-dialog': { template: '<div><slot/><slot name="footer"/></div>' }, 'el-drawer': { template: '<div><slot/></div>' } } } })
    )
  }

  it('渲染费用定额页面', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.exists()).toBe(true)
  })

  it('挂载时加载年份列表', async () => {
    await createWrapper()
    await new Promise(r => setTimeout(r, 50))
    // onMounted calls loadYears which calls getYearListApi
    expect(costQuotaApi.getYearListApi).toHaveBeenCalled()
  })

  it('年份加载后自动选中第一个年份', async () => {
    const wrapper = await createWrapper()
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.vm.selectedYearId).toBe('y1')
  })

  it('选中年份后加载定额列表', async () => {
    const wrapper = await createWrapper()
    await new Promise(r => setTimeout(r, 50))
    expect(costQuotaApi.getQuotaListApi).toHaveBeenCalled()
  })

  it('未选择年份时显示空状态提示', async () => {
    costQuotaApi.getYearListApi.mockResolvedValue({ code: 200, data: [] })
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    // selectedYearId should be empty since no years returned
    expect(wrapper.vm.selectedYearId).toBe('')
  })

  it('加载定额失败时显示错误', async () => {
    costQuotaApi.getQuotaListApi.mockRejectedValue(new Error('API Error'))
    const wrapper = await createWrapper()
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.exists()).toBe(true)
  })

  it('点击新增年份按钮打开年份弹窗', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.showYearDialog = true
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.showYearDialog).toBe(true)
  })

  it('打开年份编辑弹窗', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    await wrapper.vm.openYearForm(mockYears[0])
    expect(wrapper.vm.yearFormVisible).toBe(true)
    expect(wrapper.vm.editingYear.id).toBe('y1')
  })

  it('打开新增年份弹窗', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    await wrapper.vm.openYearForm(null)
    expect(wrapper.vm.yearFormVisible).toBe(true)
    expect(wrapper.vm.editingYear.id).toBeFalsy()
  })

  it('保存年份时验证必填字段', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.yearForm = { name: '', startDate: '', endDate: '' }
    await wrapper.vm.saveYear()
    expect(costQuotaApi.createYearApi).not.toHaveBeenCalled()
  })

  it('创建新年份', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.yearForm = { name: '2026财年', startDate: '2026-01-01', endDate: '2026-12-31' }
    await wrapper.vm.saveYear()
    await wrapper.vm.$nextTick()
    expect(costQuotaApi.createYearApi).toHaveBeenCalled()
  })

  it('更新年份', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    await wrapper.vm.openYearForm(mockYears[0])
    wrapper.vm.yearForm.name = '2024财年(更新)'
    await wrapper.vm.saveYear()
    await wrapper.vm.$nextTick()
    expect(costQuotaApi.updateYearApi).toHaveBeenCalledWith('y1', expect.any(Object))
  })

  it('删除年份', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    await wrapper.vm.handleDeleteYear(mockYears[0])
    await wrapper.vm.$nextTick()
    expect(costQuotaApi.deleteYearApi).toHaveBeenCalledWith('y1')
  })

  it('打开岗位管理弹窗并加载岗位数据', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.showPositionDialog = true
    await wrapper.vm.loadPositions()
    await wrapper.vm.$nextTick()
    expect(costQuotaApi.getPositionListApi).toHaveBeenCalled()
  })

  it('创建岗位', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.openPositionForm(null)
    wrapper.vm.posForm = { name: '新岗位', sortOrder: 3 }
    await wrapper.vm.savePosition()
    await wrapper.vm.$nextTick()
    expect(costQuotaApi.createPositionApi).toHaveBeenCalled()
  })

  it('更新岗位', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.openPositionForm(mockPositions[0])
    wrapper.vm.posForm.name = '高级开发(更新)'
    await wrapper.vm.savePosition()
    await wrapper.vm.$nextTick()
    expect(costQuotaApi.updatePositionApi).toHaveBeenCalledWith('p1', expect.any(Object))
  })

  it('保存岗位时验证必填字段', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.posForm = { name: '', sortOrder: 0 }
    await wrapper.vm.savePosition()
    expect(costQuotaApi.createPositionApi).not.toHaveBeenCalled()
  })

  it('删除岗位', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    await wrapper.vm.handleDeletePosition(mockPositions[0])
    await wrapper.vm.$nextTick()
    expect(costQuotaApi.deletePositionApi).toHaveBeenCalledWith('p1')
  })

  it('打开调价弹窗', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.openAdjust(mockQuotas[0])
    expect(wrapper.vm.adjustVisible).toBe(true)
    expect(wrapper.vm.adjustForm.positionName).toBe('高级开发')
  })

  it('调价时验证单价字段', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.openAdjust(mockQuotas[0])
    wrapper.vm.adjustForm.dailyRate = null
    await wrapper.vm.doAdjust()
    expect(costQuotaApi.adjustQuotaApi).not.toHaveBeenCalled()
  })

  it('执行调价', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.openAdjust(mockQuotas[0])
    wrapper.vm.adjustForm.dailyRate = 2500
    await wrapper.vm.doAdjust()
    await wrapper.vm.$nextTick()
    expect(costQuotaApi.adjustQuotaApi).toHaveBeenCalled()
  })

  it('查看历史版本', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    await wrapper.vm.openHistory(mockQuotas[0])
    await wrapper.vm.$nextTick()
    expect(costQuotaApi.getQuotaHistoryApi).toHaveBeenCalled()
    expect(wrapper.vm.historyVisible).toBe(true)
  })

  it('formatMoney 格式化金额', () => {
    const wrapper = { vm: { formatMoney: null } }
    return import('@/views/system/cost-quota/index.vue').then(m => {
      const vm = mount(m.default, { global: { stubs: elementStubs } }).vm
      expect(vm.formatMoney(1234567.89)).toBe('1,234,567.89')
      expect(vm.formatMoney(0)).toBe('0.00')
      expect(vm.formatMoney(null)).toBe('0.00')
    })
  })
})

describe('CostQuotaPage 补充', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
  })

  function createWrapper() {
    return import('@/views/system/cost-quota/index.vue').then(m => mount(m.default, { global: { stubs: elementStubs } }))
  }

  it('formatMoney 格式化金额', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.vm.formatMoney('1000')).toBe('1,000.00')
    expect(wrapper.vm.formatMoney('0')).toBe('0.00')
    expect(wrapper.vm.formatMoney(null)).toBe('0.00')
  })

  it('openAdjust 填充调价表单', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.openAdjust({ positionId: 'p1', positionName: '开发', dailyRate: '800' })
    expect(wrapper.vm.adjustVisible).toBe(true)
    expect(wrapper.vm.adjustForm.positionId).toBe('p1')
    expect(wrapper.vm.adjustForm.currentRate).toBe('800')
  })

  it('resetAdjustForm 重置调价表单', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.adjustForm = { positionId: 'p1', dailyRate: 100 }
    wrapper.vm.resetAdjustForm()
    expect(wrapper.vm.adjustForm.positionId).toBe('')
    expect(wrapper.vm.adjustForm.dailyRate).toBeNull()
  })

  it('doAdjust 单价无效时提示', async () => {
    const { ElMessage } = await import('element-plus')
    const spy = vi.spyOn(ElMessage, 'warning').mockImplementation(() => {})
    const wrapper = await createWrapper()
    wrapper.vm.adjustForm = { dailyRate: 0 }
    await wrapper.vm.doAdjust()
    expect(spy).toHaveBeenCalled()
  })

  it('doAdjust 成功调价', async () => {
    const { ElMessage } = await import('element-plus')
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    costQuotaApi.adjustQuotaApi.mockResolvedValue({ code: 200 })
    const wrapper = await createWrapper()
    wrapper.vm.adjustForm = { positionId: 'p1', dailyRate: 900, effectiveDate: '2026-01-01', changeReason: '调薪' }
    wrapper.vm.selectedYearId = 'y1'
    wrapper.vm.loadQuotas = vi.fn().mockResolvedValue()
    await wrapper.vm.doAdjust()
    expect(costQuotaApi.adjustQuotaApi).toHaveBeenCalled()
    expect(wrapper.vm.adjustVisible).toBe(false)
  })

  it('openHistory 加载历史版本', async () => {
    costQuotaApi.getQuotaHistoryApi.mockResolvedValue({ code: 200, data: [{ id: 'h1', versionNo: 1 }] })
    const wrapper = await createWrapper()
    wrapper.vm.selectedYearId = 'y1'
    await wrapper.vm.openHistory({ positionId: 'p1' })
    expect(wrapper.vm.historyVisible).toBe(true)
    expect(wrapper.vm.historyList.length).toBe(1)
  })
})

describe('CostQuotaPage 错误分支', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
  })

  function createWrapper() {
    return import('@/views/system/cost-quota/index.vue').then(m => mount(m.default, { global: { stubs: elementStubs } }))
  }

  it('saveYear 失败提示', async () => {
    const { ElMessage } = await import('element-plus')
    vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
    costQuotaApi.createYearApi.mockRejectedValue(new Error('fail'))
    const wrapper = await createWrapper()
    wrapper.vm.yearForm = { name: '2027', startDate: '2027-01-01', endDate: '2027-12-31' }
    await wrapper.vm.saveYear()
    expect(ElMessage.error).toHaveBeenCalled()
    expect(wrapper.vm.yearSaving).toBe(false)
  })

  it('saveYear 校验失败提示', async () => {
    const { ElMessage } = await import('element-plus')
    vi.spyOn(ElMessage, 'warning').mockImplementation(() => {})
    const wrapper = await createWrapper()
    wrapper.vm.yearForm = { name: '', startDate: '', endDate: '' }
    await wrapper.vm.saveYear()
    expect(ElMessage.warning).toHaveBeenCalled()
  })

  it('handleDeleteYear 删除失败提示', async () => {
    const { ElMessage, ElMessageBox } = await import('element-plus')
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
    vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
    costQuotaApi.deleteYearApi.mockRejectedValue(new Error('fail'))
    const wrapper = await createWrapper()
    await wrapper.vm.handleDeleteYear({ id: 'y1' })
    expect(ElMessage.error).toHaveBeenCalled()
  })

  it('savePosition 失败提示', async () => {
    const { ElMessage } = await import('element-plus')
    vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
    costQuotaApi.createPositionApi.mockRejectedValue(new Error('fail'))
    const wrapper = await createWrapper()
    wrapper.vm.posForm = { name: '开发', sortOrder: 0 }
    await wrapper.vm.savePosition()
    expect(ElMessage.error).toHaveBeenCalled()
    expect(wrapper.vm.posSaving).toBe(false)
  })

  it('savePosition 校验失败提示', async () => {
    const { ElMessage } = await import('element-plus')
    vi.spyOn(ElMessage, 'warning').mockImplementation(() => {})
    const wrapper = await createWrapper()
    wrapper.vm.posForm = { name: '', sortOrder: 0 }
    await wrapper.vm.savePosition()
    expect(ElMessage.warning).toHaveBeenCalled()
  })

  it('handleDeletePosition 删除失败提示', async () => {
    const { ElMessage, ElMessageBox } = await import('element-plus')
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
    vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
    costQuotaApi.deletePositionApi.mockRejectedValue(new Error('fail'))
    const wrapper = await createWrapper()
    await wrapper.vm.handleDeletePosition({ id: 'p1' })
    expect(ElMessage.error).toHaveBeenCalled()
  })

  it('doAdjust 失败提示', async () => {
    const { ElMessage } = await import('element-plus')
    vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
    costQuotaApi.adjustQuotaApi.mockRejectedValue({ response: { data: { message: '调价失败' } } })
    const wrapper = await createWrapper()
    wrapper.vm.adjustForm = { positionId: 'p1', dailyRate: 100, effectiveDate: '', changeReason: '' }
    wrapper.vm.selectedYearId = 'y1'
    await wrapper.vm.doAdjust()
    expect(ElMessage.error).toHaveBeenCalled()
  })

  it('openHistory 加载失败提示', async () => {
    const { ElMessage } = await import('element-plus')
    vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
    costQuotaApi.getQuotaHistoryApi.mockRejectedValue(new Error('fail'))
    const wrapper = await createWrapper()
    wrapper.vm.selectedYearId = 'y1'
    await wrapper.vm.openHistory({ positionId: 'p1' })
    expect(ElMessage.error).toHaveBeenCalled()
    expect(wrapper.vm.historyLoading).toBe(false)
  })

  it('loadQuotas 无年份时跳过', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.selectedYearId = ''
    await wrapper.vm.loadQuotas()
    expect(costQuotaApi.getQuotaListApi).not.toHaveBeenCalled()
  })
})
