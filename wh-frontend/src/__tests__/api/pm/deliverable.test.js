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
  getDeliverableListApi,
  getDeliverableDetailApi,
  createDeliverableApi,
  updateDeliverableApi,
  deleteDeliverableApi,
  submitDeliverableApi,
  approveDeliverableApi,
  rejectDeliverableApi,
  deliverDeliverableApi,
  uploadDeliverableAttachmentApi,
  deleteDeliverableAttachmentApi,
  downloadDeliverableAttachmentApi,
  downloadDeliverableAttachmentsZipApi,
} from '@/api/pm/deliverable'

describe('deliverable API', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
  })

  it('getDeliverableListApi 调用 request.get 带 params', async () => {
    request.get.mockResolvedValue({ code: 200, data: { records: [], total: 0 } })
    const params = { pageNum: 1, pageSize: 10 }

    await getDeliverableListApi(params)

    expect(request.get).toHaveBeenCalledWith('/pm/deliverables', { params })
  })

  it('getDeliverableDetailApi 调用 request.get 带 ID', async () => {
    request.get.mockResolvedValue({ code: 200, data: {} })

    await getDeliverableDetailApi('d1')

    expect(request.get).toHaveBeenCalledWith('/pm/deliverables/d1')
  })

  it('createDeliverableApi 调用 request.post', async () => {
    request.post.mockResolvedValue({ code: 200 })
    const data = { name: '需求文档', wbsId: 'w1' }

    await createDeliverableApi(data)

    expect(request.post).toHaveBeenCalledWith('/pm/deliverables', data)
  })

  it('updateDeliverableApi 调用 request.put 带 ID 和数据', async () => {
    request.put.mockResolvedValue({ code: 200 })
    const data = { name: '更新文档' }

    await updateDeliverableApi('d1', data)

    expect(request.put).toHaveBeenCalledWith('/pm/deliverables/d1', data)
  })

  it('deleteDeliverableApi 调用 request.delete 带 ID', async () => {
    request.delete.mockResolvedValue({ code: 200 })

    await deleteDeliverableApi('d1')

    expect(request.delete).toHaveBeenCalledWith('/pm/deliverables/d1')
  })

  it('submitDeliverableApi 调用 request.post 提交', async () => {
    request.post.mockResolvedValue({ code: 200 })

    await submitDeliverableApi('d1')

    expect(request.post).toHaveBeenCalledWith('/pm/deliverables/d1/submit')
  })

  it('approveDeliverableApi 调用 request.post 审批通过', async () => {
    request.post.mockResolvedValue({ code: 200 })
    const data = { comment: '同意' }

    await approveDeliverableApi('d1', data)

    expect(request.post).toHaveBeenCalledWith('/pm/deliverables/d1/approve', data)
  })

  it('rejectDeliverableApi 调用 request.post 驳回', async () => {
    request.post.mockResolvedValue({ code: 200 })
    const data = { rejectReason: '需要修改' }

    await rejectDeliverableApi('d1', data)

    expect(request.post).toHaveBeenCalledWith('/pm/deliverables/d1/reject', data)
  })

  it('deliverDeliverableApi 调用 request.post 交付', async () => {
    request.post.mockResolvedValue({ code: 200 })

    await deliverDeliverableApi('d1')

    expect(request.post).toHaveBeenCalledWith('/pm/deliverables/d1/deliver')
  })

  it('uploadDeliverableAttachmentApi 调用 request.post 上传附件', async () => {
    request.post.mockResolvedValue({ code: 200 })
    const file = new File(['content'], 'doc.pdf')

    await uploadDeliverableAttachmentApi('d1', file)

    const expectedFormData = new FormData()
    expectedFormData.append('file', file)
    expect(request.post).toHaveBeenCalledWith('/pm/deliverables/d1/attachments', expectedFormData)
  })

  it('deleteDeliverableAttachmentApi 调用 request.delete 删除附件', async () => {
    request.delete.mockResolvedValue({ code: 200 })

    await deleteDeliverableAttachmentApi('d1', 0)

    expect(request.delete).toHaveBeenCalledWith('/pm/deliverables/d1/attachments/0')
  })

  it('downloadDeliverableAttachmentApi 调用 request.get 下载附件', async () => {
    request.get.mockResolvedValue({ code: 200 })

    await downloadDeliverableAttachmentApi('d1', 0)

    expect(request.get).toHaveBeenCalledWith('/pm/deliverables/d1/attachments/0', { responseType: 'blob' })
  })

  it('downloadDeliverableAttachmentsZipApi 调用 request.get 下载附件压缩包', async () => {
    request.get.mockResolvedValue({ code: 200 })

    await downloadDeliverableAttachmentsZipApi('d1')

    expect(request.get).toHaveBeenCalledWith('/pm/deliverables/d1/attachments/zip', { responseType: 'blob' })
  })
})
