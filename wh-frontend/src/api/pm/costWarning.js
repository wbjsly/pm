import request from '@/utils/request'

export function getCostWarningListApi(params) {
  return request.get('/pm/cost-warnings', { params })
}

export function closeCostWarningApi(id) {
  return request.post(`/pm/cost-warnings/${id}/close`)
}

export function triggerCostWarningApi() {
  return request.post('/pm/cost-warnings/trigger')
}

export function getActiveCostWarningsApi(limit = 5) {
  return request.get('/pm/cost-warnings/active', { params: { limit } })
}
