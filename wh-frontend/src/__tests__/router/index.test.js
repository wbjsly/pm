import { describe, it, expect, vi, beforeEach, beforeAll } from 'vitest'
import { createMemoryHistory } from 'vue-router'

// ========== Create mutable store mocks ==========
const { mockUserStore, mockDictStore, mockMenuStore, mockTabStore } = vi.hoisted(() => ({
  mockUserStore: { token: '', userInfo: null, permissions: [], getUserInfo: vi.fn().mockResolvedValue(), logout: vi.fn() },
  mockDictStore: { loaded: false, loading: false, loadAll: vi.fn().mockResolvedValue(), getDictItems: vi.fn(() => []), getLabel: vi.fn(() => ''), getTagType: vi.fn(() => '') },
  mockMenuStore: { menuItems: [], loaded: false, loading: false, fetchMenus: vi.fn().mockResolvedValue(), reset: vi.fn() },
  mockTabStore: { tabs: [{ name: 'Dashboard', path: '/dashboard', title: '主页', closable: false }], activeTab: '/dashboard', addTab: vi.fn() },
}))

// ========== Mock stores ==========
vi.mock('@/store/user', () => ({ useUserStore: vi.fn(() => mockUserStore) }))
vi.mock('@/store/dict', () => ({ useDictStore: vi.fn(() => mockDictStore) }))
vi.mock('@/store/menu', () => ({ useMenuStore: vi.fn(() => mockMenuStore) }))
vi.mock('@/store/tab', () => ({ useTabStore: vi.fn(() => mockTabStore) }))

// ========== Mock utils/request (needed by imports in stores) ==========
vi.mock('@/utils/request', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn(), interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } } },
}))

// ========== Override createWebHistory for test environment ==========
vi.mock('vue-router', async () => {
  const actual = await vi.importActual('vue-router')
  return { ...actual, createWebHistory: () => actual.createMemoryHistory() }
})

// ========== Mock all lazy-loaded view components ==========
const MockView = { template: '<div>MockView</div>' }
vi.mock('@/views/login/index.vue', () => ({ default: MockView }))
vi.mock('@/views/dashboard/index.vue', () => ({ default: MockView }))
vi.mock('@/views/pm/charter/index.vue', () => ({ default: MockView }))
vi.mock('@/views/pm/charter/detail.vue', () => ({ default: MockView }))
vi.mock('@/views/pm/charter/form.vue', () => ({ default: MockView }))
vi.mock('@/views/pm/wbs/index.vue', () => ({ default: MockView }))
vi.mock('@/views/pm/wbs/detail.vue', () => ({ default: MockView }))
vi.mock('@/views/pm/wbs/form.vue', () => ({ default: MockView }))
vi.mock('@/views/pm/wbs/history.vue', () => ({ default: MockView }))
vi.mock('@/views/pm/product/index.vue', () => ({ default: MockView }))
vi.mock('@/views/pm/deliverable/index.vue', () => ({ default: MockView }))
vi.mock('@/views/pm/deliverable/detail.vue', () => ({ default: MockView }))
vi.mock('@/views/pm/deliverable/form.vue', () => ({ default: MockView }))
vi.mock('@/views/pm/budget/index.vue', () => ({ default: MockView }))
vi.mock('@/views/pm/budget/form.vue', () => ({ default: MockView }))
vi.mock('@/views/pm/budget/detail.vue', () => ({ default: MockView }))
vi.mock('@/views/pm/budget/upgrade.vue', () => ({ default: MockView }))
vi.mock('@/views/pm/budget/comparison.vue', () => ({ default: MockView }))
vi.mock('@/views/pm/work-hours/index.vue', () => ({ default: MockView }))
vi.mock('@/views/pm/work-hours/approval.vue', () => ({ default: MockView }))
vi.mock('@/views/system/calendar/index.vue', () => ({ default: MockView }))
vi.mock('@/views/system/cost-quota/index.vue', () => ({ default: MockView }))
vi.mock('@/views/system/dict/index.vue', () => ({ default: MockView }))
vi.mock('@/views/system/menu/index.vue', () => ({ default: MockView }))
vi.mock('@/views/system/user/index.vue', () => ({ default: MockView }))
vi.mock('@/views/system/user/detail.vue', () => ({ default: MockView }))
vi.mock('@/views/system/user/edit.vue', () => ({ default: MockView }))
vi.mock('@/views/system/user/profile.vue', () => ({ default: MockView }))
vi.mock('@/components/layout/MainLayout.vue', () => ({ default: MockView }))
vi.mock('@/components/layout/SidebarMenu.vue', () => ({ default: MockView }))

import router from '@/router'

