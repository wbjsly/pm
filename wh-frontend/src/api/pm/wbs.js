import request from '@/utils/request'

export function getWbsListApi(params) {
  return request.get('/pm/wbs', { params })
}

export function getWbsDetailApi(id) {
  return request.get(`/pm/wbs/${id}`)
}

export function createWbsApi(data) {
  return request.post('/pm/wbs', data)
}

export function updateWbsApi(id, data) {
  return request.put(`/pm/wbs/${id}`, data)
}

export function deleteWbsApi(id) {
  return request.delete(`/pm/wbs/${id}`)
}

export function suspendWbsApi(id) {
  return request.post(`/pm/wbs/${id}/suspend`)
}

export function resumeWbsApi(id) {
  return request.post(`/pm/wbs/${id}/resume`)
}

export function reopenWbsApi(id) {
  return request.post(`/pm/wbs/${id}/reopen`)
}

export function getWbsVersionsApi(id) {
  return request.get(`/pm/wbs/${id}/versions`)
}

export function importWbsApi(file, projectId) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/pm/wbs/import', formData, {
    params: { projectId },
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function exportWbsApi(projectId) {
  return request.get('/pm/wbs/export', {
    params: { projectId },
    responseType: 'blob'
  })
}

export function downloadWbsTemplateApi() {
  return request.get('/pm/wbs/template', {
    responseType: 'blob'
  })
}

export function getProjectsForWbsApi(params) {
  // Reuse project charter list to get projects
  return request.get('/pm/charters', { params })
}

// Product and Module APIs
export function getProductListApi(params) {
  return request.get('/pm/products', { params })
}

export function getAllProductsApi() {
  return request.get('/pm/products/all')
}

export function createProductApi(data) {
  return request.post('/pm/products', data)
}

export function updateProductApi(id, data) {
  return request.put(`/pm/products/${id}`, data)
}

export function deleteProductApi(id) {
  return request.delete(`/pm/products/${id}`)
}

export function getModuleListApi(params) {
  return request.get('/pm/modules', { params })
}

export function getModulesByProductApi(productId) {
  return request.get(`/pm/modules/by-product/${productId}`)
}

export function createModuleApi(data) {
  return request.post('/pm/modules', data)
}

export function updateModuleApi(id, data) {
  return request.put(`/pm/modules/${id}`, data)
}

export function deleteModuleApi(id) {
  return request.delete(`/pm/modules/${id}`)
}
