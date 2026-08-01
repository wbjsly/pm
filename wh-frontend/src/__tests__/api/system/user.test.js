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
  getUserListApi,
  getUserDetailApi,
  updateUserApi,
  deleteUserApi,
  resetPasswordApi,
  getAllRolesApi,
} from '@/api/system/user'

describe('user API', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
  })

  it('getUserListApi 调用 request.get 带 params', async () => {
    request.get.mockResolvedValue({ code: 200, data: { records: [], total: 0 } })
    const params = { pageNum: 1, pageSize: 10 }

    await getUserListApi(params)

    expect(request.get).toHaveBeenCalledWith('/system/users', { params })
  })

  it('getUserDetailApi 调用 request.get 带 ID', async () => {
    request.get.mockResolvedValue({ code: 200, data: {} })

    await getUserDetailApi('u1')

    expect(request.get).toHaveBeenCalledWith('/system/users/u1')
  })

  it('updateUserApi 调用 request.put 带 ID 和数据', async () => {
    request.put.mockResolvedValue({ code: 200 })
    const data = { nickName: '新昵称' }

    await updateUserApi('u1', data)

    expect(request.put).toHaveBeenCalledWith('/system/users/u1', data)
  })

  it('deleteUserApi 调用 request.delete 带 ID', async () => {
    request.delete.mockResolvedValue({ code: 200 })

    await deleteUserApi('u1')

    expect(request.delete).toHaveBeenCalledWith('/system/users/u1')
  })

  it('resetPasswordApi 调用 request.put 重置密码', async () => {
    request.put.mockResolvedValue({ code: 200 })

    await resetPasswordApi('u1', 'newPass123')

    expect(request.put).toHaveBeenCalledWith('/system/users/u1/password', { password: 'newPass123' })
  })

  it('getAllRolesApi 调用 request.get', async () => {
    request.get.mockResolvedValue({ code: 200, data: [] })

    await getAllRolesApi()

    expect(request.get).toHaveBeenCalledWith('/system/roles')
  })
})
