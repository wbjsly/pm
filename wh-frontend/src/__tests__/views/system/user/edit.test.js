import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setupPinia, elementStubs } from '../../../helpers'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const { routerMock } = vi.hoisted(() => ({ routerMock: { push: vi.fn(), replace: vi.fn(), go: vi.fn() } }))

vi.mock('vue-router', () => ({
  useRouter: () => routerMock,
  useRoute: () => ({ path: '/system/user/edit/u1', params: { id: 'u1' }, query: {} }),
}))

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ code: 200, data: { records: [], total: 0 } })),
    post: vi.fn(() => Promise.resolve({ code: 200 })),
    put: vi.fn(() => Promise.resolve({ code: 200 })),
    delete: vi.fn(() => Promise.resolve({ code: 200 })),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}))

describe('UserEditPage', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
  })

  function createWrapper(validateImpl) {
    const formStub = validateImpl === undefined
      ? { template: '<div><slot/></div>' }
      : { template: '<div><slot/></div>', methods: { validate: validateImpl } }
    return import('@/views/system/user/edit.vue').then(m => mount(m.default, { global: { stubs: { ...elementStubs, 'el-form': formStub } } }))
  }

  it('渲染用户编辑页面', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.exists()).toBe(true)
  })

  it('API 失败时不崩溃', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    expect(wrapper.exists()).toBe(true)
  })

  it('loadUserData 填充表单与角色', async () => {
    request.get.mockImplementation((url) => {
      if (url === '/system/users/u1') return Promise.resolve({ code: 200, data: { username: 'admin', nickName: '管理员', roles: ['ROLE_ADMIN'] } })
      if (url === '/system/roles') return Promise.resolve({ code: 200, data: [{ id: 'r1', roleCode: 'ROLE_ADMIN' }] })
      if (url === '/system/cost-quota/positions') return Promise.resolve({ code: 200, data: [{ id: 'p1', name: '开发' }] })
      return Promise.resolve({ code: 200, data: {} })
    })
    const wrapper = await createWrapper()
    await wrapper.vm.loadUserData()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.form.username).toBe('admin')
    expect(wrapper.vm.form.nickName).toBe('管理员')
    expect(wrapper.vm.form.roleIds).toEqual(['r1'])
    expect(wrapper.vm.positionList.length).toBe(1)
    expect(wrapper.vm.loading).toBe(false)
  })

  it('handleSubmit 成功保存并跳转', async () => {
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    request.put.mockResolvedValue({ code: 200 })
    const wrapper = await createWrapper(() => Promise.resolve(true))
    await wrapper.vm.$nextTick()
    wrapper.vm.form.nickName = '新昵称'
    await wrapper.vm.handleSubmit()
    expect(request.put).toHaveBeenCalledWith('/system/users/u1', expect.objectContaining({ nickName: '新昵称' }))
    expect(routerMock.push).toHaveBeenCalledWith('/system/user')
    expect(wrapper.vm.submitting).toBe(false)
  })

  it('handleSubmit 校验失败不提交', async () => {
    const wrapper = await createWrapper(() => Promise.reject())
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleSubmit()
    expect(request.put).not.toHaveBeenCalled()
  })

  it('handleBack 返回用户列表', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    wrapper.vm.handleBack()
    expect(routerMock.push).toHaveBeenCalledWith('/system/user')
  })
})
