import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import DashboardPage from '@/views/dashboard/index.vue'

describe('DashboardPage', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
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

  it('渲染仪表盘页面', () => {
    const wrapper = createWrapper()
    expect(wrapper.exists()).toBe(true)
  })

  it('显示项目统计数据区域', () => {
    const wrapper = createWrapper()
    expect(wrapper.text().length).toBeGreaterThan(0)
  })
})
