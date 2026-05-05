import request from '@/utils/request'

export function getWorkHoursApi(year, month) {
  return request.get('/pm/work-hours', { params: { year, month } })
}

export function getPendingWorkHoursApi(year, month) {
  return request.get('/pm/work-hours/pending', { params: { year, month } })
}

export function createWorkHourApi(data) {
  return request.post('/pm/work-hours', data)
}

export function updateWorkHourApi(id, data) {
  return request.put(`/pm/work-hours/${id}`, data)
}

export function deleteWorkHourApi(id) {
  return request.delete(`/pm/work-hours/${id}`)
}

export function resubmitWorkHourApi(id) {
  return request.post(`/pm/work-hours/${id}/resubmit`)
}

export function approveWorkHourApi(id, data) {
  return request.post(`/pm/work-hours/${id}/approve`, data || {})
}

export function rejectWorkHourApi(id, data) {
  return request.post(`/pm/work-hours/${id}/reject`, data)
}

export function batchApproveWorkHoursApi(ids) {
  return request.post('/pm/work-hours/batch-approve', { ids })
}

export function batchRejectWorkHoursApi(ids, reason) {
  return request.post('/pm/work-hours/batch-reject', { ids, reason })
}

export function getWorkHoursStatsApi(year, month) {
  return request.get('/pm/work-hours/stats', { params: { year, month } })
}
