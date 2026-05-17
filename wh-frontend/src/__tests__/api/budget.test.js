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
  getBudgetListApi,
  getProjectBudgetsApi,
  getBudgetDetailApi,
  getBudgetDetailWithItemsApi,
  createBudgetApi,
  updateBudgetApi,
  deleteBudgetApi,
  upgradeBudgetApi,
  submitBudgetApi,
  approveBudgetApi,
  rejectBudgetApi,
  getBudgetComparisonApi,
  getBudgetVersionsApi,
} from '@/api/pm/budget'

describe('budget API', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
  })

  it('getBudgetListApi 调用 request.get', async () => {
    request.get.mockResolvedValue({ code: 200, data: { records: [], total: 0 } })
    const params = { pageNum: 1, pageSize: 10 }

    await getBudgetListApi(params)

    expect(request.get).toHaveBeenCalledWith('/pm/budgets', { params })
  })

  it('getProjectBudgetsApi 调用 request.get', async () => {
    request.get.mockResolvedValue({ code: 200, data: { records: [], total: 0 } })
    const params = { pageNum: 1, pageSize: 10, projectId: 'p1' }

    await getProjectBudgetsApi(params)

    expect(request.get).toHaveBeenCalledWith('/pm/budgets/projects', { params })
  })

  it('getBudgetDetailApi 调用 request.get 带 ID', async () => {
    request.get.mockResolvedValue({ code: 200, data: {} })

    await getBudgetDetailApi('b1')

    expect(request.get).toHaveBeenCalledWith('/pm/budgets/b1')
  })

  it('getBudgetDetailWithItemsApi 调用 request.get 带 /detail', async () => {
    request.get.mockResolvedValue({ code: 200, data: {} })

    await getBudgetDetailWithItemsApi('b1')

    expect(request.get).toHaveBeenCalledWith('/pm/budgets/b1/detail')
  })

  it('createBudgetApi 调用 request.post', async () => {
    request.post.mockResolvedValue({ code: 200 })
    const data = { projectId: 'p1', totalBudget: 100000 }

    await createBudgetApi(data)

    expect(request.post).toHaveBeenCalledWith('/pm/budgets', data)
  })

  it('updateBudgetApi 调用 request.put', async () => {
    request.put.mockResolvedValue({ code: 200 })
    const data = { totalBudget: 120000 }

    await updateBudgetApi('b1', data)

    expect(request.put).toHaveBeenCalledWith('/pm/budgets/b1', data)
  })

  it('deleteBudgetApi 调用 request.delete', async () => {
    request.delete.mockResolvedValue({ code: 200 })

    await deleteBudgetApi('b1')

    expect(request.delete).toHaveBeenCalledWith('/pm/budgets/b1')
  })

  it('upgradeBudgetApi 调用 request.post 升级', async () => {
    request.post.mockResolvedValue({ code: 200 })
    const data = { reason: '预算调整' }

    await upgradeBudgetApi('b1', data)

    expect(request.post).toHaveBeenCalledWith('/pm/budgets/b1/upgrade', data)
  })

  it('submitBudgetApi 调用 request.post 提交审批', async () => {
    request.post.mockResolvedValue({ code: 200 })

    await submitBudgetApi('b1')

    expect(request.post).toHaveBeenCalledWith('/pm/budgets/b1/submit')
  })

  it('approveBudgetApi 调用 request.post 审批通过', async () => {
    request.post.mockResolvedValue({ code: 200 })
    const data = { comment: '同意' }

    await approveBudgetApi('b1', data)

    expect(request.post).toHaveBeenCalledWith('/pm/budgets/b1/approve', data)
  })

  it('rejectBudgetApi 调用 request.post 驳回', async () => {
    request.post.mockResolvedValue({ code: 200 })
    const data = { rejectReason: '预算不合理' }

    await rejectBudgetApi('b1', data)

    expect(request.post).toHaveBeenCalledWith('/pm/budgets/b1/reject', data)
  })

  it('getBudgetComparisonApi 调用 request.get 比较', async () => {
    request.get.mockResolvedValue({ code: 200, data: {} })
    const params = { startDate: '2024-01-01' }

    await getBudgetComparisonApi('b1', params)

    expect(request.get).toHaveBeenCalledWith('/pm/budgets/b1/comparison', { params })
  })

  it('getBudgetVersionsApi 调用 request.get 获取版本列表', async () => {
    request.get.mockResolvedValue({ code: 200, data: [] })

    await getBudgetVersionsApi('p1')

    expect(request.get).toHaveBeenCalledWith('/pm/budgets/versions/p1')
  })
})
