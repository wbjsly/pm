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
  getAllDictsApi,
  getDictItemsApi,
  getDictTypesApi,
  createDictTypeApi,
  updateDictTypeApi,
  deleteDictTypeApi,
  createDictItemApi,
  updateDictItemApi,
  deleteDictItemApi,
} from '@/api/system/dict'

describe('dict API', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
  })

  it('getAllDictsApi 调用 request.get', async () => {
    request.get.mockResolvedValue({ code: 200, data: {} })

    await getAllDictsApi()

    expect(request.get).toHaveBeenCalledWith('/system/dict/all')
  })

  it('getDictItemsApi 调用 request.get 带 typeCode', async () => {
    request.get.mockResolvedValue({ code: 200, data: [] })

    await getDictItemsApi('PROJECT_STATUS')

    expect(request.get).toHaveBeenCalledWith('/system/dict/items/PROJECT_STATUS')
  })

  it('getDictTypesApi 调用 request.get', async () => {
    request.get.mockResolvedValue({ code: 200, data: [] })

    await getDictTypesApi()

    expect(request.get).toHaveBeenCalledWith('/system/dict/types')
  })

  it('createDictTypeApi 调用 request.post', async () => {
    request.post.mockResolvedValue({ code: 200 })
    const data = { typeCode: 'NEW_TYPE', typeName: '新类型' }

    await createDictTypeApi(data)

    expect(request.post).toHaveBeenCalledWith('/system/dict/type', data)
  })

  it('updateDictTypeApi 调用 request.put', async () => {
    request.put.mockResolvedValue({ code: 200 })
    const data = { typeCode: 'EXISTING_TYPE', typeName: '更新名称' }

    await updateDictTypeApi(data)

    expect(request.put).toHaveBeenCalledWith('/system/dict/type', data)
  })

  it('deleteDictTypeApi 调用 request.delete 带 ID', async () => {
    request.delete.mockResolvedValue({ code: 200 })

    await deleteDictTypeApi('dt1')

    expect(request.delete).toHaveBeenCalledWith('/system/dict/type/dt1')
  })

  it('createDictItemApi 调用 request.post', async () => {
    request.post.mockResolvedValue({ code: 200 })
    const data = { typeCode: 'STATUS', itemValue: 'active', label: '启用' }

    await createDictItemApi(data)

    expect(request.post).toHaveBeenCalledWith('/system/dict/item', data)
  })

  it('updateDictItemApi 调用 request.put', async () => {
    request.put.mockResolvedValue({ code: 200 })
    const data = { id: 'di1', label: '禁用' }

    await updateDictItemApi(data)

    expect(request.put).toHaveBeenCalledWith('/system/dict/item', data)
  })

  it('deleteDictItemApi 调用 request.delete 带 ID', async () => {
    request.delete.mockResolvedValue({ code: 200 })

    await deleteDictItemApi('di1')

    expect(request.delete).toHaveBeenCalledWith('/system/dict/item/di1')
  })
})
