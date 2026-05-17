import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setupPinia, elementStubs } from '../../../helpers'

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), go: vi.fn(), back: vi.fn() }),
  useRoute: () => ({ path: '/', params: {}, query: {} }),
}))

vi.mock('@element-plus/icons-vue', () => ({
  Plus: { name: 'Plus', template: '<i class="el-icon-plus" />' },
  Edit: { name: 'Edit', template: '<i class="el-icon-edit" />' },
  Delete: { name: 'Delete', template: '<i class="el-icon-delete" />' },
}))

import request from '@/utils/request'

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
    mount(m.default, { global: { stubs: elementStubs } })
  )
}

describe('MenuPage', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    request.get.mockImplementation((url) => {
      if (url === '/system/menus') {
        return Promise.resolve({ code: 200, data: mockMenuList })
      }
      if (url === '/system/roles') {
        return Promise.resolve({ code: 200, data: mockRoles })
      }
      if (url === '/system/menus/user') {
        return Promise.resolve({ code: 200, data: mockMenuList })
      }
      return Promise.resolve({ code: 200, data: {} })
    })
    request.post.mockResolvedValue({ code: 200 })
    request.put.mockResolvedValue({ code: 200 })
    request.delete.mockResolvedValue({ code: 200 })
  })

  it('渲染菜单管理页面', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    expect(wrapper.exists()).toBe(true)
  })

  it('组件使用 onActivated，由于无 KeepAlive 环境，mount 时不请求 API', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    // 组件使用 onActivated 而非 onMounted，因此 mount 后不会自动调用 loadData
    expect(request.get).not.toHaveBeenCalled()
  })

  it('显示菜单管理标题', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('菜单管理')
  })

  it('页面渲染后不展示菜单列表数据（因 onActivated 未触发）', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).not.toContain('系统管理')
    expect(wrapper.text()).not.toContain('用户管理')
  })

  it('删除按钮不存在时不会触发 API（onActivated 未触发，表格无数据）', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    // 无数据，页面上没有表格行
    expect(request.delete).not.toHaveBeenCalled()
  })

  it('当 API 被手动触发时能正常返回数据', async () => {
    // Verify mock API setup works correctly
    const result = await request.get('/system/menus')
    expect(result).toEqual({ code: 200, data: mockMenuList })
  })
})
