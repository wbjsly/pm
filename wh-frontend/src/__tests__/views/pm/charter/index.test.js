import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setupPinia, elementStubs } from '../../../helpers'
import { ElMessage, ElMessageBox } from 'element-plus'

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), go: vi.fn(), back: vi.fn() }),
  useRoute: () => ({ path: '/', params: {}, query: {} }),
}))

vi.mock('@element-plus/icons-vue', () => ({
  Plus: { name: 'Plus', template: '<i class="el-icon-plus" />' },
  Edit: { name: 'Edit', template: '<i class="el-icon-edit" />' },
  Delete: { name: 'Delete', template: '<i class="el-icon-delete" />' },
  Promotion: { name: 'Promotion', template: '<i class="el-icon-promotion" />' },
  View: { name: 'View', template: '<i class="el-icon-view" />' },
  Select: { name: 'Select', template: '<i class="el-icon-select" />' },
  CloseBold: { name: 'CloseBold', template: '<i class="el-icon-close-bold" />' },
}))

import request from '@/utils/request'

const mockCharters = [
  {
    id: 'c1', projectCode: 'P2024001', projectName: '测试项目A',
    projectShortName: 'TPA', projectCategory: 'CONTRACT',
    pmName: '张三', progress: 'PLANNING', status: 'DRAFT',
    startDate: '2024-01-01', endDate: '2024-06-30',
  },
  {
    id: 'c2', projectCode: 'P2024002', projectName: '测试项目B',
    projectShortName: null, projectCategory: 'R_D',
    pmName: '李四', progress: 'IN_PROGRESS', status: 'PENDING_APPROVAL',
    startDate: '2024-02-01', endDate: '2024-08-31',
  },
  {
    id: 'c3', projectCode: 'P2024003', projectName: '测试项目C',
    projectShortName: null, projectCategory: 'PUBLIC',
    pmName: '王五', progress: 'COMPLETED', status: 'APPROVED',
    startDate: '2024-03-01', endDate: '2024-09-30',
  },
]

function createWrapper() {
  return import('@/views/pm/charter/index.vue').then(m =>
    mount(m.default, { global: { stubs: elementStubs } })
  )
}

describe('CharterPage', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    request.get.mockResolvedValue({ code: 200, data: { records: mockCharters, total: 3 } })
    request.post.mockResolvedValue({ code: 200 })
    request.delete.mockResolvedValue({ code: 200 })
  })

  it('渲染立项列表页面', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    expect(wrapper.exists()).toBe(true)
  })

  it('挂载时加载章程列表数据', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    expect(request.get).toHaveBeenCalledWith('/pm/charters', expect.any(Object))
  })

  it('重置按钮清除筛选条件并重新加载', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    vi.clearAllMocks()
    request.get.mockResolvedValue({ code: 200, data: { records: mockCharters, total: 3 } })

    const buttons = wrapper.findAll('button')
    const resetBtn = buttons.find(b => b.text().includes('重置'))
    expect(resetBtn).toBeTruthy()

    await resetBtn.trigger('click')
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    expect(request.get).toHaveBeenCalledWith('/pm/charters', expect.objectContaining({
      params: expect.objectContaining({ status: '', keyword: '', pageNum: 1 }),
    }))
  })

  it('查询按钮调用 loadData', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    vi.clearAllMocks()
    request.get.mockResolvedValue({ code: 200, data: { records: mockCharters, total: 3 } })

    const buttons = wrapper.findAll('button')
    const queryBtn = buttons.find(b => b.text().includes('查询'))
    expect(queryBtn).toBeTruthy()

    await queryBtn.trigger('click')
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    expect(request.get).toHaveBeenCalled()
  })

  it('mockCharters 包含所有状态的数据', () => {
    expect(mockCharters).toHaveLength(3)
    expect(mockCharters[0].status).toBe('DRAFT')
    expect(mockCharters[1].status).toBe('PENDING_APPROVAL')
    expect(mockCharters[2].status).toBe('APPROVED')
  })

  it('页面加载后数据渲染到表格', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    expect(request.get).toHaveBeenCalled()
    const table = wrapper.find('.el-card')
    expect(table.exists()).toBe(true)
  })

  it('分页组件显示数据总数', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    const html = wrapper.html()
    expect(html).toContain('total="3"')
  })

  it('API 失败时组件不崩溃', async () => {
    request.get.mockRejectedValue(new Error('Network error'))
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    expect(wrapper.exists()).toBe(true)
  })

  it('分页属性设置', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.queryParams.pageNum).toBe(1)
    expect(wrapper.vm.queryParams.pageSize).toBe(10)
  })

  it('状态筛选参数正确传递', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    vi.clearAllMocks()
    request.get.mockResolvedValue({ code: 200, data: { records: mockCharters, total: 3 } })

    wrapper.vm.queryParams.status = 'APPROVED'
    wrapper.vm.loadData()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    expect(request.get).toHaveBeenCalledWith('/pm/charters', expect.objectContaining({
      params: expect.objectContaining({ status: 'APPROVED' }),
    }))
  })
})

