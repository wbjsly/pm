import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setupPinia, routerStubs, elementStubs } from '../../helpers'
import LoginPage from '@/views/login/index.vue'

describe('LoginPage', () => {
  beforeEach(() => setupPinia())

  function createWrapper() {
    return mount(LoginPage, {
      global: {
        provide: routerStubs.provide,
        stubs: { 'router-link': true, ...elementStubs },
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
})
