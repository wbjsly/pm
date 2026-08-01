import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setupPinia } from '../../helpers'

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}))

import request from '@/utils/request'
import {
  getWbsListApi,
  getWbsDetailApi,
  createWbsApi,
  updateWbsApi,
  deleteWbsApi,
  suspendWbsApi,
  resumeWbsApi,
  reopenWbsApi,
  cancelWbsApi,
  completeWbsApi,
  startWbsApi,
  testWbsApi,
  getWbsVersionsApi,
  importWbsApi,
  exportWbsApi,
  downloadWbsTemplateApi,
  getProjectsForWbsApi,
  getProductListApi,
  getAllProductsApi,
  createProductApi,
  updateProductApi,
  deleteProductApi,
  getModuleListApi,
  getModulesByProductApi,
  createModuleApi,
  updateModuleApi,
  deleteModuleApi,
} from '@/api/pm/wbs'

describe('wbs API', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
  })

  it('getWbsListApi 调用 request.get 带 params', async () => {
    request.get.mockResolvedValue({ code: 200, data: { records: [], total: 0 } })
    const params = { pageNum: 1, pageSize: 10 }

    await getWbsListApi(params)

    expect(request.get).toHaveBeenCalledWith('/pm/wbs', { params })
  })

  it('getWbsDetailApi 调用 request.get 带 ID', async () => {
    request.get.mockResolvedValue({ code: 200, data: {} })

    await getWbsDetailApi('w1')

    expect(request.get).toHaveBeenCalledWith('/pm/wbs/w1')
  })

  it('createWbsApi 调用 request.post', async () => {
    request.post.mockResolvedValue({ code: 200 })
    const data = { name: '设计任务', projectId: 'p1' }

    await createWbsApi(data)

    expect(request.post).toHaveBeenCalledWith('/pm/wbs', data)
  })

  it('updateWbsApi 调用 request.put 带 ID 和数据', async () => {
    request.put.mockResolvedValue({ code: 200 })
    const data = { name: '更新任务' }

    await updateWbsApi('w1', data)

    expect(request.put).toHaveBeenCalledWith('/pm/wbs/w1', data)
  })

  it('deleteWbsApi 调用 request.delete 带 ID', async () => {
    request.delete.mockResolvedValue({ code: 200 })

    await deleteWbsApi('w1')

    expect(request.delete).toHaveBeenCalledWith('/pm/wbs/w1')
  })

  it('suspendWbsApi 调用 request.post 暂停', async () => {
    request.post.mockResolvedValue({ code: 200 })

    await suspendWbsApi('w1')

    expect(request.post).toHaveBeenCalledWith('/pm/wbs/w1/suspend')
  })

  it('resumeWbsApi 调用 request.post 恢复', async () => {
    request.post.mockResolvedValue({ code: 200 })

    await resumeWbsApi('w1')

    expect(request.post).toHaveBeenCalledWith('/pm/wbs/w1/resume')
  })

  it('reopenWbsApi 调用 request.post 重新打开', async () => {
    request.post.mockResolvedValue({ code: 200 })

    await reopenWbsApi('w1')

    expect(request.post).toHaveBeenCalledWith('/pm/wbs/w1/reopen')
  })

  it('cancelWbsApi 调用 request.post 取消', async () => {
    request.post.mockResolvedValue({ code: 200 })

    await cancelWbsApi('w1')

    expect(request.post).toHaveBeenCalledWith('/pm/wbs/w1/cancel')
  })

  it('completeWbsApi 调用 request.post 完成', async () => {
    request.post.mockResolvedValue({ code: 200 })

    await completeWbsApi('w1')

    expect(request.post).toHaveBeenCalledWith('/pm/wbs/w1/complete')
  })

  it('startWbsApi 调用 request.post 开始', async () => {
    request.post.mockResolvedValue({ code: 200 })

    await startWbsApi('w1')

    expect(request.post).toHaveBeenCalledWith('/pm/wbs/w1/start')
  })

  it('testWbsApi 调用 request.post 测试', async () => {
    request.post.mockResolvedValue({ code: 200 })

    await testWbsApi('w1')

    expect(request.post).toHaveBeenCalledWith('/pm/wbs/w1/test')
  })

  it('getWbsVersionsApi 调用 request.get 获取版本列表', async () => {
    request.get.mockResolvedValue({ code: 200, data: [] })

    await getWbsVersionsApi('w1')

    expect(request.get).toHaveBeenCalledWith('/pm/wbs/w1/versions')
  })

  it('importWbsApi 调用 request.post 上传文件', async () => {
    request.post.mockResolvedValue({ code: 200 })
    const file = new File(['data'], 'wbs.xlsx')

    await importWbsApi(file, 'p1')

    const expectedFormData = new FormData()
    expectedFormData.append('file', file)
    expect(request.post).toHaveBeenCalledWith('/pm/wbs/import', expectedFormData, {
      params: { projectId: 'p1' }
    })
  })

  it('exportWbsApi 调用 request.get 导出', async () => {
    request.get.mockResolvedValue({ code: 200 })

    await exportWbsApi('p1')

    expect(request.get).toHaveBeenCalledWith('/pm/wbs/export', {
      params: { projectId: 'p1' },
      responseType: 'blob'
    })
  })

  it('downloadWbsTemplateApi 调用 request.get 下载模板', async () => {
    request.get.mockResolvedValue({ code: 200 })

    await downloadWbsTemplateApi()

    expect(request.get).toHaveBeenCalledWith('/pm/wbs/template', {
      responseType: 'blob'
    })
  })

  it('getProjectsForWbsApi 调用 request.get 获取项目列表', async () => {
    request.get.mockResolvedValue({ code: 200, data: { records: [], total: 0 } })
    const params = { pageNum: 1, pageSize: 10 }

    await getProjectsForWbsApi(params)

    expect(request.get).toHaveBeenCalledWith('/pm/charters', { params })
  })

  it('getProductListApi 调用 request.get 带 params', async () => {
    request.get.mockResolvedValue({ code: 200, data: { records: [], total: 0 } })
    const params = { pageNum: 1, pageSize: 10 }

    await getProductListApi(params)

    expect(request.get).toHaveBeenCalledWith('/pm/products', { params })
  })

  it('getAllProductsApi 调用 request.get', async () => {
    request.get.mockResolvedValue({ code: 200, data: [] })

    await getAllProductsApi()

    expect(request.get).toHaveBeenCalledWith('/pm/products/all')
  })

  it('createProductApi 调用 request.post', async () => {
    request.post.mockResolvedValue({ code: 200 })
    const data = { name: '新产品' }

    await createProductApi(data)

    expect(request.post).toHaveBeenCalledWith('/pm/products', data)
  })

  it('updateProductApi 调用 request.put 带 ID 和数据', async () => {
    request.put.mockResolvedValue({ code: 200 })
    const data = { name: '更新产品' }

    await updateProductApi('p1', data)

    expect(request.put).toHaveBeenCalledWith('/pm/products/p1', data)
  })

  it('deleteProductApi 调用 request.delete 带 ID', async () => {
    request.delete.mockResolvedValue({ code: 200 })

    await deleteProductApi('p1')

    expect(request.delete).toHaveBeenCalledWith('/pm/products/p1')
  })

  it('getModuleListApi 调用 request.get 带 params', async () => {
    request.get.mockResolvedValue({ code: 200, data: { records: [], total: 0 } })
    const params = { pageNum: 1, pageSize: 10 }

    await getModuleListApi(params)

    expect(request.get).toHaveBeenCalledWith('/pm/modules', { params })
  })

  it('getModulesByProductApi 调用 request.get 按产品查询', async () => {
    request.get.mockResolvedValue({ code: 200, data: [] })

    await getModulesByProductApi('p1')

    expect(request.get).toHaveBeenCalledWith('/pm/modules/by-product/p1')
  })

  it('createModuleApi 调用 request.post', async () => {
    request.post.mockResolvedValue({ code: 200 })
    const data = { name: '新模块' }

    await createModuleApi(data)

    expect(request.post).toHaveBeenCalledWith('/pm/modules', data)
  })

  it('updateModuleApi 调用 request.put 带 ID 和数据', async () => {
    request.put.mockResolvedValue({ code: 200 })
    const data = { name: '更新模块' }

    await updateModuleApi('m1', data)

    expect(request.put).toHaveBeenCalledWith('/pm/modules/m1', data)
  })

  it('deleteModuleApi 调用 request.delete 带 ID', async () => {
    request.delete.mockResolvedValue({ code: 200 })

    await deleteModuleApi('m1')

    expect(request.delete).toHaveBeenCalledWith('/pm/modules/m1')
  })
})
