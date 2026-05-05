import request from '@/utils/request'

export function getUserListApi(params) {
  return request.get('/system/users', { params })
}

export function getUserDetailApi(id) {
  return request.get(`/system/users/${id}`)
}

export function updateUserApi(id, data) {
  return request.put(`/system/users/${id}`, data)
}

export function deleteUserApi(id) {
  return request.delete(`/system/users/${id}`)
}

export function resetPasswordApi(id, password) {
  return request.put(`/system/users/${id}/password`, { password })
}

export function getAllRolesApi() {
  return request.get('/system/roles')
}
