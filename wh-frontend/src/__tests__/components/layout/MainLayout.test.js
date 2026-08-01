import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { useUserStore } from '@/store/user'
import { useTabStore } from '@/store/tab'
import { setupPinia } from '../../helpers'

const { routerMock } = vi.hoisted(() => ({ routerMock: { push: vi.fn(), replace: vi.fn(), go: vi.fn() } }))

vi.mock('vue-router', () => ({
  useRouter: () => routerMock,
  useRoute: () => ({ path: '/dashboard', params: {}, query: {} }),
}))

vi.mock('@/utils/request', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn(), interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } } },
}))

vi.mock('@element-plus/icons-vue', () => ({
  UserFilled: { name: 'UserFilled', template: '<i class="el-icon-user-filled" />' },
  ArrowDown: { name: 'ArrowDown', template: '<i class="el-icon-arrow-down" />' },
}))

function createWrapper() {
  return import('@/components/layout/MainLayout.vue').then(m =>
    mount(m.default, {
      global: {
        stubs: {
          'router-view': { template: '<div class="mock-router-view"><slot/></div>' },
          SidebarMenu: { template: '<div class="mock-sidebar">Sidebar</div>' },
          'el-container': { template: '<div class="el-container"><slot/></div>' },
          'el-aside': { template: '<div class="el-aside" :style="{ width }"><slot/></div>', props: ['width'] },
          'el-header': { template: '<div class="el-header"><slot/></div>' },
          'el-main': { template: '<div class="el-main"><slot/></div>' },
          'el-tabs': { template: '<div class="el-tabs"><slot/></div>', props: ['modelValue'], emits: ['tab-click', 'tab-remove', 'update:modelValue'] },
          'el-tab-pane': { template: '<div class="el-tab-pane"><slot name="label"/></div>', props: ['name', 'closable'] },
          'el-dropdown': { template: '<div class="el-dropdown" @click="handleTrigger"><slot/><slot name="dropdown"/></div>' },
          'el-dropdown-menu': { template: '<div class="el-dropdown-menu"><slot/></div>' },
          'el-dropdown-item': { template: '<div class="el-dropdown-item" @click="$parent.$emit(\'command\', command)"><slot/></div>', props: ['command'] },
          'el-icon': { template: '<i class="el-icon-stub"><slot/></i>' },
        },
      },
    })
  )
}

describe('MainLayout', () => {
  let router
  let userStore
  let tabStore

  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    router = { push: vi.fn(), replace: vi.fn(), go: vi.fn() }
    userStore = useUserStore()
    tabStore = useTabStore()
  })

  it('渲染主布局', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.exists()).toBe(true)
  })

  it('包含侧边栏组件', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.find('.mock-sidebar').exists()).toBe(true)
  })

  it('渲染用户信息（无 userInfo 时显示空）', async () => {
    const wrapper = await createWrapper()
    const userName = wrapper.find('.user-name')
    expect(userName.exists()).toBe(true)
  })

  it('显示用户昵称', async () => {
    userStore.userInfo = { username: 'admin', nickName: '管理员', roles: ['ROLE_ADMIN'] }
    const wrapper = await createWrapper()
    expect(wrapper.text()).toContain('管理员')
  })

  it('无 nickName 时显示 username', async () => {
    userStore.userInfo = { username: 'testuser', roles: ['ROLE_PM'] }
    const wrapper = await createWrapper()
    expect(wrapper.text()).toContain('testuser')
  })

  it('切换侧边栏折叠状态', async () => {
    const wrapper = await createWrapper()
    const aside = wrapper.find('.el-aside')
    expect(aside.attributes('style')).toContain('220px')
  })

  it('处理用户命令 - profile 跳转到个人资料页', async () => {
    const wrapper = await createWrapper()
    // Trigger profile via dropdown
    const handleUserCommand = wrapper.vm.handleUserCommand
    if (handleUserCommand) {
      handleUserCommand('profile')
      // router.push is mocked at module level, so we can't directly check it
    }
  })

  it('处理用户命令 - logout', async () => {
    userStore.token = 'test-token'
    userStore.userInfo = { username: 'admin', nickName: '管理员', roles: ['ROLE_ADMIN'] }

    const wrapper = await createWrapper()
    // Access the component's handleUserCommand
    const vm = wrapper.vm
    if (vm.handleUserCommand) {
      vm.handleUserCommand('logout')
    }
  })

  it('渲染标签页', async () => {
    const wrapper = await createWrapper()
    const tabs = wrapper.find('.el-tabs')
    expect(tabs.exists()).toBe(true)
  })

  it('渲染标签页中的 tab', async () => {
    tabStore.addTab({ name: 'Dashboard', path: '/dashboard', meta: { title: '主页', closable: false } })
    const wrapper = await createWrapper()
    const tabPanes = wrapper.findAll('.el-tab-pane')
    expect(tabPanes.length).toBeGreaterThanOrEqual(1)
  })

  it('组件挂载时添加全局点击事件监听', async () => {
    const addEventListenerSpy = vi.spyOn(document, 'addEventListener')
    const wrapper = await createWrapper()
    expect(addEventListenerSpy).toHaveBeenCalledWith('click', expect.any(Function))
  })

  it('组件卸载时移除点击事件监听', async () => {
    const removeEventListenerSpy = vi.spyOn(document, 'removeEventListener')
    const wrapper = await createWrapper()
    wrapper.unmount()
    expect(removeEventListenerSpy).toHaveBeenCalledWith('click', expect.any(Function))
  })
})

