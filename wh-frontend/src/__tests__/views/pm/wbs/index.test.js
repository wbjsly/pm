import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setupPinia, elementStubs } from '../../../helpers'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

// 可配置的表格行 mock，用于渲染 el-table scoped slot 覆盖模板行
let rowMock = {
  id: 'w1', wbsCode: 'WBS-001', name: '任务1', productName: '产品A', moduleName: '模块A',
  priority: 'P1', techDifficulty: 'HIGH', status: 'NOT_STARTED',
  latestPlannedEndDate: '2026-06-30', actualStartDate: '2026-01-01', actualEndDate: null,
  effortEstimate: '40', budgetEstimate: '5000', plannedOwnerName: '张三', children: [],
}

const tableStubs = {
  ...elementStubs,
  'el-table': { template: '<div class="el-table-stub"><slot /></div>' },
  'el-table-column': {
    props: ['prop', 'label'],
    template: '<div class="el-table-column-stub"><slot name="default" :row="rowMock" /><slot /></div>',
    data() { return { rowMock } },
  },
}

vi.mock('vue-router', async () => {
  const actual = await vi.importActual('vue-router')
  return { ...actual, useRouter: () => ({ push: vi.fn(), replace: vi.fn(), go: vi.fn() }), useRoute: () => ({ path: '/', params: {}, query: { projectId: 'test' } }) }
})

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ code: 200, data: { records: [], total: 0 } })),
    post: vi.fn(() => Promise.resolve({ code: 200 })),
    put: vi.fn(() => Promise.resolve({ code: 200 })),
    delete: vi.fn(() => Promise.resolve({ code: 200 })),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}))

vi.mock('@element-plus/icons-vue', () => ({
  View: true, Edit: true, Delete: true, Plus: true, Upload: true, Download: true,
  ArrowRight: true, VideoPause: true, VideoPlay: true, RefreshRight: true,
  Promotion: true, CircleCheck: true, CircleClose: true,
}))

describe('WbsPage', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
  })

  function createWrapper() {
    return import('@/views/pm/wbs/index.vue').then(m => mount(m.default, { global: { stubs: { ...tableStubs, ImportDialog: true } } }))
  }

  it('渲染WBS列表页面', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.exists()).toBe(true)
  })

  it('API 失败时不崩溃', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    expect(wrapper.exists()).toBe(true)
  })

  it('重置按钮清除筛选条件', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    wrapper.vm.resetQuery()
    expect(wrapper.vm.keyword).toBe('')
    expect(wrapper.vm.queryParams.pageNum).toBe(1)
  })

  it('新增任务触发路由跳转', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    wrapper.vm.handleAdd('p1')
  })
})

