import { defineStore } from 'pinia'
import { getAllDictsApi } from '@/api/system/dict'

export const useDictStore = defineStore('dict', {
  state: () => ({
    dictMap: {},    // { CHARTER_STATUS: { DRAFT: { label, tagType }, ... }, ... }
    loaded: false,
    loading: false
  }),

  getters: {
    /**
     * 获取指定类型的枚举条目列表（按 sort_order 排序）。
     * 组件下拉选项用: getDictItems('CHARTER_STATUS')
     */
    getDictItems: (state) => (typeCode) => {
      const group = state.dictMap[typeCode]
      if (!group) return []
      return Object.values(group)
    },

    /**
     * 获取单项 label。组件标签用: getLabel('CHARTER_STATUS', row.status)
     */
    getLabel: (state) => (typeCode, itemCode) => {
      return state.dictMap[typeCode]?.[itemCode]?.label || itemCode
    },

    /**
     * 获取单项 Element Plus tag 类型。statusTagType 用。
     */
    getTagType: (state) => (typeCode, itemCode) => {
      return state.dictMap[typeCode]?.[itemCode]?.tagType || ''
    }
  },

  actions: {
    async loadAll() {
      if (this.loaded || this.loading) return
      this.loading = true
      try {
        const res = await getAllDictsApi()
        const data = res.data || {}
        const map = {}
        for (const [typeCode, items] of Object.entries(data)) {
          const itemMap = {}
          for (const item of items) {
            itemMap[item.itemCode] = {
              itemCode: item.itemCode,
              label: item.itemLabel,
              tagType: item.tagType || '',
              itemValue: item.itemValue
            }
          }
          map[typeCode] = itemMap
        }
        this.dictMap = map
        this.loaded = true
      } catch (e) {
        console.warn('Dict load failed, using fallback:', e)
      } finally {
        this.loading = false
      }
    }
  }
})