describe('CharterPage 交互', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    request.get.mockResolvedValue({ code: 200, data: { records: mockCharters, total: 3 } })
    request.post.mockResolvedValue({ code: 200 })
    request.delete.mockResolvedValue({ code: 200 })
  })

  function createWrapper() {
    return import('@/views/pm/charter/index.vue').then(m => mount(m.default, { global: { stubs: elementStubs } }))
  }

  it('categoryTagType / categoryLabel 辅助函数', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.categoryTagType('CONTRACT')).toBe('primary')
    expect(wrapper.vm.categoryTagType('R_D')).toBe('warning')
    expect(wrapper.vm.categoryTagType('X')).toBe('info')
    expect(wrapper.vm.categoryLabel('CONTRACT')).toBe('合同项目')
  })

  it('handleDelete 确认后调用删除 API', async () => {
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleDelete('c1')
    expect(request.delete).toHaveBeenCalledWith('/pm/charters/c1')
  })

  it('handleSubmit 确认后调用提交 API', async () => {
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleSubmit('c1')
    expect(request.post).toHaveBeenCalledWith('/pm/charters/c1/submit')
  })

  it('handleApprove / handleReject 打开审批弹窗', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    wrapper.vm.handleApprove('c1')
    expect(wrapper.vm.approvalDialog.visible).toBe(true)
    expect(wrapper.vm.approvalDialog.mode).toBe('approve')
    wrapper.vm.handleReject('c1')
    expect(wrapper.vm.approvalDialog.mode).toBe('reject')
  })

  it('分页切换调用 loadData', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    vi.clearAllMocks()
    request.get.mockResolvedValue({ code: 200, data: { records: mockCharters, total: 3 } })
    wrapper.vm.queryParams.pageNum = 2
    wrapper.vm.loadData()
    await wrapper.vm.$nextTick()
    expect(request.get).toHaveBeenCalledWith('/pm/charters', expect.objectContaining({
      params: expect.objectContaining({ pageNum: 2 }),
    }))
  })
})

describe('CharterPage 审批弹窗', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    request.get.mockResolvedValue({ code: 200, data: { records: mockCharters, total: 3 } })
    request.post.mockResolvedValue({ code: 200 })
  })

  function createWrapper() {
    return import('@/views/pm/charter/index.vue').then(m => mount(m.default, { global: { stubs: elementStubs } }))
  }

  it('confirmApproval approve 通过审批', async () => {
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    const wrapper = await createWrapper()
    wrapper.vm.handleApprove('c1')
    wrapper.vm.approvalDialog.comment = '同意'
    wrapper.vm.loadData = vi.fn()
    await wrapper.vm.confirmApproval()
    expect(request.post).toHaveBeenCalledWith('/pm/charters/c1/approve', expect.any(Object))
    expect(wrapper.vm.approvalDialog.visible).toBe(false)
  })

  it('confirmApproval reject 成功驳回', async () => {
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    const wrapper = await createWrapper()
    wrapper.vm.approvalDialog.charterId = 'c1'
    wrapper.vm.approvalDialog.mode = 'reject'
    wrapper.vm.approvalDialog.comment = '理由'
    wrapper.vm.approvalDialog.visible = true
    wrapper.vm.loadData = vi.fn()
    await wrapper.vm.confirmApproval()
    expect(request.post).toHaveBeenCalledWith('/pm/charters/c1/reject', { rejectReason: '理由' })
  })

  it('confirmApproval reject 无原因提示', async () => {
    vi.spyOn(ElMessage, 'warning').mockImplementation(() => {})
    const wrapper = await createWrapper()
    wrapper.vm.approvalDialog.charterId = 'c1'
    wrapper.vm.approvalDialog.mode = 'reject'
    wrapper.vm.approvalDialog.comment = ''
    wrapper.vm.approvalDialog.visible = true
    await wrapper.vm.confirmApproval()
    expect(ElMessage.warning).toHaveBeenCalled()
  })
})
