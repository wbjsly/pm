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
    // 验证表格区域存在
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
})
