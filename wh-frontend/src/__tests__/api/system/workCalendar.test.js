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
  getWorkCalendarApi,
  getWorkCalendarMonthApi,
  updateWorkCalendarDayApi,
  batchUpdateWorkCalendarApi,
  generateWorkCalendarApi,
  deleteWorkCalendarApi,
  isWorkDayApi,
} from '@/api/system/workCalendar'

describe('workCalendar API', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
  })

  it('getWorkCalendarApi 调用 request.get 带年份', async () => {
    request.get.mockResolvedValue({ code: 200, data: [] })

    await getWorkCalendarApi(2024)

    expect(request.get).toHaveBeenCalledWith('/system/work-calendar', { params: { year: 2024 } })
  })

  it('getWorkCalendarMonthApi 调用 request.get 带年月', async () => {
    request.get.mockResolvedValue({ code: 200, data: [] })

    await getWorkCalendarMonthApi(2024, 6)

    expect(request.get).toHaveBeenCalledWith('/system/work-calendar/month', { params: { year: 2024, month: 6 } })
  })

  it('updateWorkCalendarDayApi 调用 request.post', async () => {
    request.post.mockResolvedValue({ code: 200 })
    const data = { date: '2024-06-01', isWorkDay: false }

    await updateWorkCalendarDayApi(data)

    expect(request.post).toHaveBeenCalledWith('/system/work-calendar', data)
  })

  it('batchUpdateWorkCalendarApi 调用 request.post 批量更新', async () => {
    request.post.mockResolvedValue({ code: 200 })
    const data = { days: ['2024-06-01', '2024-06-02'], isWorkDay: false }

    await batchUpdateWorkCalendarApi(data)

    expect(request.post).toHaveBeenCalledWith('/system/work-calendar/batch', data)
  })

  it('generateWorkCalendarApi 调用 request.post 生成', async () => {
    request.post.mockResolvedValue({ code: 200 })

    await generateWorkCalendarApi(2025)

    expect(request.post).toHaveBeenCalledWith('/system/work-calendar/generate', { year: 2025 })
  })

  it('deleteWorkCalendarApi 调用 request.delete 带 ID', async () => {
    request.delete.mockResolvedValue({ code: 200 })

    await deleteWorkCalendarApi('wc1')

    expect(request.delete).toHaveBeenCalledWith('/system/work-calendar/wc1')
  })

  it('isWorkDayApi 调用 request.get 查询是否工作日', async () => {
    request.get.mockResolvedValue({ code: 200, data: true })

    await isWorkDayApi('2024-06-17')

    expect(request.get).toHaveBeenCalledWith('/system/work-calendar/is-workday', { params: { date: '2024-06-17' } })
  })
})
