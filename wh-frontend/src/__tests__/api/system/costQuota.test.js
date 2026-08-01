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
  getPositionListApi,
  createPositionApi,
  updatePositionApi,
  deletePositionApi,
  getYearListApi,
  createYearApi,
  updateYearApi,
  deleteYearApi,
  getDefaultStartDateApi,
  getCurrentRateApi,
  getQuotaListApi,
  adjustQuotaApi,
  getQuotaHistoryApi,
  compareVersionsApi,
} from '@/api/system/costQuota'

describe('costQuota API', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
  })

  it('getPositionListApi 调用 request.get', async () => {
    request.get.mockResolvedValue({ code: 200, data: [] })

    await getPositionListApi()

    expect(request.get).toHaveBeenCalledWith('/system/cost-quota/positions')
  })

  it('createPositionApi 调用 request.post', async () => {
    request.post.mockResolvedValue({ code: 200 })
    const data = { name: '高级工程师' }

    await createPositionApi(data)

    expect(request.post).toHaveBeenCalledWith('/system/cost-quota/positions', data)
  })

  it('updatePositionApi 调用 request.put 带 ID 和数据', async () => {
    request.put.mockResolvedValue({ code: 200 })
    const data = { name: '资深工程师' }

    await updatePositionApi('pos1', data)

    expect(request.put).toHaveBeenCalledWith('/system/cost-quota/positions/pos1', data)
  })

  it('deletePositionApi 调用 request.delete 带 ID', async () => {
    request.delete.mockResolvedValue({ code: 200 })

    await deletePositionApi('pos1')

    expect(request.delete).toHaveBeenCalledWith('/system/cost-quota/positions/pos1')
  })

  it('getYearListApi 调用 request.get', async () => {
    request.get.mockResolvedValue({ code: 200, data: [] })

    await getYearListApi()

    expect(request.get).toHaveBeenCalledWith('/system/cost-quota/years')
  })

  it('createYearApi 调用 request.post', async () => {
    request.post.mockResolvedValue({ code: 200 })
    const data = { year: 2024 }

    await createYearApi(data)

    expect(request.post).toHaveBeenCalledWith('/system/cost-quota/years', data)
  })

  it('updateYearApi 调用 request.put 带 ID 和数据', async () => {
    request.put.mockResolvedValue({ code: 200 })
    const data = { year: 2025 }

    await updateYearApi('y1', data)

    expect(request.put).toHaveBeenCalledWith('/system/cost-quota/years/y1', data)
  })

  it('deleteYearApi 调用 request.delete 带 ID', async () => {
    request.delete.mockResolvedValue({ code: 200 })

    await deleteYearApi('y1')

    expect(request.delete).toHaveBeenCalledWith('/system/cost-quota/years/y1')
  })

  it('getDefaultStartDateApi 调用 request.get', async () => {
    request.get.mockResolvedValue({ code: 200, data: '2024-01-01' })

    await getDefaultStartDateApi()

    expect(request.get).toHaveBeenCalledWith('/system/cost-quota/years/latest/default-start-date')
  })

  it('getCurrentRateApi 调用 request.get 带 positionId', async () => {
    request.get.mockResolvedValue({ code: 200, data: {} })

    await getCurrentRateApi('pos1')

    expect(request.get).toHaveBeenCalledWith('/system/cost-quota/quotas/current-rate', { params: { positionId: 'pos1' } })
  })

  it('getQuotaListApi 调用 request.get 带 yearId', async () => {
    request.get.mockResolvedValue({ code: 200, data: [] })

    await getQuotaListApi('y1')

    expect(request.get).toHaveBeenCalledWith('/system/cost-quota/quotas', { params: { yearId: 'y1' } })
  })

  it('adjustQuotaApi 调用 request.post', async () => {
    request.post.mockResolvedValue({ code: 200 })
    const data = { positionId: 'pos1', yearId: 'y1', rate: 500 }

    await adjustQuotaApi(data)

    expect(request.post).toHaveBeenCalledWith('/system/cost-quota/quotas/adjust', data)
  })

  it('getQuotaHistoryApi 调用 request.get 带 positionId 和 yearId', async () => {
    request.get.mockResolvedValue({ code: 200, data: [] })

    await getQuotaHistoryApi('pos1', 'y1')

    expect(request.get).toHaveBeenCalledWith('/system/cost-quota/quotas/history', { params: { positionId: 'pos1', yearId: 'y1' } })
  })

  it('compareVersionsApi 调用 request.get 带两个配额 ID', async () => {
    request.get.mockResolvedValue({ code: 200, data: {} })

    await compareVersionsApi('q1', 'q2')

    expect(request.get).toHaveBeenCalledWith('/system/cost-quota/quotas/compare', { params: { quotaId1: 'q1', quotaId2: 'q2' } })
  })
})
