import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setupPinia, elementStubs } from '../helpers'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

vi.mock('vue-router', async () => {
  const actual = await vi.importActual('vue-router')
  return { ...actual, useRouter: () => ({ push: vi.fn(), replace: vi.fn(), go: vi.fn() }), useRoute: () => ({ path: '/', params: {}, query: {} }) }
})

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ code: 200, data: { records: [] } })),
    post: vi.fn(() => Promise.resolve({ code: 200 })),
    put: vi.fn(() => Promise.resolve({ code: 200 })),
    delete: vi.fn(() => Promise.resolve({ code: 200 })),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}))

describe('WorkHourDialog', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
  })

  function createWrapper(props = {}, validateImpl) {
    const formStub = validateImpl === undefined
      ? { template: '<div><slot/></div>' }
      : { template: '<div><slot/></div>', methods: { validate: validateImpl } }
    return import('@/components/WorkHourDialog.vue').then(m => mount(m.default, {
      props: { dateStr: '2026-01-05', editEntry: null, ...props },
      global: { stubs: { ...elementStubs, 'el-form': formStub, 'el-dialog': { template: '<div><slot/><slot name="footer"/></div>' } } },
    }))
  }

  it('渲染工时对话框', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.exists()).toBe(true)
  })

  it('新建模式下 isEdit 为 false', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.vm.isEdit).toBe(false)
  })

  it('编辑模式下 isEdit 为 true', async () => {
    const wrapper = await createWrapper({ editEntry: { id: 'wl1', hours: '8', projectId: 'p1' } })
    expect(wrapper.vm.isEdit).toBe(true)
  })

  it('open 打开弹窗', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.open()
    expect(wrapper.vm.visible).toBe(true)
  })

  it('loadProjects 加载已审批项目', async () => {
    request.get.mockResolvedValue({ code: 200, data: { records: [{ id: 'p1', status: 'APPROVED' }, { id: 'p2', status: 'DRAFT' }] } })
    const wrapper = await createWrapper()
    await wrapper.vm.loadProjects()
    expect(wrapper.vm.projects.length).toBe(1)
    expect(wrapper.vm.projects[0].id).toBe('p1')
  })

  it('loadExisting 加载当日已有工时', async () => {
    request.get.mockResolvedValue({ code: 200, data: [{ id: 'l1', logDate: '2026-01-05', hoursWorked: '4', status: 'DRAFT' }, { id: 'l2', logDate: '2026-01-06', hoursWorked: '8', status: 'APPROVED' }] })
    const wrapper = await createWrapper()
    await wrapper.vm.loadExisting()
    expect(wrapper.vm.existingEntries.length).toBe(1)
  })

  it('handleSubmit 新建保存', async () => {
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    const wrapper = await createWrapper({}, () => Promise.resolve(true))
    await wrapper.vm.$nextTick()
    wrapper.vm.form.projectId = 'p1'
    wrapper.vm.form.hoursWorked = 8
    await wrapper.vm.handleSubmit()
    expect(request.post).toHaveBeenCalled()
    expect(wrapper.vm.submitting).toBe(false)
  })

  it('handleSubmit 编辑更新', async () => {
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    const wrapper = await createWrapper({ editEntry: { id: 'wl1', hours: '8', projectId: 'p1', workDescription: 'x' } }, () => Promise.resolve(true))
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleSubmit()
    expect(request.put).toHaveBeenCalledWith('/pm/work-hours/wl1', expect.any(Object))
  })

  it('handleSubmit 校验失败不提交', async () => {
    const wrapper = await createWrapper({}, () => Promise.reject('invalid'))
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleSubmit()
    expect(request.post).not.toHaveBeenCalled()
  })

  it('handleDeleteCurrent 确认后删除', async () => {
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    const wrapper = await createWrapper({ editEntry: { id: 'wl1' } })
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleDeleteCurrent()
    expect(request.delete).toHaveBeenCalledWith('/pm/work-hours/wl1')
  })

  it('handleResubmitCurrent 重新提交', async () => {
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    const wrapper = await createWrapper({ editEntry: { id: 'wl1' } })
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleResubmitCurrent()
    expect(request.post).toHaveBeenCalledWith('/pm/work-hours/wl1/resubmit')
  })

  it('handleClose 重置表单', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.form = { projectId: 'p1', hoursWorked: 4, workDescription: 'x' }
    wrapper.vm.existingEntries = [{ id: 'l1' }]
    wrapper.vm.handleClose()
    expect(wrapper.vm.form.hoursWorked).toBe(8)
    expect(wrapper.vm.existingEntries).toEqual([])
  })

  it('每日合计计算', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.existingEntries = [{ hoursWorked: '4', status: 'DRAFT' }, { hoursWorked: '2.5', status: 'DRAFT' }]
    expect(wrapper.vm.dailyTotal).toBe('6.5')
  })
})

describe('WorkHourDialog 编辑模式边界', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
  })

  function createWrapper(props = {}) {
    return import('@/components/WorkHourDialog.vue').then(m => mount(m.default, {
      props: { dateStr: '2026-01-05', editEntry: null, ...props },
      global: { stubs: { ...elementStubs, 'el-dialog': { template: '<div><slot/><slot name="footer"/></div>' } } },
    }))
  }

  it('dailyTotal 编辑模式减去原工时', async () => {
    const wrapper = await createWrapper({ editEntry: { id: 'wl1', hours: '2' } })
    wrapper.vm.existingEntries = [{ hoursWorked: '4', status: 'DRAFT' }, { hoursWorked: '3', status: 'DRAFT' }]
    // 4 + 3 - 2 = 5
    expect(wrapper.vm.dailyTotal).toBe('5.0')
  })

  it('handleDelete 取消时不删除', async () => {
    const { ElMessageBox } = await import('element-plus')
    vi.spyOn(ElMessageBox, 'confirm').mockRejectedValue('cancel')
    const wrapper = await createWrapper()
    await wrapper.vm.handleDelete({ id: 'wl1' })
    expect(request.delete).not.toHaveBeenCalled()
  })

  it('handleResubmit 重新提交并触发事件', async () => {
    const { ElMessage } = await import('element-plus')
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    request.post.mockResolvedValue({ code: 200 })
    const wrapper = await createWrapper()
    const emitSpy = vi.fn()
    wrapper.vm.$emit = emitSpy
    await wrapper.vm.handleResubmit({ id: 'wl1' })
    expect(request.post).toHaveBeenCalledWith('/pm/work-hours/wl1/resubmit')
    expect(wrapper.vm.visible).toBe(false)
  })
})
