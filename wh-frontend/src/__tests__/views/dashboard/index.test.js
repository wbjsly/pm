import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import DashboardPage from '@/views/dashboard/index.vue'

import { createRouter, createMemoryHistory } from 'vue-router' 

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ code: 200, data: {} })),
    post: vi.fn(() => Promise.resolve({ code: 200 })),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}))

import request from '@/utils/request'

describe('DashboardPage', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  function createWrapper() {
    return mount(DashboardPage, {
      global: {
        stubs: {
          'router-link': true,
          'el-card': { template: '<div class="el-card"><slot name="header"/><slot/></div>' },
          'el-row': { template: '<div class="el-row"><slot/></div>' },
          'el-col': { template: '<div class="el-col"><slot/></div>' },
          'el-table': true,
          'el-table-column': true,
          'el-tag': true,
          'el-button': { template: '<button><slot/></button>' },
          'el-icon': { template: '<i/>' },
        },
      },
    })
  }

  it('渲染仪表盘页面', async () => {
    const wrapper = createWrapper()
    expect(wrapper.exists()).toBe(true)
  })

  it('显示项目统计数据区域', () => {
    const wrapper = createWrapper()
    expect(wrapper.text().length).toBeGreaterThan(0)
  })

  it('用户未登录时不加载统计', async () => {
    const wrapper = createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    expect(request.get).not.toHaveBeenCalled()
  })

  it('用户已登录时加载统计/预警/工时', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const { useUserStore } = await import('@/store/user')
    const store = useUserStore()
    store.userInfo = { userId: 'pm_001' }
    request.get.mockResolvedValue({ code: 200, data: { total: 5, draft: 1, pending: 2, approved: 2, rejected: 0 } })

    const wrapper = mount(DashboardPage, {
      global: {
        stubs: {
          'router-link': true,
          'el-card': { template: '<div class="el-card"><slot name="header"/><slot/></div>' },
          'el-row': { template: '<div class="el-row"><slot/></div>' },
          'el-col': { template: '<div class="el-col"><slot/></div>' },
          'el-table': true,
          'el-table-column': true,
          'el-tag': true,
          'el-button': { template: '<button><slot/></button>' },
          'el-icon': { template: '<i/>' },
        },
      },
    })
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    expect(request.get).toHaveBeenCalled()
    expect(wrapper.vm.stats.total).toBe(5)
    expect(wrapper.vm.statCards[0].value).toBe(5)
  })
})

describe('DashboardPage 函数', () => {
  let routerMock
  let userStore

  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  function createRealRouter() {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div/>' } }],
    })
    router.push = vi.fn()
    router.replace = vi.fn()
    return router
  }

  async function createWrapperWithUser() {
    const { useUserStore } = await import('@/store/user')
    userStore = useUserStore()
    userStore.userInfo = { userId: 'pm_001' }
    routerMock = createRealRouter()
    return mount(DashboardPage, {
      global: {
        plugins: [routerMock],
        stubs: {
          'router-link': true,
          'el-card': { template: '<div class="el-card"><slot name="header"/><slot/></div>' },
          'el-row': { template: '<div class="el-row"><slot/></div>' },
          'el-col': { template: '<div class="el-col"><slot/></div>' },
          'el-table': true,
          'el-table-column': true,
          'el-tag': true,
          'el-button': { template: '<button><slot/></button>' },
          'el-icon': { template: '<i/>' },
        },
      },
    })
  }

  it('loadWarnings 加载成本预警', async () => {
    request.get.mockResolvedValue({ code: 200, data: [{ id: 'w1', level: 'WARN' }] })
    const wrapper = await createWrapperWithUser()
    await wrapper.vm.loadWarnings()
    expect(wrapper.vm.costWarnings.length).toBe(1)
  })

  it('loadHoursStats 加载工时统计', async () => {
    request.get.mockResolvedValue({ code: 200, data: { filledDays: 5 } })
    const wrapper = await createWrapperWithUser()
    await wrapper.vm.loadHoursStats()
    expect(wrapper.vm.hoursStats.filledDays).toBe(5)
  })

  it('handleStatClick 有状态跳转带 query', async () => {
    const wrapper = await createWrapperWithUser()
    wrapper.vm.handleStatClick({ status: 'DRAFT' })
    expect(routerMock.push).toHaveBeenCalledWith({ path: '/pm/charter', query: { status: 'DRAFT' } })
  })

  it('handleStatClick 无状态跳转立项列表', async () => {
    const wrapper = await createWrapperWithUser()
    wrapper.vm.handleStatClick({ status: null })
    expect(routerMock.push).toHaveBeenCalledWith('/pm/charter')
  })

  it('handleViewProject / handleNewProject / handleGoProjects / handleViewWarnings / handleGoWorkHours', async () => {
    const wrapper = await createWrapperWithUser()
    wrapper.vm.handleViewProject({ id: 'p1' })
    expect(routerMock.push).toHaveBeenCalledWith('/pm/charter/detail/p1')
    wrapper.vm.handleNewProject()
    expect(routerMock.push).toHaveBeenCalledWith('/pm/charter/form')
    wrapper.vm.handleGoProjects()
    expect(routerMock.push).toHaveBeenCalledWith('/pm/charter')
    wrapper.vm.handleViewWarnings()
    expect(routerMock.push).toHaveBeenCalledWith('/pm/budget')
    wrapper.vm.handleGoWorkHours()
    expect(routerMock.push).toHaveBeenCalledWith('/pm/work-hours')
  })
})
