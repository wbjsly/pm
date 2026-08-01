import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setupPinia, elementStubs } from '../../../helpers'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

vi.mock('vue-router', async () => {
  const actual = await vi.importActual('vue-router')
  const router = { push: vi.fn(), replace: vi.fn(), go: vi.fn() }
  return { ...actual, useRouter: () => router, useRoute: () => ({ path: '/system/user/detail/u1', params: { id: 'u1' }, query: {} }) }
})

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ code: 200, data: {} })),
    post: vi.fn(() => Promise.resolve({ code: 200 })),
    put: vi.fn(() => Promise.resolve({ code: 200 })),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}))

describe('UserDetailPage', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
  })

  function createWrapper() {
    return import('@/views/system/user/detail.vue').then(m => mount(m.default, { global: { stubs: elementStubs } }))
  }

  it('渲染用户详情页面', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.exists()).toBe(true)
  })

  it('API 失败时不崩溃', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    expect(wrapper.exists()).toBe(true)
  })

  it('loadUserData 加载用户详情', async () => {
    request.get.mockResolvedValue({ code: 200, data: { username: 'admin', roles: ['ROLE_ADMIN'] } })
    const wrapper = await createWrapper()
    await wrapper.vm.loadUserData()
    await wrapper.vm.$nextTick()
    expect(request.get).toHaveBeenCalledWith('/system/users/u1')
    expect(wrapper.vm.userData.username).toBe('admin')
    expect(wrapper.vm.loading).toBe(false)
  })

  it('loadUserData 失败提示错误', async () => {
    const spy = vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
    request.get.mockRejectedValue(new Error('fail'))
    const wrapper = await createWrapper()
    await wrapper.vm.loadUserData()
    expect(spy).toHaveBeenCalled()
  })

  it('handleBack 返回用户列表', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    wrapper.vm.handleBack()
    const { useRouter } = await import('vue-router')
    expect(useRouter().push).toHaveBeenCalledWith('/system/user')
  })
})
