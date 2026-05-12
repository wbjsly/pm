import request from '@/utils/request'

/** 全量字典 */
export function getAllDictsApi() {
  return request.get('/system/dict/all')
}

/** 按类型获取条目 */
export function getDictItemsApi(typeCode) {
  return request.get(`/system/dict/items/${typeCode}`)
}

/** 字典类型列表 */
export function getDictTypesApi() {
  return request.get('/system/dict/types')
}

/** 新增字典类型 */
export function createDictTypeApi(data) {
  return request.post('/system/dict/type', data)
}

/** 更新字典类型 */
export function updateDictTypeApi(data) {
  return request.put('/system/dict/type', data)
}

/** 删除字典类型 */
export function deleteDictTypeApi(id) {
  return request.delete(`/system/dict/type/${id}`)
}

/** 新增字典条目 */
export function createDictItemApi(data) {
  return request.post('/system/dict/item', data)
}

/** 更新字典条目 */
export function updateDictItemApi(data) {
  return request.put('/system/dict/item', data)
}

/** 删除字典条目 */
export function deleteDictItemApi(id) {
  return request.delete(`/system/dict/item/${id}`)
}
