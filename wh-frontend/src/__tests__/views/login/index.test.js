import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setupPinia, routerStubs, elementStubs } from '../../helpers'
import LoginPage from '@/views/login/index.vue'
import { ElMessage } from 'element-plus'

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    useRouter: () => ({ push: vi.fn(), replace: vi.fn(), go: vi.fn() }),
    useRoute: () => ({ path: '/login', params: {}, query: {} }),
  }
})

describe('LoginPage', () => {
  beforeEach(() => setupPinia())

  function createWrapper(validateImpl) {
    const formStub = validateImpl === undefined
      ? { template: '<div><slot/></div>' }
      : { template: '<div><slot/></div>', methods: { validate: validateImpl } }
    return mount(LoginPage, {
      global: {
        provide: routerStubs.provide,
        stubs: { 'router-link': true, ...elementStubs, 'el-form': formStub },
      },
    })
  }

  it('渲染登录页面', () => {
    const wrapper = createWrapper()
    expect(wrapper.exists()).toBe(true)
    expect(wrapper.text()).toContain('WH管理系统')
  })

  it('显示用户名和密码输入框', () => {
    const wrapper = createWrapper()
    const inputs = wrapper.findAll('input')
    expect(inputs.length).toBeGreaterThanOrEqual(2)
  })

  it('显示登录按钮', () => {
    const wrapper = createWrapper()
    expect(wrapper.text()).toContain('登录')
  })

  it('handleLogin 校验失败不调用登录', async () => {
    const wrapper = createWrapper(() => Promise.reject())
    await wrapper.vm.handleLogin()
    expect(wrapper.vm.loading).toBe(false)
  })

  it('handleLogin 成功跳转首页', async () => {
    const pinia = await import('pinia')
    const { useUserStore } = await import('@/store/user')
    const store = useUserStore()
    store.login = vi.fn().mockResolvedValue({})
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})

    const wrapper = createWrapper(() => Promise.resolve(true))
    wrapper.vm.form.username = 'admin'
    wrapper.vm.form.password = 'Admin@123'
    await wrapper.vm.handleLogin()
    expect(store.login).toHaveBeenCalledWith('admin', 'Admin@123')
    expect(wrapper.vm.loading).toBe(false)
  })

  it('handleLogin 失败提示错误', async () => {
    const { useUserStore } = await import('@/store/user')
    const store = useUserStore()
    store.login = vi.fn().mockRejectedValue(new Error('bad'))
    vi.spyOn(ElMessage, 'error').mockImplementation(() => {})

    const wrapper = createWrapper(() => Promise.resolve(true))
    await wrapper.vm.handleLogin()
    expect(ElMessage.error).toHaveBeenCalled()
  })
})
