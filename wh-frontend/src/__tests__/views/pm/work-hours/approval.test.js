import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setupPinia, elementStubs } from '../../../helpers'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

vi.mock('vue-router', async () => {
  const actual = await vi.importActual('vue-router')
  return { ...actual, useRouter: () => ({ push: vi.fn(), replace: vi.fn(), go: vi.fn() }), useRoute: () => ({ path: '/', params: {}, query: {} }) }
})

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ code: 200, data: [] })),
    post: vi.fn(() => Promise.resolve({ code: 200 })),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}))

describe('WorkHoursApprovalPage', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
  })

  function createWrapper() {
    return import('@/views/pm/work-hours/approval.vue').then(m => mount(m.default, { global: { stubs: elementStubs } }))
  }

  it('渲染工时审批页面', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.exists()).toBe(true)
  })

  it('loadData 加载待审批数据并应用筛选', async () => {
    request.get.mockResolvedValue({ code: 200, data: [{ id: 'wl1', projectName: '项目A', createByName: '张三' }] })
    const wrapper = await createWrapper()
    await wrapper.vm.loadData()
    expect(wrapper.vm.allData.length).toBe(1)
    expect(wrapper.vm.tableData.length).toBe(1)
    expect(wrapper.vm.loading).toBe(false)
  })

  it('loadData 失败提示', async () => {
    vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
    request.get.mockRejectedValue(new Error('fail'))
    const wrapper = await createWrapper()
    await wrapper.vm.loadData()
    expect(ElMessage.error).toHaveBeenCalled()
  })

  it('applyFilter 按项目与创建人过滤', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.allData = [
      { id: '1', projectName: '项目A', createByName: '张三' },
      { id: '2', projectName: '项目B', createByName: '李四' },
    ]
    wrapper.vm.filterProject = '项目A'
    wrapper.vm.applyFilter()
    expect(wrapper.vm.tableData.length).toBe(1)
    wrapper.vm.filterProject = ''
    wrapper.vm.filterCreator = '李四'
    wrapper.vm.applyFilter()
    expect(wrapper.vm.tableData.length).toBe(1)
  })

  it('handleSelectionChange 记录选中行', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.handleSelectionChange([{ id: '1' }, { id: '2' }])
    expect(wrapper.vm.selectedRows.length).toBe(2)
  })

  it('handleApprove 确认后通过', async () => {
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    request.post.mockResolvedValue({ code: 200 })
    const wrapper = await createWrapper()
    wrapper.vm.loadData = vi.fn()
    await wrapper.vm.handleApprove({ id: 'wl1' })
    expect(request.post).toHaveBeenCalledWith('/pm/work-hours/wl1/approve', expect.any(Object))
  })

  it('handleReject 打开驳回弹窗', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.handleReject({ id: 'wl1' })
    expect(wrapper.vm.rejectVisible).toBe(true)
    expect(wrapper.vm.rejectForm.ids).toEqual(['wl1'])
  })

  it('confirmReject 无原因提示', async () => {
    vi.spyOn(ElMessage, 'warning').mockImplementation(() => {})
    const wrapper = await createWrapper()
    wrapper.vm.rejectForm = { reason: '', ids: ['wl1'] }
    await wrapper.vm.confirmReject()
    expect(ElMessage.warning).toHaveBeenCalled()
  })

  it('confirmReject 单个驳回', async () => {
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    request.post.mockResolvedValue({ code: 200 })
    const wrapper = await createWrapper()
    wrapper.vm.rejectForm = { reason: '不合格', ids: ['wl1'] }
    wrapper.vm.loadData = vi.fn()
    await wrapper.vm.confirmReject()
    expect(request.post).toHaveBeenCalledWith('/pm/work-hours/wl1/reject', { reason: '不合格' })
    expect(wrapper.vm.rejectVisible).toBe(false)
  })

  it('prevMonth / prevYear 计算', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.vm.prevMonth).toBeGreaterThanOrEqual(1)
  })
})

describe('WorkHoursApprovalPage 边界', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    request.get.mockResolvedValue({ code: 200, data: [] })
    request.post.mockResolvedValue({ code: 200 })
  })

  function createWrapper() {
    return import('@/views/pm/work-hours/approval.vue').then(m => mount(m.default, { global: { stubs: elementStubs } }))
  }

  it('prevMonth 1 月回退到 12 月', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.month = 1
    wrapper.vm.year = 2026
    expect(wrapper.vm.prevMonth).toBe(12)
    expect(wrapper.vm.prevYear).toBe(2025)
  })

  it('handleApprove 失败提示', async () => {
    const { ElMessage, ElMessageBox } = await import('element-plus')
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
    vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
    request.post.mockRejectedValue(new Error('fail'))
    const wrapper = await createWrapper()
    await wrapper.vm.handleApprove({ id: 'wl1' })
    expect(ElMessage.error).toHaveBeenCalled()
  })

  it('confirmReject 批量驳回', async () => {
    const { ElMessage } = await import('element-plus')
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    request.post.mockResolvedValue({ code: 200 })
    const wrapper = await createWrapper()
    wrapper.vm.rejectForm = { reason: '不合格', ids: ['wl1', 'wl2'] }
    wrapper.vm.loadData = vi.fn()
    await wrapper.vm.confirmReject()
    expect(request.post).toHaveBeenCalledWith('/pm/work-hours/batch-reject', expect.any(Object))
    expect(wrapper.vm.rejectVisible).toBe(false)
  })

  it('confirmReject 失败提示', async () => {
    const { ElMessage } = await import('element-plus')
    vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
    request.post.mockRejectedValue(new Error('fail'))
    const wrapper = await createWrapper()
    wrapper.vm.rejectForm = { reason: 'x', ids: ['wl1'] }
    await wrapper.vm.confirmReject()
    expect(ElMessage.error).toHaveBeenCalled()
  })

  it('handleBatchApprove 批量通过', async () => {
    const { ElMessage, ElMessageBox } = await import('element-plus')
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    request.post.mockResolvedValue({ code: 200, data: { success: 2, skipped: 1 } })
    const wrapper = await createWrapper()
    wrapper.vm.selectedRows = [{ id: 'wl1' }, { id: 'wl2' }]
    wrapper.vm.loadData = vi.fn()
    await wrapper.vm.handleBatchApprove()
    expect(request.post).toHaveBeenCalledWith('/pm/work-hours/batch-approve', expect.objectContaining({ ids: ['wl1', 'wl2'] }))
  })
})
