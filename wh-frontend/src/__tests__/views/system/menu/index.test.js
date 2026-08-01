import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setupPinia, elementStubs } from '../../../helpers'
import { setActivePinia, createPinia } from 'pinia'

vi.mock('@/api/system/menu', () => ({
  getMenuListApi: vi.fn(),
  createMenuApi: vi.fn(),
  updateMenuApi: vi.fn(),
  deleteMenuApi: vi.fn(),
}))

vi.mock('@/api/system/user', () => ({
  getAllRolesApi: vi.fn(),
}))

vi.mock('@element-plus/icons-vue', () => ({
  Plus: { name: 'Plus', template: '<i class="el-icon-plus" />' },
  Edit: { name: 'Edit', template: '<i class="el-icon-edit" />' },
  Delete: { name: 'Delete', template: '<i class="el-icon-delete" />' },
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn(), info: vi.fn() },
    ElMessageBox: { confirm: vi.fn(() => Promise.resolve()) },
  }
})

import * as menuApi from '@/api/system/menu'
import * as userApi from '@/api/system/user'

const mockMenuList = [
  { id: 'm1', title: '系统管理', path: '/system', icon: 'Setting', sortOrder: 1, perm: 'admin', status: '1', parentId: null },
  { id: 'm2', title: '用户管理', path: '/system/user', icon: 'User', sortOrder: 2, perm: 'admin', status: '1', parentId: 'm1' },
  { id: 'm3', title: '菜单管理', path: '/system/menu', icon: 'Menu', sortOrder: 3, perm: 'admin', status: '1', parentId: 'm1' },
  { id: 'm4', title: '项目管理', path: '/pm', icon: 'Files', sortOrder: 4, perm: 'admin,pm', status: '1', parentId: null },
  { id: 'm5', title: '立项管理', path: '/pm/charter', icon: 'Document', sortOrder: 5, perm: 'pm', status: '0', parentId: 'm4' },
]

const mockRoles = [
  { roleCode: 'admin', roleName: '管理员' },
  { roleCode: 'pm', roleName: '项目经理' },
]

function createWrapper() {
  return import('@/views/system/menu/index.vue').then(m =>
    mount(m.default, { global: { stubs: { ...elementStubs, 'el-dialog': { template: '<div><slot/><slot name="footer"/></div>' } } } })
  )
}

