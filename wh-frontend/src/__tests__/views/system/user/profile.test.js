import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setupPinia, elementStubs } from '../../../helpers'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

vi.mock('vue-router', async () => {
  const actual = await vi.importActual('vue-router')
  return { ...actual, useRouter: () => ({ push: vi.fn(), replace: vi.fn(), go: vi.fn() }), useRoute: () => ({ path: '/', params: {}, query: {} }) }
})

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ code: 200, data: {} })),
    post: vi.fn(() => Promise.resolve({ code: 200 })),
    put: vi.fn(() => Promise.resolve({ code: 200 })),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}))

describe('UserProfilePage', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
  })

  function createWrapper() {
    return import('@/views/system/user/profile.vue').then(m => mount(m.default, { global: { stubs: elementStubs } }))
  }

  it('渲染个人资料页面', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.exists()).toBe(true)
  })

  it('无 userId 时不请求详情', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    expect(request.get).not.toHaveBeenCalled()
  })

  it('有 userId 时加载用户详情', async () => {
    const pinia = await import('pinia')
    const { useUserStore } = await import('@/store/user')
    const store = useUserStore()
    store.userInfo = { userId: 'u1' }
    request.get.mockResolvedValue({ code: 200, data: { username: 'admin', roles: ['ROLE_ADMIN'] } })

    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    expect(request.get).toHaveBeenCalledWith('/system/users/u1')
    expect(wrapper.vm.userData.username).toBe('admin')
  })

  it('加载失败提示错误', async () => {
    const pinia = await import('pinia')
    const { useUserStore } = await import('@/store/user')
    const store = useUserStore()
    store.userInfo = { userId: 'u1' }
    const spy = vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
    request.get.mockRejectedValue(new Error('fail'))

    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    expect(spy).toHaveBeenCalled()
  })
})
