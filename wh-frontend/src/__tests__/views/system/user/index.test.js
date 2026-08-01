import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setupPinia, elementStubs } from '../../../helpers'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const { routerMock } = vi.hoisted(() => ({ routerMock: { push: vi.fn(), replace: vi.fn(), go: vi.fn() } }))

vi.mock('vue-router', () => ({
  useRouter: () => routerMock,
  useRoute: () => ({ path: '/', params: {}, query: {} }),
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

describe('UserPage', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
  })

  function createWrapper(validateImpl) {
    const formStub = validateImpl === undefined
      ? { template: '<div><slot/></div>' }
      : { template: '<div><slot/></div>', methods: { validate: validateImpl } }
    return import('@/views/system/user/index.vue').then(m => mount(m.default, { global: { stubs: { ...elementStubs, 'el-form': formStub, 'el-dialog': { template: '<div><slot/><slot name="footer"/></div>' } } } }))
  }

  it('渲染用户列表页面', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.exists()).toBe(true)
  })

  it('挂载时加载角色、岗位与用户列表', async () => {
    request.get.mockImplementation((url) => {
      if (url === '/system/roles') return Promise.resolve({ code: 200, data: [{ id: 'r1', roleName: '管理员' }] })
      if (url === '/system/cost-quota/positions') return Promise.resolve({ code: 200, data: [{ id: 'p1', name: '开发' }] })
      return Promise.resolve({ code: 200, data: { records: [{ id: 'u1', username: 'admin' }], total: 1 } })
    })
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.roleMap).toBeTruthy()
    expect(wrapper.vm.positionList.length).toBe(1)
    expect(wrapper.vm.tableData.length).toBe(1)
  })

  it('resetQuery 重置查询参数', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    wrapper.vm.queryParams.keyword = 'x'
    wrapper.vm.queryParams.pageNum = 2
    wrapper.vm.resetQuery()
    expect(wrapper.vm.queryParams.keyword).toBe('')
    expect(wrapper.vm.queryParams.pageNum).toBe(1)
  })

  it('handleView / handleEdit 跳转', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    wrapper.vm.handleView({ id: 'u1' })
    expect(routerMock.push).toHaveBeenCalledWith('/system/user/detail/u1')
    wrapper.vm.handleEdit({ id: 'u1' })
    expect(routerMock.push).toHaveBeenCalledWith('/system/user/edit/u1')
  })

  it('handleResetPassword 打开弹窗并清空表单', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    wrapper.vm.passwordForm.password = 'old'
    wrapper.vm.handleResetPassword({ id: 'u1' })
    expect(wrapper.vm.currentUserId).toBe('u1')
    expect(wrapper.vm.passwordDialogVisible).toBe(true)
    expect(wrapper.vm.passwordForm.password).toBe('')
  })

  it('confirmResetPassword 成功重置密码', async () => {
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    request.put.mockResolvedValue({ code: 200 })
    const wrapper = await createWrapper(() => Promise.resolve(true))
    await wrapper.vm.$nextTick()
    wrapper.vm.currentUserId = 'u1'
    wrapper.vm.passwordDialogVisible = true
    wrapper.vm.passwordForm.password = 'NewPass@1'
    await wrapper.vm.$nextTick()
    await wrapper.vm.confirmResetPassword()
    expect(request.put).toHaveBeenCalledWith('/system/users/u1/password', { password: 'NewPass@1' })
    expect(wrapper.vm.passwordDialogVisible).toBe(false)
  })

  it('confirmResetPassword 校验失败不提交', async () => {
    const wrapper = await createWrapper(() => Promise.reject())
    await wrapper.vm.$nextTick()
    wrapper.vm.passwordDialogVisible = true
    await wrapper.vm.$nextTick()
    await wrapper.vm.confirmResetPassword()
    expect(request.put).not.toHaveBeenCalled()
  })

  it('handleDelete 确认后删除并刷新', async () => {
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    request.delete.mockResolvedValue({ code: 200 })
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    wrapper.vm.loadData = vi.fn()
    await wrapper.vm.handleDelete({ id: 'u1', username: 'admin' })
    expect(request.delete).toHaveBeenCalledWith('/system/users/u1')
  })

  it('handleToggleStatus 停用用户', async () => {
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    request.put.mockResolvedValue({ code: 200 })
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    wrapper.vm.loadData = vi.fn()
    await wrapper.vm.handleToggleStatus({ id: 'u1', status: '1', username: 'admin' })
    expect(request.put).toHaveBeenCalledWith('/system/users/u1', { status: '0' })
  })
})
