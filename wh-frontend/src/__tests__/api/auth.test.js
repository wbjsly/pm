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
import { loginApi, logoutApi, getUserInfoApi } from '@/api/auth'

describe('auth API', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
  })

  it('loginApi 调用 request.post', async () => {
    request.post.mockResolvedValue({ code: 200, data: { token: 'abc' } })
    const data = { username: 'admin', password: '123456' }

    await loginApi(data)

    expect(request.post).toHaveBeenCalledWith('/auth/login', data)
  })

  it('logoutApi 调用 request.post', async () => {
    request.post.mockResolvedValue({ code: 200 })

    await logoutApi()

    expect(request.post).toHaveBeenCalledWith('/auth/logout')
  })

  it('getUserInfoApi 调用 request.get', async () => {
    request.get.mockResolvedValue({ code: 200, data: { name: 'Admin' } })

    await getUserInfoApi()

    expect(request.get).toHaveBeenCalledWith('/auth/info')
  })
})
