import request from '@/utils/request'

export function getStatsApi(pmId) {
  return request.get('/pm/charters/stats', { params: { pmId } })
}