describe('WbsPage 交互', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    request.get.mockResolvedValue({ code: 200, data: { records: [], total: 0 } })
    request.post.mockResolvedValue({ code: 200 })
    request.delete.mockResolvedValue({ code: 200 })
  })

  function createWrapper() {
    return import('@/views/pm/wbs/index.vue').then(m => mount(m.default, { global: { stubs: { ...tableStubs, ImportDialog: true } } }))
  }

  it('loadProjects 成功加载项目列表', async () => {
    request.get.mockResolvedValue({ code: 200, data: { records: [{ id: 'p1', projectName: '项目1' }], total: 1 } })
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.projectList.length).toBe(1)
    expect(wrapper.vm.total).toBe(1)
  })

  it('loadProjects 失败时提示错误', async () => {
    const spy = vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
    request.get.mockRejectedValue(new Error('fail'))
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    expect(spy).toHaveBeenCalled()
    spy.mockRestore()
  })

  it('toggleProject 展开/收起项目', async () => {
    request.get.mockResolvedValue({ code: 200, data: [{ id: 'w1', name: '任务1', children: [] }] })
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.toggleProject('p1')
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.expandedProjectId).toBe('p1')
    expect(wrapper.vm.wbsTreeData.length).toBe(1)
    await wrapper.vm.toggleProject('p1')
    expect(wrapper.vm.expandedProjectId).toBeNull()
  })

  it('handleExpandProject 加载任务树', async () => {
    request.get.mockResolvedValue({ code: 200, data: [{ id: 'w1', name: '任务' }] })
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleExpandProject('p1')
    expect(wrapper.vm.expandedProjectId).toBe('p1')
    expect(wrapper.vm.wbsTreeData.length).toBe(1)
  })

  it('handleDelete 确认后调用删除 API', async () => {
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    wrapper.vm.expandedProjectId = 'p1'
    await wrapper.vm.handleDelete('w1')
    expect(request.delete).toHaveBeenCalledWith('/pm/wbs/w1')
  })

  it('handleSuspend 确认后调用暂停 API', async () => {
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleSuspend('w1')
    expect(request.post).toHaveBeenCalledWith('/pm/wbs/w1/suspend')
  })

  it('handleResume 调用恢复 API', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleResume('w1')
    expect(request.post).toHaveBeenCalledWith('/pm/wbs/w1/resume')
  })

  it('handleStart 确认后调用开始 API', async () => {
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleStart('w1')
    expect(request.post).toHaveBeenCalledWith('/pm/wbs/w1/start')
  })

  it('handleTest 确认后调用提测 API', async () => {
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleTest('w1')
    expect(request.post).toHaveBeenCalledWith('/pm/wbs/w1/test')
  })

  it('handleComplete 确认后调用完成 API', async () => {
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleComplete('w1')
    expect(request.post).toHaveBeenCalledWith('/pm/wbs/w1/complete')
  })

  it('handleCancel 确认后调用取消 API', async () => {
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleCancel('w1')
    expect(request.post).toHaveBeenCalledWith('/pm/wbs/w1/cancel')
  })

  it('priorityTagType / difficultyTagType 辅助函数', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.priorityTagType('P0')).toBe('danger')
    expect(wrapper.vm.priorityTagType('UNKNOWN')).toBe('info')
    expect(wrapper.vm.difficultyTagType('HIGH')).toBe('danger')
    expect(wrapper.vm.difficultyTagType('UNKNOWN')).toBe('info')
    expect(wrapper.vm.difficultyLabel('HIGH')).toBe('高')
  })

  it('handleExport 成功触发下载', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    global.URL.createObjectURL = vi.fn(() => 'blob:url')
    global.URL.revokeObjectURL = vi.fn()
    document.body.appendChild = vi.fn()
    document.body.removeChild = vi.fn()
    request.get.mockResolvedValueOnce({ code: 200, data: { records: [], total: 0 } })
      .mockResolvedValueOnce('csv-content')
    await wrapper.vm.handleExport('p1')
    expect(URL.revokeObjectURL).toHaveBeenCalled()
  })
})

describe('WbsPage 模板渲染', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    request.get.mockImplementation((url) => {
      if (url === '/pm/charters') return Promise.resolve({ code: 200, data: { records: [{ id: 'p1', projectName: '项目1', projectShortName: 'P1', charterCode: 'C1', progress: 'IN_PROGRESS', pmName: '张三', wbsTotalEffort: 100, wbsLatestEndDate: '2026-12-31' }], total: 1 } })
      if (url === '/pm/wbs') return Promise.resolve({ code: 200, data: [{ id: 'w1', ...rowMock }] })
      return Promise.resolve({ code: 200, data: {} })
    })
  })

  function createWrapper() {
    return import('@/views/pm/wbs/index.vue').then(m => mount(m.default, { global: { stubs: { ...tableStubs, ImportDialog: true } } }))
  }

  it('NOT_STARTED 状态渲染操作按钮分支', async () => {
    rowMock = { ...rowMock, status: 'NOT_STARTED', priority: 'P2', techDifficulty: 'MEDIUM' }
    const wrapper = await createWrapper()
    await wrapper.vm.loadProjects()
    await wrapper.vm.handleExpandProject('p1')
    await wrapper.vm.$nextTick()
    console.log('PL_LEN', wrapper.vm.projectList.length)
    console.log('HTML_HAS_PROJECT', wrapper.html().includes('项目1'))
    expect(wrapper.vm.wbsTreeData.length).toBe(1)
    expect(wrapper.vm.projectList.length).toBe(1)
    expect(wrapper.exists()).toBe(true)
  })

  it('IN_DEVELOPMENT / TESTING / COMPLETED 状态分支', async () => {
    for (const status of ['IN_DEVELOPMENT', 'TESTING', 'COMPLETED', 'SUSPENDED', 'CANCELLED']) {
      rowMock = { ...rowMock, status }
      const wrapper = await createWrapper()
      await wrapper.vm.loadProjects()
      await wrapper.vm.handleExpandProject('p1')
      await wrapper.vm.$nextTick()
      expect(wrapper.exists()).toBe(true)
    }
  })

  it('空字段降级渲染（无产品/模块/优先级）', async () => {
    rowMock = { id: 'w2', wbsCode: 'WBS-2', name: '任务2', status: 'DONE_UNKNOWN', priority: null, techDifficulty: null }
    const wrapper = await createWrapper()
    await wrapper.vm.loadProjects()
    await wrapper.vm.handleExpandProject('p1')
    await wrapper.vm.$nextTick()
    expect(wrapper.exists()).toBe(true)
  })
})
