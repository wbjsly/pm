import request from '@/utils/request'

export function getCharterListApi(params) {
  return request.get('/pm/charters', { params })
}

export function createCharterApi(data) {
  return request.post('/pm/charters', data)
}

export function getCharterDetailApi(id) {
  return request.get(`/pm/charters/${id}`)
}

export function updateCharterApi(id, data) {
  return request.put(`/pm/charters/${id}`, data)
}

export function deleteCharterApi(id) {
  return request.delete(`/pm/charters/${id}`)
}

export function submitCharterApi(id) {
  return request.post(`/pm/charters/${id}/submit`)
}

export function approveCharterApi(id, data) {
  return request.post(`/pm/charters/${id}/approve`, data)
}

export function rejectCharterApi(id, data) {
  return request.post(`/pm/charters/${id}/reject`, data)
}
