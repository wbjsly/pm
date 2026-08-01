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
  getActualCostListApi,
  createActualCostApi,
  deleteActualCostApi,
  getActualCostAggregationApi,
  getActualCostSumApi,
} from '@/api/pm/actualCost'

describe('actualCost API', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
  })

  it('getActualCostListApi 调用 request.get 带 params', async () => {
    request.get.mockResolvedValue({ code: 200, data: { records: [], total: 0 } })
    const params = { pageNum: 1, pageSize: 10 }

    await getActualCostListApi(params)

    expect(request.get).toHaveBeenCalledWith('/pm/actual-costs', { params })
  })

  it('createActualCostApi 调用 request.post', async () => {
    request.post.mockResolvedValue({ code: 200 })
    const data = { projectId: 'p1', amount: 5000 }

    await createActualCostApi(data)

    expect(request.post).toHaveBeenCalledWith('/pm/actual-costs', data)
  })

  it('deleteActualCostApi 调用 request.delete 带 ID', async () => {
    request.delete.mockResolvedValue({ code: 200 })

    await deleteActualCostApi('c1')

    expect(request.delete).toHaveBeenCalledWith('/pm/actual-costs/c1')
  })

  it('getActualCostAggregationApi 调用 request.get 带 projectId', async () => {
    request.get.mockResolvedValue({ code: 200, data: {} })

    await getActualCostAggregationApi('p1')

    expect(request.get).toHaveBeenCalledWith('/pm/actual-costs/aggregation', { params: { projectId: 'p1' } })
  })

  it('getActualCostSumApi 调用 request.get 带 params', async () => {
    request.get.mockResolvedValue({ code: 200, data: {} })
    const params = { projectId: 'p1' }

    await getActualCostSumApi(params)

    expect(request.get).toHaveBeenCalledWith('/pm/actual-costs/sum', { params })
  })
})
