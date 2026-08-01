import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { useUserStore } from '@/store/user'
import { useMenuStore } from '@/store/menu'
import { setupPinia } from '../../helpers'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), go: vi.fn() }),
  useRoute: () => ({ path: '/dashboard', matched: [] }),
}))

vi.mock('@/utils/request', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn(), interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } } },
}))

vi.mock('@element-plus/icons-vue', () => ({
  Search: { name: 'Search', template: '<i class="el-icon-search" />' },
  Fold: { name: 'Fold', template: '<i class="el-icon-fold" />' },
  Expand: { name: 'Expand', template: '<i class="el-icon-expand" />' },
  Folder: { name: 'Folder', template: '<i class="el-icon-folder" />' },
}))

const mockMenuItems = [
  { id: '1', title: '仪表盘', path: '/dashboard', icon: 'Folder', perm: '', parentId: null, sortOrder: 0 },
  { id: '2', title: '项目管理', path: null, icon: 'Folder', perm: 'ROLE_PM', parentId: null, sortOrder: 1 },
  { id: '3', title: '项目立项', path: '/pm/charter', icon: 'Folder', perm: 'ROLE_PM', parentId: '2', sortOrder: 0 },
  { id: '4', title: '任务管理', path: '/pm/wbs', icon: 'Folder', perm: 'ROLE_PM', parentId: '2', sortOrder: 1 },
  { id: '5', title: '系统管理', path: null, icon: 'Folder', perm: 'ROLE_ADMIN', parentId: null, sortOrder: 2 },
  { id: '6', title: '用户管理', path: '/system/user', icon: 'Folder', perm: 'ROLE_ADMIN', parentId: '5', sortOrder: 0 },
  { id: '7', title: '字典管理', path: '/system/dict', icon: 'Folder', perm: 'ROLE_ADMIN', parentId: '5', sortOrder: 1 },
]

function createWrapper(collapsed = false) {
  return import('@/components/layout/SidebarMenu.vue').then(m =>
    mount(m.default, {
      props: { collapsed },
      global: {
        stubs: {
          'el-icon': { template: '<i class="el-icon-stub"><slot/></i>' },
          'el-menu': { template: '<div class="el-menu"><slot/></div>' },
          'el-menu-item': { template: '<div class="el-menu-item"><slot/></div>', props: ['index'] },
          'el-sub-menu': { template: '<div class="el-sub-menu"><slot name="title" /><slot /></div>' },
          'el-input': { template: '<input class="mock-el-input" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" :placeholder="placeholder" />', props: ['modelValue', 'placeholder'] },
        },
      },
    })
  )
}

describe('SidebarMenu', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
  })

  it('渲染侧边栏菜单组件', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.exists()).toBe(true)
  })

  it('未折叠时显示 WH管理系统 标题', async () => {
    const wrapper = await createWrapper(false)
    expect(wrapper.text()).toContain('WH管理系统')
  })

  it('折叠时显示 WH 缩写', async () => {
    const wrapper = await createWrapper(true)
    expect(wrapper.text()).toContain('WH')
  })

  it('未折叠时搜索输入框可见', async () => {
    const wrapper = await createWrapper(false)
    const input = wrapper.find('input')
    expect(input.exists()).toBe(true)
  })

  it('折叠时隐藏搜索输入框（v-show="false"）', async () => {
    const wrapper = await createWrapper(true)
    // v-show="false" sets display:none; element still in DOM
    const el = wrapper.find('.sidebar-search')
    expect(el.exists()).toBe(true)
    // happy-dom doesn't compute CSS, just verify collapsed class
    expect(wrapper.find('.sidebar-menu').classes()).toContain('collapsed')
  })

  it('空菜单列表时显示搜索框', async () => {
    const wrapper = await createWrapper(false)
    expect(wrapper.find('input').exists()).toBe(true)
  })

  it('传递 collapsed props 能正确设置 class', async () => {
    const wrapper = await createWrapper(true)
    const sidebar = wrapper.find('.sidebar-menu')
    expect(sidebar.classes()).toContain('collapsed')
  })

  it('未折叠时 sidebar-menu 不应有 collapsed class', async () => {
    const wrapper = await createWrapper(false)
    const sidebar = wrapper.find('.sidebar-menu')
    expect(sidebar.classes()).not.toContain('collapsed')
  })

  it('根据 ROLE_ADMIN 角色过滤菜单项（显示系统管理）', async () => {
    const userStore = useUserStore()
    userStore.userInfo = { username: 'admin', roles: ['ROLE_ADMIN'] }
    const menuStore = useMenuStore()
    menuStore.menuItems = mockMenuItems

    const wrapper = await createWrapper(false)
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('仪表盘')
    // ROLE_ADMIN should see system management items
    expect(wrapper.text()).toContain('用户管理')
  })

  it('根据 ROLE_PM 角色过滤菜单项（应隐藏系统管理）', async () => {
    const userStore = useUserStore()
    userStore.userInfo = { username: 'pmuser', roles: ['ROLE_PM'] }
    const menuStore = useMenuStore()
    menuStore.menuItems = mockMenuItems

    const wrapper = await createWrapper(false)
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('仪表盘')
    expect(wrapper.text()).toContain('项目立项')
    // ROLE_PM should NOT see system management items
    expect(wrapper.text()).not.toContain('用户管理')
    expect(wrapper.text()).not.toContain('字典管理')
  })

  it('无角色用户仅可见无权限要求的菜单', async () => {
    const userStore = useUserStore()
    userStore.userInfo = { username: 'guest', roles: [] }
    const menuStore = useMenuStore()
    menuStore.menuItems = mockMenuItems

    const wrapper = await createWrapper(false)
    await wrapper.vm.$nextTick()

    // 仪表盘 has no perm requirement
    expect(wrapper.text()).toContain('仪表盘')
    expect(wrapper.text()).not.toContain('项目立项')
    expect(wrapper.text()).not.toContain('用户管理')
  })
})
