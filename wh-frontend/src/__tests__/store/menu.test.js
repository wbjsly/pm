import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

vi.mock('@/api/system/menu', () => ({
  getUserMenusApi: vi.fn(),
}))

import { useMenuStore } from '@/store/menu'
import { getUserMenusApi } from '@/api/system/menu'

describe('useMenuStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('初始状态未加载', () => {
    const store = useMenuStore()
    expect(store.menuItems).toEqual([])
    expect(store.loaded).toBe(false)
    expect(store.loading).toBe(false)
  })

  describe('fetchMenus', () => {
    it('获取菜单数据后 loaded 为 true', async () => {
      const mockMenus = [
        { name: 'Charter', path: '/pm/charter', title: '项目立项', children: [] },
      ]
      vi.mocked(getUserMenusApi).mockResolvedValue({ code: 200, data: mockMenus })

      const store = useMenuStore()
      await store.fetchMenus()

      expect(store.menuItems).toEqual(mockMenus)
      expect(store.loaded).toBe(true)
      expect(store.loading).toBe(false)
    })

    it('如果已加载则跳过重复请求', async () => {
      const store = useMenuStore()
      store.loaded = true

      await store.fetchMenus()
      expect(getUserMenusApi).not.toHaveBeenCalled()
    })

    it('如果正在加载则跳过重复请求', async () => {
      const store = useMenuStore()
      store.loading = true

      await store.fetchMenus()
      expect(getUserMenusApi).not.toHaveBeenCalled()
    })

    it('API 失败时清空 menus，loaded 保持 false', async () => {
      vi.mocked(getUserMenusApi).mockRejectedValue(new Error('Network error'))

      const store = useMenuStore()
      await store.fetchMenus()

      expect(store.menuItems).toEqual([])
      expect(store.loaded).toBe(false)
      expect(store.loading).toBe(false)
    })

    it('API 返回空 data 时处理正常', async () => {
      vi.mocked(getUserMenusApi).mockResolvedValue({ code: 200, data: null })

      const store = useMenuStore()
      await store.fetchMenus()

      expect(store.menuItems).toEqual([])
      expect(store.loaded).toBe(true)
    })
  })

  describe('reset', () => {
    it('重置所有状态', async () => {
      const store = useMenuStore()
      vi.mocked(getUserMenusApi).mockResolvedValue({
        code: 200, data: [{ name: 'Test', path: '/test' }],
      })
      await store.fetchMenus()
      expect(store.loaded).toBe(true)
      expect(store.menuItems).toHaveLength(1)

      store.reset()
      expect(store.menuItems).toEqual([])
      expect(store.loaded).toBe(false)
      expect(store.loading).toBe(false)
    })
  })
})
