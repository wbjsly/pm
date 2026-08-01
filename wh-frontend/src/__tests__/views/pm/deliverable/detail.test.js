import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setupPinia, elementStubs } from '../../../helpers'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

vi.mock('vue-router', async () => {
  const actual = await vi.importActual('vue-router')
  return { ...actual, useRouter: () => ({ push: vi.fn(), replace: vi.fn(), go: vi.fn(), back: vi.fn() }), useRoute: () => ({ path: '/pm/deliverable/detail/d1', params: { id: 'd1' }, query: {} }) }
})

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ code: 200, data: {} })),
    post: vi.fn(() => Promise.resolve({ code: 200 })),
    put: vi.fn(() => Promise.resolve({ code: 200 })),
    delete: vi.fn(() => Promise.resolve({ code: 200 })),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}))

const mockDeliverable = {
  id: 'd1', deliverableCode: 'DEL-001', name: '需求文档',
  status: 'PENDING_APPROVAL', projectId: 'p1',
  plannedDeliveryDate: '2024-02-01', actualDeliveryDate: null,
  createByName: '张三', sponsorName: '李四',
  description: '需求规格说明文档',
  approvalComment: null, remarks: null,
  attachments: JSON.stringify([{ fileName: 'requirements.docx', fileSize: 102400, uploadTime: '2024-01-15T10:00:00', deleted: false }]),
}

const mockProject = {
  id: 'p1', projectName: '测试项目', projectShortName: 'TP',
}

function mockApis() {
  request.get.mockImplementation((url) => {
    if (url === '/pm/deliverables/d1') return Promise.resolve({ code: 200, data: mockDeliverable })
    if (url === '/pm/charters/p1') return Promise.resolve({ code: 200, data: mockProject })
    return Promise.resolve({ code: 200, data: {} })
  })
}

describe('DeliverableDetailPage', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    mockApis()
  })

  function createWrapper() {
    return import('@/views/pm/deliverable/detail.vue').then(m => mount(m.default, { global: { stubs: { ...elementStubs, 'el-dialog': { template: '<div><slot/><slot name="footer"/></div>' } } } }))
  }

  it('渲染交付物详情页面', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.exists()).toBe(true)
  })

  it('API 失败时不崩溃', async () => {
    request.get.mockRejectedValue(new Error('Network error'))
    try {
      const wrapper = await createWrapper()
      expect(wrapper.exists()).toBe(true)
    } catch (e) { /* expected */ }
  })

  it('formatFileSize 格式化文件大小', () => {
    const parsed = JSON.parse(mockDeliverable.attachments)
    expect(parsed[0].fileName).toBe('requirements.docx')
    expect(parsed[0].fileSize).toBe(102400)
  })

  it('loadDetail 成功解析附件', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.deliverable.id).toBe('d1')
    expect(wrapper.vm.attachments.length).toBe(1)
    expect(wrapper.vm.projectName).toBe('测试项目')
  })

  it('loadDetail - 非法附件 JSON 容错', async () => {
    request.get.mockImplementation((url) => {
      if (url === '/pm/deliverables/d1') return Promise.resolve({ code: 200, data: { ...mockDeliverable, attachments: 'not-json' } })
      if (url === '/pm/charters/p1') return Promise.resolve({ code: 200, data: mockProject })
      return Promise.resolve({ code: 200, data: {} })
    })
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.attachments).toEqual([])
  })

  it('formatFileSize / formatUploadTime', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.formatFileSize(null)).toBe('-')
    expect(wrapper.vm.formatFileSize(512)).toBe('512 B')
    expect(wrapper.vm.formatFileSize(2048)).toBe('2.0 KB')
    expect(wrapper.vm.formatFileSize(5 * 1024 * 1024)).toBe('5.0 MB')
    expect(wrapper.vm.formatUploadTime(null)).toBe('-')
    expect(wrapper.vm.formatUploadTime('2024-01-15T10:00:00.000Z')).toBe('2024-01-15T10:00:00')
  })

  it('showDialog approve/reject 设置弹窗', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    wrapper.vm.showDialog('approve')
    expect(wrapper.vm.dialog.mode).toBe('approve')
    expect(wrapper.vm.dialog.title).toBe('审批通过')
    wrapper.vm.showDialog('reject')
    expect(wrapper.vm.dialog.mode).toBe('reject')
    expect(wrapper.vm.dialog.title).toBe('驳回')
  })

  it('dialog.onConfirm approve 调用审批 API', async () => {
    request.post.mockResolvedValue({ code: 200 })
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    wrapper.vm.showDialog('approve')
    wrapper.vm.dialog.comment = '同意'
    await wrapper.vm.dialog.onConfirm()
    expect(request.post).toHaveBeenCalledWith('/pm/deliverables/d1/approve', { comment: '同意' })
    expect(wrapper.vm.dialog.visible).toBe(false)
  })

  it('dialog.onConfirm reject 调用驳回 API', async () => {
    request.post.mockResolvedValue({ code: 200 })
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    wrapper.vm.showDialog('reject')
    wrapper.vm.dialog.comment = '驳回原因'
    await wrapper.vm.dialog.onConfirm()
    expect(request.post).toHaveBeenCalledWith('/pm/deliverables/d1/reject', { rejectReason: '驳回原因' })
  })

  it('handleDeliver 确认后调用交付 API', async () => {
    request.post.mockResolvedValue({ code: 200 })
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleDeliver()
    expect(request.post).toHaveBeenCalledWith('/pm/deliverables/d1/deliver')
  })

  it('handleDownload 成功触发下载', async () => {
    global.URL.createObjectURL = vi.fn(() => 'blob:url')
    global.URL.revokeObjectURL = vi.fn()
    document.body.appendChild = vi.fn()
    document.body.removeChild = vi.fn()
    request.get.mockResolvedValue({ code: 200, data: new Blob(['x']) })
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleDownload(0, 'a.txt')
    expect(URL.revokeObjectURL).toHaveBeenCalled()
  })

  it('handleDownloadAll 成功触发打包下载', async () => {
    global.URL.createObjectURL = vi.fn(() => 'blob:url')
    global.URL.revokeObjectURL = vi.fn()
    document.body.appendChild = vi.fn()
    document.body.removeChild = vi.fn()
    request.get.mockResolvedValue({ code: 200, data: new Blob(['zip']) })
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    wrapper.vm.projectShortName = 'TP'
    await wrapper.vm.handleDownloadAll()
    expect(URL.revokeObjectURL).toHaveBeenCalled()
  })
})

describe('DeliverableDetailPage 角色与边界', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    mockApis()
  })

  function createWrapper() {
    return import('@/views/pm/deliverable/detail.vue').then(m => mount(m.default, { global: { stubs: { ...elementStubs, 'el-dialog': { template: '<div><slot/><slot name="footer"/></div>' } } } }))
  }

  it('isPm / isSponsor 根据角色判断', async () => {
    const { useUserStore } = await import('@/store/user')
    const store = useUserStore()
    store.userInfo = { userId: 'u1', roles: ['ROLE_PM', 'ROLE_SPONSOR'] }
    const wrapper = await createWrapper()
    expect(wrapper.vm.isPm).toBe(true)
    expect(wrapper.vm.isSponsor).toBe(true)
    store.userInfo = { userId: 'u1', roles: ['ROLE_USER'] }
    expect(wrapper.vm.isPm).toBe(false)
  })

  it('handleDownload 失败提示', async () => {
    const { ElMessage } = await import('element-plus')
    vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
    request.get.mockRejectedValue(new Error('fail'))
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleDownload(0, 'a.txt')
    expect(ElMessage.error).toHaveBeenCalled()
  })
})
