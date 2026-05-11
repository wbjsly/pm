import request from '@/utils/request'

// 岗位管理
export function getPositionListApi() {
  return request.get('/system/cost-quota/positions')
}

export function createPositionApi(data) {
  return request.post('/system/cost-quota/positions', data)
}

export function updatePositionApi(id, data) {
  return request.put(`/system/cost-quota/positions/${id}`, data)
}

export function deletePositionApi(id) {
  return request.delete(`/system/cost-quota/positions/${id}`)
}

// 年份管理
export function getYearListApi() {
  return request.get('/system/cost-quota/years')
}

export function createYearApi(data) {
  return request.post('/system/cost-quota/years', data)
}

export function updateYearApi(id, data) {
  return request.put(`/system/cost-quota/years/${id}`, data)
}

export function deleteYearApi(id) {
  return request.delete(`/system/cost-quota/years/${id}`)
}

export function getDefaultStartDateApi() {
  return request.get('/system/cost-quota/years/latest/default-start-date')
}

// 定额管理
export function getCurrentRateApi(positionId) {
  return request.get('/system/cost-quota/quotas/current-rate', { params: { positionId } })
}

export function getQuotaListApi(yearId) {
  return request.get('/system/cost-quota/quotas', { params: { yearId } })
}

export function adjustQuotaApi(data) {
  return request.post('/system/cost-quota/quotas/adjust', data)
}

export function getQuotaHistoryApi(positionId, yearId) {
  return request.get('/system/cost-quota/quotas/history', { params: { positionId, yearId } })
}

export function compareVersionsApi(quotaId1, quotaId2) {
  return request.get('/system/cost-quota/quotas/compare', { params: { quotaId1, quotaId2 } })
}
