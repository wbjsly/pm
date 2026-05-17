import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

vi.mock('@/api/system/dict', () => ({
  getAllDictsApi: vi.fn(),
}))

import { useDictStore } from '@/store/dict'
import { getAllDictsApi } from '@/api/system/dict'

describe('useDictStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('初始状态未加载', () => {
    const store = useDictStore()
    expect(store.loaded).toBe(false)
    expect(store.loading).toBe(false)
    expect(store.dictMap).toEqual({})
  })

  describe('getDictItems', () => {
    it('获取字典选项列表', async () => {
      vi.mocked(getAllDictsApi).mockResolvedValue({
        code: 200,
        data: {
          CHARTER_STATUS: [
            { itemCode: 'DRAFT', itemLabel: '草稿', tagType: 'info', itemValue: 'DRAFT' },
            { itemCode: 'APPROVED', itemLabel: '已审批', tagType: 'success', itemValue: 'APPROVED' },
          ],
        },
      })

      const store = useDictStore()
      await store.loadAll()

      const items = store.getDictItems('CHARTER_STATUS')
      expect(items).toHaveLength(2)
      expect(items[0].label).toBe('草稿')
      expect(items[1].label).toBe('已审批')
    })

    it('不存在的字典类型返回空数组', () => {
      const store = useDictStore()
      expect(store.getDictItems('NONEXISTENT')).toEqual([])
    })
  })

  describe('getLabel', () => {
    it('根据类型和编码获取标签', async () => {
      vi.mocked(getAllDictsApi).mockResolvedValue({
        code: 200,
        data: {
          CHARTER_STATUS: [
            { itemCode: 'DRAFT', itemLabel: '草稿', tagType: 'info', itemValue: 'DRAFT' },
          ],
        },
      })

      const store = useDictStore()
      await store.loadAll()

      expect(store.getLabel('CHARTER_STATUS', 'DRAFT')).toBe('草稿')
    })

    it('找不到时返回原始编码', () => {
      const store = useDictStore()
      expect(store.getLabel('CHARTER_STATUS', 'UNKNOWN')).toBe('UNKNOWN')
    })
  })

  describe('getTagType', () => {
    it('根据类型和编码获取 tag 类型', async () => {
      vi.mocked(getAllDictsApi).mockResolvedValue({
        code: 200,
        data: {
          CHARTER_STATUS: [
            { itemCode: 'DRAFT', itemLabel: '草稿', tagType: 'info', itemValue: 'DRAFT' },
          ],
        },
      })

      const store = useDictStore()
      await store.loadAll()

      expect(store.getTagType('CHARTER_STATUS', 'DRAFT')).toBe('info')
    })

    it('找不到或无 tagType 时返回空字符串', () => {
      const store = useDictStore()
      expect(store.getTagType('NONEXISTENT', 'X')).toBe('')
    })
  })

  describe('loadAll', () => {
    it('加载字典数据后 loaded 为 true', async () => {
      vi.mocked(getAllDictsApi).mockResolvedValue({ code: 200, data: {} })

      const store = useDictStore()
      await store.loadAll()

      expect(store.loaded).toBe(true)
      expect(store.loading).toBe(false)
    })

    it('如果已加载则跳过重复请求', async () => {
      vi.mocked(getAllDictsApi).mockResolvedValue({ code: 200, data: {} })

      const store = useDictStore()
      store.loaded = true

      await store.loadAll()
      expect(getAllDictsApi).not.toHaveBeenCalled()
    })

    it('如果正在加载则跳过重复请求', async () => {
      const store = useDictStore()
      store.loading = true

      await store.loadAll()
      expect(getAllDictsApi).not.toHaveBeenCalled()
    })

    it('API 失败时不抛出异常，loaded 保持 false', async () => {
      vi.mocked(getAllDictsApi).mockRejectedValue(new Error('Network error'))

      const store = useDictStore()
      await store.loadAll()

      expect(store.loaded).toBe(false)
      expect(store.loading).toBe(false)
      expect(store.dictMap).toEqual({})
    })

    it('正确构建 dictMap 结构', async () => {
      vi.mocked(getAllDictsApi).mockResolvedValue({
        code: 200,
        data: {
          CHARTER_STATUS: [
            { itemCode: 'DRAFT', itemLabel: '草稿', tagType: 'info' },
          ],
          BUDGET_STATUS: [
            { itemCode: 'APPROVED', itemLabel: '已审批', tagType: 'success' },
          ],
        },
      })

      const store = useDictStore()
      await store.loadAll()

      expect(Object.keys(store.dictMap)).toHaveLength(2)
      expect(store.dictMap.CHARTER_STATUS.DRAFT).toMatchObject({
        itemCode: 'DRAFT', label: '草稿', tagType: 'info',
      })
    })
  })
})