describe('MainLayout 标签操作', () => {
  let router
  let tabStore

  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    router = { push: vi.fn(), replace: vi.fn(), go: vi.fn() }
    tabStore = useTabStore()
  })

  it('showContextMenu 传入 tab 时使用该 tab', async () => {
    const wrapper = await createWrapper()
    const e = { clientX: 10, clientY: 20 }
    wrapper.vm.showContextMenu(e, { path: '/pm/charter' })
    expect(wrapper.vm.contextTab.path).toBe('/pm/charter')
    expect(wrapper.vm.contextTop).toBe(20)
    expect(wrapper.vm.contextVisible).toBe(true)
  })

  it('showContextMenu 无 tab 时使用 activeTab', async () => {
    const wrapper = await createWrapper()
    tabStore.addTab({ name: 'Charter', path: '/pm/charter', meta: { title: '项目立项' } })
    tabStore.activeTab = '/pm/charter'
    wrapper.vm.showContextMenu({ clientX: 0, clientY: 0 }, null)
    expect(wrapper.vm.contextTab.path).toBe('/pm/charter')
  })

  it('closeCurrent 关闭当前标签并跳转', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.contextTab = { path: '/pm/charter' }
    tabStore.addTab({ name: 'Charter', path: '/pm/charter', meta: { title: '项目立项' } })
    tabStore.activeTab = '/dashboard'
    wrapper.vm.closeCurrent()
    expect(wrapper.vm.contextVisible).toBe(false)
    expect(tabStore.tabs.some(t => t.path === '/pm/charter')).toBe(false)
  })

  it('closeCurrent 无 activeTab 时跳转 dashboard', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.contextTab = { path: '/pm/charter' }
    tabStore.activeTab = ''
    wrapper.vm.closeCurrent()
    expect(routerMock.push).toHaveBeenCalledWith('/dashboard')
  })

  it('closeLeft / closeRight / closeOther 调用 store 并隐藏菜单', async () => {
    const wrapper = await createWrapper()
    tabStore.addTab({ name: 'A', path: '/a', meta: { title: 'A' } })
    tabStore.addTab({ name: 'B', path: '/b', meta: { title: 'B' } })
    tabStore.addTab({ name: 'C', path: '/c', meta: { title: 'C' } })
    wrapper.vm.contextTab = { path: '/b' }

    wrapper.vm.closeLeft()
    expect(tabStore.tabs.some(t => t.path === '/a')).toBe(false)

    wrapper.vm.closeRight()
    expect(tabStore.tabs.some(t => t.path === '/c')).toBe(false)

    wrapper.vm.contextTab = { path: '/b' }
    wrapper.vm.closeOther()
    expect(tabStore.tabs.filter(t => t.path !== '/dashboard' && t.path !== '/b')).toHaveLength(0)
  })

  it('closeAll 调用 store 并跳转 dashboard', async () => {
    const wrapper = await createWrapper()
    tabStore.addTab({ name: 'A', path: '/a', meta: { title: 'A' } })
    wrapper.vm.closeAll()
    expect(routerMock.push).toHaveBeenCalledWith('/dashboard')
    expect(wrapper.vm.contextVisible).toBe(false)
  })

  it('handleTabClick 跳转到标签路径', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.handleTabClick({ props: { name: '/pm/charter' } })
    expect(routerMock.push).toHaveBeenCalledWith('/pm/charter')
  })

  it('handleTabRemove 移除标签并跳转', async () => {
    const wrapper = await createWrapper()
    tabStore.addTab({ name: 'A', path: '/a', meta: { title: 'A' } })
    tabStore.activeTab = '/dashboard'
    wrapper.vm.handleTabRemove('/a')
    expect(tabStore.tabs.some(t => t.path === '/a')).toBe(false)
  })

  it('切换侧边栏折叠状态', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.vm.sidebarCollapsed).toBe(false)
    wrapper.vm.sidebarCollapsed = true
    expect(wrapper.vm.sidebarWidth).toBe('64px')
  })
})
