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
  getWorkHoursApi,
  getPendingWorkHoursApi,
  createWorkHourApi,
  updateWorkHourApi,
  deleteWorkHourApi,
  resubmitWorkHourApi,
  approveWorkHourApi,
  rejectWorkHourApi,
  batchApproveWorkHoursApi,
  batchRejectWorkHoursApi,
  getWorkHoursByProjectApi,
  getWorkHoursStatsApi,
} from '@/api/pm/workHours'

describe('workHours API', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
  })

  it('getWorkHoursApi 调用 request.get 带年月', async () => {
    request.get.mockResolvedValue({ code: 200, data: [] })

    await getWorkHoursApi(2024, 6)

    expect(request.get).toHaveBeenCalledWith('/pm/work-hours', { params: { year: 2024, month: 6 } })
  })

  it('getPendingWorkHoursApi 调用 request.get 待审批', async () => {
    request.get.mockResolvedValue({ code: 200, data: [] })

    await getPendingWorkHoursApi(2024, 6)

    expect(request.get).toHaveBeenCalledWith('/pm/work-hours/pending', { params: { year: 2024, month: 6 } })
  })

  it('createWorkHourApi 调用 request.post', async () => {
    request.post.mockResolvedValue({ code: 200 })
    const data = { projectId: 'p1', hours: 8 }

    await createWorkHourApi(data)

    expect(request.post).toHaveBeenCalledWith('/pm/work-hours', data)
  })

  it('updateWorkHourApi 调用 request.put 带 ID 和数据', async () => {
    request.put.mockResolvedValue({ code: 200 })
    const data = { hours: 10 }

    await updateWorkHourApi('wh1', data)

    expect(request.put).toHaveBeenCalledWith('/pm/work-hours/wh1', data)
  })

  it('deleteWorkHourApi 调用 request.delete 带 ID', async () => {
    request.delete.mockResolvedValue({ code: 200 })

    await deleteWorkHourApi('wh1')

    expect(request.delete).toHaveBeenCalledWith('/pm/work-hours/wh1')
  })

  it('resubmitWorkHourApi 调用 request.post 重新提交', async () => {
    request.post.mockResolvedValue({ code: 200 })

    await resubmitWorkHourApi('wh1')

    expect(request.post).toHaveBeenCalledWith('/pm/work-hours/wh1/resubmit')
  })

  it('approveWorkHourApi 调用 request.post 审批通过', async () => {
    request.post.mockResolvedValue({ code: 200 })
    const data = { comment: '同意' }

    await approveWorkHourApi('wh1', data)

    expect(request.post).toHaveBeenCalledWith('/pm/work-hours/wh1/approve', data || {})
  })

  it('approveWorkHourApi 不传 data 时默认空对象', async () => {
    request.post.mockResolvedValue({ code: 200 })

    await approveWorkHourApi('wh1')

    expect(request.post).toHaveBeenCalledWith('/pm/work-hours/wh1/approve', {})
  })

  it('rejectWorkHourApi 调用 request.post 驳回', async () => {
    request.post.mockResolvedValue({ code: 200 })
    const data = { rejectReason: '工时超量' }

    await rejectWorkHourApi('wh1', data)

    expect(request.post).toHaveBeenCalledWith('/pm/work-hours/wh1/reject', data)
  })

  it('batchApproveWorkHoursApi 调用 request.post 批量通过', async () => {
    request.post.mockResolvedValue({ code: 200 })
    const ids = ['wh1', 'wh2']

    await batchApproveWorkHoursApi(ids)

    expect(request.post).toHaveBeenCalledWith('/pm/work-hours/batch-approve', { ids })
  })

  it('batchRejectWorkHoursApi 调用 request.post 批量驳回', async () => {
    request.post.mockResolvedValue({ code: 200 })
    const ids = ['wh1', 'wh2']
    const reason = '需要补充说明'

    await batchRejectWorkHoursApi(ids, reason)

    expect(request.post).toHaveBeenCalledWith('/pm/work-hours/batch-reject', { ids, reason })
  })

  it('getWorkHoursByProjectApi 调用 request.get 按项目查询', async () => {
    request.get.mockResolvedValue({ code: 200, data: [] })

    await getWorkHoursByProjectApi('p1', 2024, 6)

    expect(request.get).toHaveBeenCalledWith('/pm/work-hours/by-project', { params: { projectId: 'p1', year: 2024, month: 6 } })
  })

  it('getWorkHoursStatsApi 调用 request.get 获取统计', async () => {
    request.get.mockResolvedValue({ code: 200, data: {} })

    await getWorkHoursStatsApi(2024, 6)

    expect(request.get).toHaveBeenCalledWith('/pm/work-hours/stats', { params: { year: 2024, month: 6 } })
  })
})