describe('Router Navigation Guards', () => {
  beforeAll(async () => {
    // Push to a route to resolve initial navigation
    await router.push('/login').catch(() => {})
  })

  beforeEach(async () => {
    // Reset store state
    mockUserStore.token = ''
    mockUserStore.userInfo = null
    mockUserStore.permissions = []
    mockUserStore.getUserInfo.mockReset().mockResolvedValue()
    mockUserStore.logout.mockReset()
    mockDictStore.loaded = false
    mockDictStore.loadAll.mockReset().mockResolvedValue()
    mockMenuStore.loaded = false
    mockMenuStore.loading = false
    mockMenuStore.fetchMenus.mockReset().mockResolvedValue()
    mockTabStore.addTab.mockReset()
  })

  it('直接访问 /login 无需认证直接放行', async () => {
    await router.push('/login')
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('无 token 访问 /dashboard 重定向到 /login 并携带 redirect', async () => {
    await router.push('/login')
    await router.push('/dashboard')
    expect(router.currentRoute.value.path).toBe('/login')
    expect(router.currentRoute.value.query.redirect).toBe('/dashboard')
  })

  it('有 token 有 userInfo 访问受保护路由直接放行', async () => {
    mockUserStore.token = 'mock-token-123'
    mockUserStore.userInfo = { username: 'admin', nickName: '管理员', roles: ['ROLE_ADMIN'] }

    await router.push('/login')
    await router.push('/dashboard')
    expect(router.currentRoute.value.path).toBe('/dashboard')
  })

  it('有 token 但无 userInfo 时调用 getUserInfo 成功后放行', async () => {
    mockUserStore.token = 'mock-token-123'
    mockUserStore.userInfo = null

    await router.push('/login')
    await router.push('/pm/charter')
    expect(mockUserStore.getUserInfo).toHaveBeenCalled()
    expect(router.currentRoute.value.path).toBe('/pm/charter')
  })

  it('有 token 但 getUserInfo 失败时重定向到 /login', async () => {
    mockUserStore.token = 'mock-token-123'
    mockUserStore.userInfo = null
    mockUserStore.getUserInfo.mockRejectedValue(new Error('Auth failed'))

    await router.push('/login')
    await router.push('/pm/wbs')
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('放行后调用 dictStore.loadAll()', async () => {
    mockUserStore.token = 'mock-token'
    mockUserStore.userInfo = { username: 'admin', roles: ['ROLE_ADMIN'] }

    await router.push('/login')
    await router.push('/pm/product')
    expect(mockDictStore.loadAll).toHaveBeenCalled()
  })

  it('dictStore 已加载时不重复调用 loadAll', async () => {
    mockUserStore.token = 'mock-token'
    mockUserStore.userInfo = { username: 'admin', roles: ['ROLE_ADMIN'] }
    mockDictStore.loaded = true

    await router.push('/login')
    await router.push('/pm/deliverable')
    expect(mockDictStore.loadAll).not.toHaveBeenCalled()
  })

  it('放行后调用 menuStore.fetchMenus()', async () => {
    mockUserStore.token = 'mock-token'
    mockUserStore.userInfo = { username: 'admin', roles: ['ROLE_ADMIN'] }

    await router.push('/login')
    await router.push('/pm/budget')
    expect(mockMenuStore.fetchMenus).toHaveBeenCalled()
  })

  it('menuStore 已加载或加载中时不重复调用 fetchMenus', async () => {
    mockUserStore.token = 'mock-token'
    mockUserStore.userInfo = { username: 'admin', roles: ['ROLE_ADMIN'] }
    mockMenuStore.loaded = true

    await router.push('/login')
    await router.push('/pm/work-hours')
    expect(mockMenuStore.fetchMenus).not.toHaveBeenCalled()

    mockMenuStore.loaded = false
    mockMenuStore.loading = true
    await router.push('/system/calendar')
    expect(mockMenuStore.fetchMenus).not.toHaveBeenCalled()
  })

  it('放行后调用 tabStore.addTab() 添加标签页', async () => {
    mockUserStore.token = 'mock-token'
    mockUserStore.userInfo = { username: 'admin', roles: ['ROLE_ADMIN'] }

    await router.push('/login')
    await router.push('/system/dict')
    expect(mockTabStore.addTab).toHaveBeenCalled()
  })

  it('设置页面标题为 meta.title', async () => {
    await router.push('/login')
    expect(document.title).toBe('登录')
  })

  it('隐藏路由的 addTab 被调用', async () => {
    mockUserStore.token = 'mock-token'
    mockUserStore.userInfo = { username: 'admin', roles: ['ROLE_ADMIN'] }

    await router.push('/login')
    await router.push('/pm/charter/detail/123')
    expect(mockTabStore.addTab).toHaveBeenCalled()
    expect(router.currentRoute.value.path).toBe('/pm/charter/detail/123')
  })
})
