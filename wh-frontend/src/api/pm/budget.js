import request from '@/utils/request'

export function getBudgetListApi(params) {
  return request.get('/pm/budgets', { params })
}

export function getBudgetDetailApi(id) {
  return request.get(`/pm/budgets/${id}`)
}

export function getBudgetDetailWithItemsApi(id) {
  return request.get(`/pm/budgets/${id}/detail`)
}

export function createBudgetApi(data) {
  return request.post('/pm/budgets', data)
}

export function updateBudgetApi(id, data) {
  return request.put(`/pm/budgets/${id}`, data)
}

export function deleteBudgetApi(id) {
  return request.delete(`/pm/budgets/${id}`)
}

export function submitBudgetApi(id) {
  return request.post(`/pm/budgets/${id}/submit`)
}

export function approveBudgetApi(id, data) {
  return request.post(`/pm/budgets/${id}/approve`, data)
}

export function rejectBudgetApi(id, data) {
  return request.post(`/pm/budgets/${id}/reject`, data)
}

export function getBudgetComparisonApi(budgetId, params) {
  return request.get(`/pm/budgets/${budgetId}/comparison`, { params })
}

export function getBudgetVersionsApi(projectId) {
  return request.get(`/pm/budgets/versions/${projectId}`)
}
