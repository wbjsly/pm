import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setupPinia, elementStubs } from './helpers'

vi.mock('vue-router', async () => {
  const actual = await vi.importActual('vue-router')
  const useRoute = vi.fn(() => ({ path: '/dashboard', query: {} }))
  return { ...actual, useRoute }
})

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ code: 200, data: {} })),
    post: vi.fn(() => Promise.resolve({ code: 200 })),
    put: vi.fn(() => Promise.resolve({ code: 200 })),
    delete: vi.fn(() => Promise.resolve({ code: 200 })),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}))

describe('App', () => {
  beforeEach(() => setupPinia())

  it('非登录页渲染 MainLayout', async () => {
    const App = (await import('@/App.vue')).default
    const wrapper = mount(App, {
      global: {
        stubs: {
          MainLayout: { template: '<div class="layout-stub" />' },
          RouterView: { template: '<div class="router-view-stub" />' },
          'router-view': { template: '<div class="router-view-stub" />' },
        },
      },
    })
    expect(wrapper.exists()).toBe(true)
    expect(wrapper.find('.layout-stub').exists()).toBe(true)
  })

  it('登录页隐藏 MainLayout', async () => {
    const { useRoute } = await import('vue-router')
    useRoute.mockReturnValue({ path: '/login', query: {} })
    const App = (await import('@/App.vue')).default
    const wrapper = mount(App, {
      global: {
        stubs: {
          MainLayout: { template: '<div class="layout-stub" />' },
          RouterView: { template: '<div class="router-view-stub" />' },
          'router-view': { template: '<div class="router-view-stub" />' },
        },
      },
    })
    expect(wrapper.find('.layout-stub').exists()).toBe(false)
    expect(wrapper.find('.router-view-stub').exists()).toBe(true)
    useRoute.mockReturnValue({ path: '/dashboard', query: {} })
  })
})
