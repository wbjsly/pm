import { describe, it, expect, vi, beforeAll, beforeEach } from 'vitest'
import { setupPinia } from '../helpers'

const mockRequestUse = vi.fn()
const mockResponseUse = vi.fn()

let mockAxiosInstance

vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => {
      mockAxiosInstance = {
        interceptors: {
          request: { use: mockRequestUse },
          response: { use: mockResponseUse },
        },
        get: vi.fn(),
        post: vi.fn(),
        put: vi.fn(),
        delete: vi.fn(),
      }
      return mockAxiosInstance
    }),
  },
}))

const mockRouterPush = vi.fn()
vi.mock('@/router', () => ({
  default: { push: mockRouterPush },
}))

vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn(), success: vi.fn(), warning: vi.fn(), info: vi.fn() },
}))

let requestFulfilled, requestRejected, responseFulfilled, responseRejected

describe('request interceptor', () => {
  beforeAll(async () => {
    // Import triggers module evaluation which sets up interceptors
    await import('@/utils/request')
    // Capture handler references before any beforeEach clears them
    requestFulfilled = mockRequestUse.mock.calls[0][0]
    requestRejected = mockRequestUse.mock.calls[0][1]
    responseFulfilled = mockResponseUse.mock.calls[0][0]
    responseRejected = mockResponseUse.mock.calls[0][1]
  })

  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    // The captured handlers are regular function closures — they still work after clearAllMocks
  })

  it('创建时注册请求拦截器和响应拦截器', () => {
    expect(requestFulfilled).toBeDefined()
    expect(requestRejected).toBeDefined()
    expect(responseFulfilled).toBeDefined()
    expect(responseRejected).toBeDefined()
  })

  it('请求拦截器：有 token 时注入 Bearer Authorization 头', () => {
    localStorage.setItem('token', 'my-jwt-token')
    const config = { headers: {}, url: '/api/pm/charters' }

    const result = requestFulfilled(config)

    expect(result.headers['Authorization']).toBe('Bearer my-jwt-token')
  })

  it('请求拦截器：无 token 时不添加 Authorization 头', () => {
    const config = { headers: {}, url: '/api/auth/login' }

    const result = requestFulfilled(config)

    expect(result.headers['Authorization']).toBeUndefined()
  })

  it('请求拦截器错误处理：直接 reject', async () => {
    const error = new Error('Request failed')

    await expect(requestRejected(error)).rejects.toThrow('Request failed')
  })

  it('响应拦截器：code 200 时返回 data', () => {
    const response = {
      data: { code: 200, data: { id: '1', name: 'test' }, message: '成功' },
    }

    const result = responseFulfilled(response)

    expect(result).toEqual(response.data)
  })

  it('响应拦截器：code 非 200 时显示错误并 reject', async () => {
    const { ElMessage } = await import('element-plus')
    const response = {
      data: { code: 500, message: '服务器错误' },
    }

    await expect(responseFulfilled(response)).rejects.toThrow('服务器错误')
    expect(ElMessage.error).toHaveBeenCalledWith('服务器错误')
  })

  it('响应拦截器：401 时移除 token 并跳转登录页', async () => {
    localStorage.setItem('token', 'expired-token')
    const { ElMessage } = await import('element-plus')
    const response = {
      data: { code: 401, message: '未授权' },
    }

    await expect(responseFulfilled(response)).rejects.toThrow('未授权')
    expect(localStorage.getItem('token')).toBeNull()
    expect(mockRouterPush).toHaveBeenCalledWith('/login')
    expect(ElMessage.error).toHaveBeenCalledWith('未授权')
  })

  it('响应拦截错误处理：HTTP 401 移除 token 跳转登录', async () => {
    localStorage.setItem('token', 'expired-token')
    const error = {
      response: { status: 401, data: { message: '未授权' } },
    }

    await expect(responseRejected(error)).rejects.toEqual(error)
    expect(localStorage.getItem('token')).toBeNull()
    expect(mockRouterPush).toHaveBeenCalledWith('/login')
  })

  it('响应拦截错误处理：HTTP 403 移除 token 跳转登录', async () => {
    localStorage.setItem('token', 'some-token')
    const error = {
      response: { status: 403, data: { message: '禁止访问' } },
    }

    await expect(responseRejected(error)).rejects.toEqual(error)
    expect(localStorage.getItem('token')).toBeNull()
    expect(mockRouterPush).toHaveBeenCalledWith('/login')
  })

  it('响应拦截错误处理：非 401/403 显示错误消息', async () => {
    const { ElMessage } = await import('element-plus')
    const error = {
      response: { status: 400, data: { message: '参数错误' } },
    }

    await expect(responseRejected(error)).rejects.toEqual(error)
    expect(ElMessage.error).toHaveBeenCalledWith('参数错误')
  })

  it('响应拦截错误处理：无响应时显示网络异常', async () => {
    const { ElMessage } = await import('element-plus')
    const error = { message: 'Network Error' }

    await expect(responseRejected(error)).rejects.toEqual(error)
    expect(ElMessage.error).toHaveBeenCalledWith('网络异常')
  })
})
