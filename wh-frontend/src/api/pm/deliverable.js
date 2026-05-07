import request from '@/utils/request'

export function getDeliverableListApi(params) {
  return request.get('/pm/deliverables', { params })
}

export function getDeliverableDetailApi(id) {
  return request.get(`/pm/deliverables/${id}`)
}

export function createDeliverableApi(data) {
  return request.post('/pm/deliverables', data)
}

export function updateDeliverableApi(id, data) {
  return request.put(`/pm/deliverables/${id}`, data)
}

export function deleteDeliverableApi(id) {
  return request.delete(`/pm/deliverables/${id}`)
}

export function submitDeliverableApi(id) {
  return request.post(`/pm/deliverables/${id}/submit`)
}

export function approveDeliverableApi(id, data) {
  return request.post(`/pm/deliverables/${id}/approve`, data)
}

export function rejectDeliverableApi(id, data) {
  return request.post(`/pm/deliverables/${id}/reject`, data)
}

export function deliverDeliverableApi(id) {
  return request.post(`/pm/deliverables/${id}/deliver`)
}

export function uploadDeliverableAttachmentApi(id, file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post(`/pm/deliverables/${id}/attachments`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function deleteDeliverableAttachmentApi(id, index) {
  return request.delete(`/pm/deliverables/${id}/attachments/${index}`)
}

export function getDeliverableAttachmentUrlApi(id, index) {
  return request.get(`/pm/deliverables/${id}/attachments/${index}`)
}
