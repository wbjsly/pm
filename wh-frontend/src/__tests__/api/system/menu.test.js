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
  getUserMenusApi,
  getMenuListApi,
  getMenuApi,
  createMenuApi,
  updateMenuApi,
  deleteMenuApi,
} from '@/api/system/menu'

describe('menu API', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
  })

  it('getUserMenusApi 调用 request.get', async () => {
    request.get.mockResolvedValue({ code: 200, data: [] })

    await getUserMenusApi()

    expect(request.get).toHaveBeenCalledWith('/system/menus/user')
  })

  it('getMenuListApi 调用 request.get', async () => {
    request.get.mockResolvedValue({ code: 200, data: [] })

    await getMenuListApi()

    expect(request.get).toHaveBeenCalledWith('/system/menus')
  })

  it('getMenuApi 调用 request.get 带 ID', async () => {
    request.get.mockResolvedValue({ code: 200, data: {} })

    await getMenuApi('m1')

    expect(request.get).toHaveBeenCalledWith('/system/menus/m1')
  })

  it('createMenuApi 调用 request.post', async () => {
    request.post.mockResolvedValue({ code: 200 })
    const data = { name: '用户管理', path: '/system/user' }

    await createMenuApi(data)

    expect(request.post).toHaveBeenCalledWith('/system/menus', data)
  })

  it('updateMenuApi 调用 request.put', async () => {
    request.put.mockResolvedValue({ code: 200 })
    const data = { id: 'm1', name: '用户管理更新' }

    await updateMenuApi(data)

    expect(request.put).toHaveBeenCalledWith('/system/menus', data)
  })

  it('deleteMenuApi 调用 request.delete 带 ID', async () => {
    request.delete.mockResolvedValue({ code: 200 })

    await deleteMenuApi('m1')

    expect(request.delete).toHaveBeenCalledWith('/system/menus/m1')
  })
})
