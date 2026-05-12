import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setupPinia, routerStubs, elementStubs } from '../../../helpers'

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ code: 200, data: { records: [], total: 0 } })),
    post: vi.fn(() => Promise.resolve({ code: 200 })),
    put: vi.fn(() => Promise.resolve({ code: 200 })),
    delete: vi.fn(() => Promise.resolve({ code: 200 })),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}))

describe('WorkHoursApprovalPage', () => {
  beforeEach(() => setupPinia())

  function createWrapper() {
    return import('@/views/pm/work-hours/approval.vue').then(module =>
      mount(module.default, {
        global: {
          provide: routerStubs.provide,
          stubs: elementStubs,
        },
      })
    )
  }

  it('渲染审批页面', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.exists()).toBe(true)
  })
})
