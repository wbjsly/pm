import request from '@/utils/request'

export function getActualCostListApi(params) {
  return request.get('/pm/actual-costs', { params })
}

export function createActualCostApi(data) {
  return request.post('/pm/actual-costs', data)
}

export function deleteActualCostApi(id) {
  return request.delete(`/pm/actual-costs/${id}`)
}

export function getActualCostAggregationApi(projectId) {
  return request.get('/pm/actual-costs/aggregation', { params: { projectId } })
}

export function getActualCostSumApi(params) {
  return request.get('/pm/actual-costs/sum', { params })
}
