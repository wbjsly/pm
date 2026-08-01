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
  getCostWarningListApi,
  closeCostWarningApi,
  triggerCostWarningApi,
  getActiveCostWarningsApi,
} from '@/api/pm/costWarning'

describe('costWarning API', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
  })

  it('getCostWarningListApi 调用 request.get 带 params', async () => {
    request.get.mockResolvedValue({ code: 200, data: { records: [], total: 0 } })
    const params = { pageNum: 1, pageSize: 10 }

    await getCostWarningListApi(params)

    expect(request.get).toHaveBeenCalledWith('/pm/cost-warnings', { params })
  })

  it('closeCostWarningApi 调用 request.post 关闭', async () => {
    request.post.mockResolvedValue({ code: 200 })

    await closeCostWarningApi('w1')

    expect(request.post).toHaveBeenCalledWith('/pm/cost-warnings/w1/close')
  })

  it('triggerCostWarningApi 调用 request.post 触发', async () => {
    request.post.mockResolvedValue({ code: 200 })

    await triggerCostWarningApi()

    expect(request.post).toHaveBeenCalledWith('/pm/cost-warnings/trigger')
  })

  it('getActiveCostWarningsApi 调用 request.get 带默认 limit', async () => {
    request.get.mockResolvedValue({ code: 200, data: [] })

    await getActiveCostWarningsApi()

    expect(request.get).toHaveBeenCalledWith('/pm/cost-warnings/active', { params: { limit: 5 } })
  })

  it('getActiveCostWarningsApi 调用 request.get 带自定义 limit', async () => {
    request.get.mockResolvedValue({ code: 200, data: [] })

    await getActiveCostWarningsApi(10)

    expect(request.get).toHaveBeenCalledWith('/pm/cost-warnings/active', { params: { limit: 10 } })
  })
})
