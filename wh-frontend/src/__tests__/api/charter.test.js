import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setupPinia } from '../helpers'

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
  getCharterListApi,
  createCharterApi,
  getCharterDetailApi,
  updateCharterApi,
  deleteCharterApi,
  submitCharterApi,
  approveCharterApi,
  rejectCharterApi,
} from '@/api/pm/charter'

describe('charter API', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
  })

  it('getCharterListApi 调用 request.get', async () => {
    request.get.mockResolvedValue({ code: 200, data: { records: [], total: 0 } })
    const params = { pageNum: 1, pageSize: 10, status: 'DRAFT' }

    await getCharterListApi(params)

    expect(request.get).toHaveBeenCalledWith('/pm/charters', { params })
  })

  it('createCharterApi 调用 request.post', async () => {
    request.post.mockResolvedValue({ code: 200 })
    const data = { projectName: '新项目', projectCode: 'P2024001' }

    await createCharterApi(data)

    expect(request.post).toHaveBeenCalledWith('/pm/charters', data)
  })

  it('getCharterDetailApi 调用 request.get 带 ID', async () => {
    request.get.mockResolvedValue({ code: 200, data: {} })

    await getCharterDetailApi('c1')

    expect(request.get).toHaveBeenCalledWith('/pm/charters/c1')
  })

  it('updateCharterApi 调用 request.put 带 ID 和数据', async () => {
    request.put.mockResolvedValue({ code: 200 })
    const data = { projectName: '更新项目' }

    await updateCharterApi('c1', data)

    expect(request.put).toHaveBeenCalledWith('/pm/charters/c1', data)
  })

  it('deleteCharterApi 调用 request.delete 带 ID', async () => {
    request.delete.mockResolvedValue({ code: 200 })

    await deleteCharterApi('c1')

    expect(request.delete).toHaveBeenCalledWith('/pm/charters/c1')
  })

  it('submitCharterApi 调用 request.post 提交审批', async () => {
    request.post.mockResolvedValue({ code: 200 })

    await submitCharterApi('c1')

    expect(request.post).toHaveBeenCalledWith('/pm/charters/c1/submit')
  })

  it('approveCharterApi 调用 request.post 审批通过', async () => {
    request.post.mockResolvedValue({ code: 200 })
    const data = { rejectReason: '同意' }

    await approveCharterApi('c1', data)

    expect(request.post).toHaveBeenCalledWith('/pm/charters/c1/approve', data)
  })

  it('rejectCharterApi 调用 request.post 驳回', async () => {
    request.post.mockResolvedValue({ code: 200 })
    const data = { rejectReason: '缺少资料' }

    await rejectCharterApi('c1', data)

    expect(request.post).toHaveBeenCalledWith('/pm/charters/c1/reject', data)
  })
})
