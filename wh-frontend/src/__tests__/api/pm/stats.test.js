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
import { getStatsApi } from '@/api/pm/stats'

describe('stats API', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
  })

  it('getStatsApi 调用 request.get 带 pmId', async () => {
    request.get.mockResolvedValue({ code: 200, data: {} })

    await getStatsApi('123')

    expect(request.get).toHaveBeenCalledWith('/pm/charters/stats', { params: { pmId: '123' } })
  })
})
