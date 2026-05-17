import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setupPinia } from '../helpers'

vi.mock('@/api/auth', () => ({
  loginApi: vi.fn(),
  logoutApi: vi.fn(),
  getUserInfoApi: vi.fn(),
}))

import { useUserStore } from '@/store/user'
import { loginApi, logoutApi, getUserInfoApi } from '@/api/auth'

describe('useUserStore', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
  })

  it('登录成功后存储 token 到 state 和 localStorage', async () => {
    loginApi.mockResolvedValue({ code: 200, data: { token: 'test-token-123' } })
    const store = useUserStore()

    await store.login('admin', 'password')

    expect(store.token).toBe('test-token-123')
    expect(localStorage.getItem('token')).toBe('test-token-123')
  })

  it('登录失败时不存储 token', async () => {
    loginApi.mockRejectedValue(new Error('登录失败'))
    const store = useUserStore()

    await expect(store.login('admin', 'wrong')).rejects.toThrow('登录失败')

    expect(store.token).toBe('')
    expect(localStorage.getItem('token')).toBeNull()
  })

  it('getUserInfo 填充 userInfo 和 permissions', async () => {
    const mockUserInfo = {
      id: 'u1',
      username: 'admin',
      nickname: '管理员',
      permissions: ['pm:budget:view', 'pm:charter:edit'],
    }
    getUserInfoApi.mockResolvedValue({ code: 200, data: mockUserInfo })
    const store = useUserStore()

    await store.getUserInfo()

    expect(store.userInfo).toEqual(mockUserInfo)
    expect(store.permissions).toEqual(['pm:budget:view', 'pm:charter:edit'])
  })

  it('getUserInfo 无权限数据时默认为空数组', async () => {
    getUserInfoApi.mockResolvedValue({ code: 200, data: { id: 'u1', username: 'admin', nickname: '管理员' } })
    const store = useUserStore()

    await store.getUserInfo()

    expect(store.permissions).toEqual([])
  })

  it('logout 清除所有状态并移除 token', async () => {
    logoutApi.mockResolvedValue({ code: 200 })
    localStorage.setItem('token', 'test-token')
    const store = useUserStore()
    store.token = 'test-token'
    store.userInfo = { id: 'u1', username: 'admin' }
    store.permissions = ['pm:budget:view']

    await store.logout()

    expect(store.token).toBe('')
    expect(store.userInfo).toBeNull()
    expect(store.permissions).toEqual([])
    expect(localStorage.getItem('token')).toBeNull()
  })

  it('logout 时如果 logoutApi 失败仍能清除本地状态', async () => {
    logoutApi.mockRejectedValue(new Error('Logout API error'))
    localStorage.setItem('token', 'test-token')
    const store = useUserStore()
    store.token = 'test-token'
    store.userInfo = { id: 'u1', username: 'admin' }

    await store.logout()

    expect(store.token).toBe('')
    expect(store.userInfo).toBeNull()
    expect(localStorage.getItem('token')).toBeNull()
  })
})
