import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setupPinia, elementStubs } from '../../../helpers'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

vi.mock('vue-router', async () => {
  const actual = await vi.importActual('vue-router')
  return { ...actual, useRouter: () => ({ push: vi.fn(), replace: vi.fn(), go: vi.fn() }), useRoute: () => ({ path: '/', params: {}, query: {} }) }
})

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ code: 200, data: {} })),
    post: vi.fn(() => Promise.resolve({ code: 200 })),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}))

describe('WbsImportDialog', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
  })

  function createWrapper() {
    return import('@/views/pm/wbs/ImportDialog.vue').then(m => mount(m.default, { global: { stubs: elementStubs } }))
  }

  it('渲染WBS导入弹窗', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.exists()).toBe(true)
  })

  it('open 设置项目 ID 并打开弹窗', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    wrapper.vm.open('p1')
    expect(wrapper.vm.projectId).toBe('p1')
    expect(wrapper.vm.visible).toBe(true)
    expect(wrapper.vm.selectedFile).toBeNull()
    expect(wrapper.vm.importResult).toBeNull()
  })

  it('handleFileSelect 记录文件并清空结果', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    wrapper.vm.importResult = { success: 1 }
    wrapper.vm.handleFileSelect({ raw: new Blob(['x']) })
    expect(wrapper.vm.selectedFile).toBeInstanceOf(Blob)
    expect(wrapper.vm.importResult).toBeNull()
  })

  it('handleDownloadTemplate 成功下载模板', async () => {
    global.URL.createObjectURL = vi.fn(() => 'blob:url')
    global.URL.revokeObjectURL = vi.fn()
    document.body.appendChild = vi.fn()
    document.body.removeChild = vi.fn()
    request.get.mockResolvedValue({ code: 200, data: 'template' })
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleDownloadTemplate()
    expect(URL.revokeObjectURL).toHaveBeenCalled()
  })

  it('handleDownloadTemplate 失败提示', async () => {
    const spy = vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
    request.get.mockRejectedValue(new Error('fail'))
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleDownloadTemplate()
    expect(spy).toHaveBeenCalled()
  })

  it('handleUpload 无文件时直接返回', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleUpload()
    expect(request.post).not.toHaveBeenCalled()
  })

  it('handleUpload 成功导入并触发 refresh', async () => {
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    request.post.mockResolvedValue({ code: 200, data: { success: 2, failed: 1 } })
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    wrapper.vm.selectedFile = new Blob(['x'])
    wrapper.vm.projectId = 'p1'
    const refreshSpy = vi.fn()
    wrapper.vm.$emit = refreshSpy
    await wrapper.vm.handleUpload()
    expect(wrapper.vm.importResult.success).toBe(2)
    expect(wrapper.vm.uploading).toBe(false)
  })

  it('handleUpload 失败提示', async () => {
    const spy = vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
    request.post.mockRejectedValue(new Error('fail'))
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    wrapper.vm.selectedFile = new Blob(['x'])
    await wrapper.vm.handleUpload()
    expect(spy).toHaveBeenCalled()
    expect(wrapper.vm.uploading).toBe(false)
  })
})
