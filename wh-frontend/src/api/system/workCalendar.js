import request from '@/utils/request'

export function getWorkCalendarApi(year) {
  return request.get('/system/work-calendar', { params: { year } })
}

export function getWorkCalendarMonthApi(year, month) {
  return request.get('/system/work-calendar/month', { params: { year, month } })
}

export function updateWorkCalendarDayApi(data) {
  return request.post('/system/work-calendar', data)
}

export function batchUpdateWorkCalendarApi(data) {
  return request.post('/system/work-calendar/batch', data)
}

export function generateWorkCalendarApi(year) {
  return request.post('/system/work-calendar/generate', { year })
}

export function deleteWorkCalendarApi(id) {
  return request.delete(`/system/work-calendar/${id}`)
}

export function isWorkDayApi(date) {
  return request.get('/system/work-calendar/is-workday', { params: { date } })
}