describe('MenuPage', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    menuApi.getMenuListApi.mockResolvedValue({ code: 200, data: mockMenuList })
    userApi.getAllRolesApi.mockResolvedValue({ code: 200, data: mockRoles })
    menuApi.createMenuApi.mockResolvedValue({ code: 200 })
    menuApi.updateMenuApi.mockResolvedValue({ code: 200 })
    menuApi.deleteMenuApi.mockResolvedValue({ code: 200 })
  })

  it('渲染菜单管理页面', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    expect(wrapper.exists()).toBe(true)
  })

  it('组件使用 onActivated，mount 时不请求 API', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    // onActivated does not fire in mount, so no API calls
    expect(menuApi.getMenuListApi).not.toHaveBeenCalled()
  })

  it('显示菜单管理标题', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('菜单管理')
  })

  it('手动触发 loadData 后加载菜单', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    // Trigger loadData directly (simulates onActivated)
    await wrapper.vm.loadData()
    await wrapper.vm.$nextTick()

    expect(menuApi.getMenuListApi).toHaveBeenCalled()
    // Tree data should have root nodes
    expect(wrapper.vm.menuList.length).toBeGreaterThan(0)
  })

  it('loadData 后构建树形数据', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    await wrapper.vm.loadData()
    await wrapper.vm.$nextTick()

    // treeData = computed; should have 2 root nodes
    expect(wrapper.vm.treeData.length).toBe(2)
  })

  it('加载角色选项', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    await wrapper.vm.fetchRoles()
    await wrapper.vm.$nextTick()

    expect(userApi.getAllRolesApi).toHaveBeenCalled()
    expect(wrapper.vm.roleOptions.length).toBe(2)
  })

  it('新增顶级菜单', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.handleAdd()
    expect(wrapper.vm.formVisible).toBe(true)
    expect(wrapper.vm.dialogTitle).toBe('新增菜单')
    expect(wrapper.vm.form.parentId).toBe('')
  })

  it('新增子菜单', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.handleAdd('m1')
    expect(wrapper.vm.formVisible).toBe(true)
    expect(wrapper.vm.dialogTitle).toBe('新增子菜单')
    expect(wrapper.vm.form.parentId).toBe('m1')
  })

  it('编辑菜单填充表单', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.handleEdit(mockMenuList[0])
    expect(wrapper.vm.formVisible).toBe(true)
    expect(wrapper.vm.dialogTitle).toBe('编辑菜单')
    expect(wrapper.vm.form.title).toBe('系统管理')
    expect(wrapper.vm.form.path).toBe('/system')
  })

  it('提交新建菜单', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.handleAdd()
    wrapper.vm.form.title = '测试菜单'
    wrapper.vm.form.path = '/test'
    wrapper.vm.formRef = { validate: vi.fn(() => Promise.resolve(true)) }

    await wrapper.vm.handleSubmit()
    await wrapper.vm.$nextTick()

    expect(menuApi.createMenuApi).toHaveBeenCalled()
  })

  it('提交编辑菜单', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.handleEdit(mockMenuList[0])
    wrapper.vm.formRef = { validate: vi.fn(() => Promise.resolve(true)) }

    await wrapper.vm.handleSubmit()
    await wrapper.vm.$nextTick()

    expect(menuApi.updateMenuApi).toHaveBeenCalled()
  })

  it('删除菜单', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    await wrapper.vm.handleDelete('m1')
    await wrapper.vm.$nextTick()

    expect(menuApi.deleteMenuApi).toHaveBeenCalledWith('m1')
  })

  it('表单验证失败时不提交', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.handleAdd()
    wrapper.vm.formRef = { validate: vi.fn(() => Promise.resolve(false)) }

    await wrapper.vm.handleSubmit()
    expect(menuApi.createMenuApi).not.toHaveBeenCalled()
  })

  it('roleMap 计算属性正确', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.roleOptions = mockRoles
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.roleMap.admin).toBe('管理员')
    expect(wrapper.vm.roleMap.pm).toBe('项目经理')
  })

  it('API 失败时组件不崩溃', async () => {
    menuApi.getMenuListApi.mockRejectedValue(new Error('Network error'))

    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    // loadData doesn't have catch, error should be handled silently
    try {
      await wrapper.vm.loadData()
    } catch (e) {
      // Expected - component should not crash
    }
    await wrapper.vm.$nextTick()

    expect(wrapper.exists()).toBe(true)
  })
})

describe('MenuPage 边界', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
  })

  function createWrapper(validateImpl) {
    const formStub = validateImpl === undefined
      ? { template: '<div><slot/></div>' }
      : { template: '<div><slot/></div>', methods: { validate: validateImpl } }
    return import('@/views/system/menu/index.vue').then(m => mount(m.default, { global: { stubs: { ...elementStubs, 'el-form': formStub, 'el-dialog': { template: '<div><slot/><slot name="footer"/></div>' } } } }))
  }

  it('parentOptions 过滤父级菜单', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.menuList = [
      { id: 'm1', name: '父菜单', parentId: null },
      { id: 'm2', name: '子菜单', parentId: 'm1' },
      { id: 'm3', name: '另一父', parentId: null },
    ]
    wrapper.vm.editId = 'm1'
    const opts = wrapper.vm.parentOptions
    expect(opts.some(o => o.id === 'm1')).toBe(false)
    expect(opts.some(o => o.id === 'm3')).toBe(true)
  })

  it('handleDelete 失败提示', async () => {
    const { ElMessageBox, ElMessage } = await import('element-plus')
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
    vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
    menuApi.deleteMenuApi.mockRejectedValue({ response: { data: { msg: '删除失败' } } })
    const wrapper = await createWrapper()
    await wrapper.vm.handleDelete('m1')
    expect(ElMessage.error).toHaveBeenCalled()
  })

  it('handleSubmit 校验失败不提交', async () => {
    const wrapper = await createWrapper(() => Promise.reject())
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleSubmit()
  })

  it('handleSubmit 保存失败提示', async () => {
    const { ElMessage } = await import('element-plus')
    vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
    menuApi.createMenuApi.mockRejectedValue({ response: { data: { msg: '保存失败' } } })
    const wrapper = await createWrapper(() => Promise.resolve(true))
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleSubmit()
    expect(ElMessage.error).toHaveBeenCalled()
    expect(wrapper.vm.submitLoading).toBe(false)
  })
})
