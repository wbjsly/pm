import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setupPinia, elementStubs } from '../../../helpers'
import { setActivePinia, createPinia } from 'pinia'

vi.mock('@/api/pm/wbs', () => ({
  getProductListApi: vi.fn(),
  createProductApi: vi.fn(),
  updateProductApi: vi.fn(),
  deleteProductApi: vi.fn(),
  getModulesByProductApi: vi.fn(),
  createModuleApi: vi.fn(),
  updateModuleApi: vi.fn(),
  deleteModuleApi: vi.fn(),
}))

vi.mock('@element-plus/icons-vue', () => ({
  Plus: { name: 'Plus', template: '<i class="el-icon-plus" />' },
  Edit: { name: 'Edit', template: '<i class="el-icon-edit" />' },
  Delete: { name: 'Delete', template: '<i class="el-icon-delete" />' },
  ArrowRight: { name: 'ArrowRight', template: '<i class="el-icon-arrow-right" />' },
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn(), info: vi.fn() },
    ElMessageBox: { confirm: vi.fn(() => Promise.resolve()) },
  }
})

import * as wbsApi from '@/api/pm/wbs'

const mockProducts = {
  records: [
    { id: 'p1', productCode: 'PROD-001', productName: '项目管理平台', productVersion: '1.0.0', status: 'ACTIVE', versionReleaseDate: '2025-01-01', description: '主平台', moduleCount: 3 },
    { id: 'p2', productCode: 'PROD-002', productName: '即时通讯模块', productVersion: '2.0.0', status: 'ACTIVE', versionReleaseDate: '2025-02-01', description: '通讯模块', moduleCount: 2 },
  ],
  total: 2,
}

const mockModules = [
  { id: 'm1', productId: 'p1', moduleCode: 'MOD-001', moduleName: '用户管理', status: 'ACTIVE', description: '用户模块' },
  { id: 'm2', productId: 'p1', moduleCode: 'MOD-002', moduleName: '权限管理', status: 'ACTIVE', description: '权限模块' },
]

describe('ProductPage', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    wbsApi.getProductListApi.mockResolvedValue({ code: 200, data: mockProducts })
    wbsApi.getModulesByProductApi.mockResolvedValue({ code: 200, data: mockModules })
    wbsApi.createProductApi.mockResolvedValue({ code: 200 })
    wbsApi.updateProductApi.mockResolvedValue({ code: 200 })
    wbsApi.deleteProductApi.mockResolvedValue({ code: 200 })
    wbsApi.createModuleApi.mockResolvedValue({ code: 200 })
    wbsApi.updateModuleApi.mockResolvedValue({ code: 200 })
    wbsApi.deleteModuleApi.mockResolvedValue({ code: 200 })
  })

  function createWrapper() {
    return import('@/views/pm/product/index.vue').then(m =>
      mount(m.default, { global: { stubs: { ...elementStubs, 'el-dialog': { template: '<div><slot/><slot name="footer"/></div>' }, 'el-pagination': { template: '<div><slot/></div>' } } } })
    )
  }

  it('渲染产品列表页面', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.exists()).toBe(true)
  })

  it('挂载时加载产品列表', async () => {
    await createWrapper()
    await new Promise(r => setTimeout(r, 50))
    expect(wbsApi.getProductListApi).toHaveBeenCalled()
  })

  it('加载后产品数据展示', async () => {
    const wrapper = await createWrapper()
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.productList.length).toBe(2)
  })

  it('展开产品加载模块', async () => {
    const wrapper = await createWrapper()
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()

    await wrapper.vm.handleExpandProduct('p1')
    await wrapper.vm.$nextTick()

    expect(wbsApi.getModulesByProductApi).toHaveBeenCalledWith('p1')
  })

  it('展开状态下点击收起', async () => {
    const wrapper = await createWrapper()
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()

    wrapper.vm.expandedProductId = 'p1'
    wrapper.vm.moduleList = mockModules
    wrapper.vm.toggleProduct('p1')
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.expandedProductId).toBeNull()
  })

  it('新增产品打开弹窗', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.handleAdd()
    expect(wrapper.vm.formVisible).toBe(true)
    expect(wrapper.vm.formTitle).toBe('新增产品')
  })

  it('编辑产品填充表单', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.handleEdit(mockProducts.records[0])
    expect(wrapper.vm.formVisible).toBe(true)
    expect(wrapper.vm.editId).toBe('p1')
    expect(wrapper.vm.form.productCode).toBe('PROD-001')
  })

  it('提交新建产品', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.handleAdd()
    wrapper.vm.form.productCode = 'PROD-003'
    wrapper.vm.form.productName = '新产品'
    wrapper.vm.formRef = { validate: vi.fn(() => Promise.resolve(true)) }

    await wrapper.vm.handleSubmit()
    await wrapper.vm.$nextTick()

    expect(wbsApi.createProductApi).toHaveBeenCalled()
  })

  it('提交编辑产品', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.handleEdit(mockProducts.records[0])
    wrapper.vm.formRef = { validate: vi.fn(() => Promise.resolve(true)) }

    await wrapper.vm.handleSubmit()
    await wrapper.vm.$nextTick()

    expect(wbsApi.updateProductApi).toHaveBeenCalled()
  })

  it('删除产品', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    await wrapper.vm.handleDelete('p1')
    await wrapper.vm.$nextTick()

    expect(wbsApi.deleteProductApi).toHaveBeenCalledWith('p1')
  })

  it('新增模块打开弹窗', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.handleAddModule('p1', '项目管理平台')
    expect(wrapper.vm.moduleFormVisible).toBe(true)
    expect(wrapper.vm.moduleFormTitle).toBe('新增模块')
  })

  it('编辑模块填充表单', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.handleEditModule({ ...mockModules[0], productId: 'p1' })
    expect(wrapper.vm.moduleFormVisible).toBe(true)
    expect(wrapper.vm.moduleForm.moduleCode).toBe('MOD-001')
  })

  it('提交新建模块', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.handleAddModule('p1', '项目管理平台')
    wrapper.vm.moduleForm.moduleCode = 'MOD-003'
    wrapper.vm.moduleForm.moduleName = '新模块'
    wrapper.vm.moduleFormRef = { validate: vi.fn(() => Promise.resolve(true)) }

    await wrapper.vm.handleModuleSubmit()
    await wrapper.vm.$nextTick()

    expect(wbsApi.createModuleApi).toHaveBeenCalled()
  })

  it('提交编辑模块', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.handleEditModule({ ...mockModules[0], productId: 'p1' })
    wrapper.vm.moduleFormRef = { validate: vi.fn(() => Promise.resolve(true)) }

    await wrapper.vm.handleModuleSubmit()
    await wrapper.vm.$nextTick()

    expect(wbsApi.updateModuleApi).toHaveBeenCalled()
  })

  it('删除模块', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    await wrapper.vm.handleDeleteModule('m1')
    await wrapper.vm.$nextTick()

    expect(wbsApi.deleteModuleApi).toHaveBeenCalledWith('m1')
  })

  it('关键字查询', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.keyword = '项目'
    wrapper.vm.loadData()
    await wrapper.vm.$nextTick()

    expect(wbsApi.getProductListApi).toHaveBeenCalled()
  })

  it('API 失败时不崩溃', async () => {
    wbsApi.getProductListApi.mockRejectedValue(new Error('Network error'))
    const wrapper = await createWrapper()
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.exists()).toBe(true)
  })

  it('分页参数正确', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.queryParams.pageNum).toBe(1)
    expect(wrapper.vm.queryParams.pageSize).toBe(5)
  })
})

describe('ProductPage 补充', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    wbsApi.getProductListApi.mockResolvedValue({ code: 200, data: mockProducts })
    wbsApi.getModulesByProductApi.mockResolvedValue({ code: 200, data: mockModules })
    wbsApi.deleteModuleApi.mockResolvedValue({ code: 200 })
  })

  function createWrapper(validateImpl) {
    const formStub = validateImpl === undefined
      ? { template: '<div><slot/></div>' }
      : { template: '<div><slot/></div>', methods: { validate: validateImpl } }
    return import('@/views/pm/product/index.vue').then(m =>
      mount(m.default, { global: { stubs: { ...elementStubs, 'el-form': formStub, 'el-dialog': { template: '<div><slot/><slot name="footer"/></div>' }, 'el-pagination': { template: '<div><slot/></div>' } } } })
    )
  }

  it('handleSubmit 校验失败不提交', async () => {
    const wrapper = await createWrapper(() => Promise.reject())
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleSubmit()
    expect(wbsApi.createProductApi).not.toHaveBeenCalled()
  })

  it('handleModuleSubmit 校验失败不提交', async () => {
    const wrapper = await createWrapper(() => Promise.reject())
    await wrapper.vm.$nextTick()
    wrapper.vm.moduleFormVisible = true
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleModuleSubmit()
    expect(wbsApi.createModuleApi).not.toHaveBeenCalled()
  })

  it('handleDeleteModule 确认后删除模块', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    wrapper.vm.moduleList = mockModules
    wrapper.vm.expandedProductId = 'p1'
    wrapper.vm.loadData = vi.fn()
    wrapper.vm.handleExpandProduct = vi.fn()
    await wrapper.vm.handleDeleteModule('m1')
    expect(wbsApi.deleteModuleApi).toHaveBeenCalledWith('m1')
  })

  it('formTitle / isEdit 计算属性', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.formTitle).toBe('新增产品')
    wrapper.vm.editId = 'p1'
    expect(wrapper.vm.formTitle).toBe('编辑产品')
  })

  it('moduleFormTitle / isModuleEdit 计算属性', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.moduleFormTitle).toBe('新增模块')
    wrapper.vm.moduleEditId = 'm1'
    expect(wrapper.vm.moduleFormTitle).toBe('编辑模块')
  })

  it('handleExpandProduct 加载模块失败提示', async () => {
    wbsApi.getModulesByProductApi.mockRejectedValue(new Error('fail'))
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleExpandProduct('p1')
    expect(wrapper.vm.moduleLoading).toBe(false)
  })

  it('toggleProduct 收起已展开产品', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    wrapper.vm.expandedProductId = 'p1'
    wrapper.vm.toggleProduct('p1')
    expect(wrapper.vm.expandedProductId).toBeNull()
  })
})

describe('ProductPage 失败分支', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    wbsApi.getProductListApi.mockResolvedValue({ code: 200, data: mockProducts })
    wbsApi.createProductApi.mockRejectedValue({ response: { data: { message: '失败' } } })
    wbsApi.createModuleApi.mockRejectedValue({ response: { data: { message: '失败' } } })
  })

  function createWrapper(validateImpl) {
    const formStub = validateImpl === undefined
      ? { template: '<div><slot/></div>' }
      : { template: '<div><slot/></div>', methods: { validate: validateImpl } }
    return import('@/views/pm/product/index.vue').then(m =>
      mount(m.default, { global: { stubs: { ...elementStubs, 'el-form': formStub, 'el-dialog': { template: '<div><slot/><slot name="footer"/></div>' }, 'el-pagination': { template: '<div><slot/></div>' } } } })
    )
  }

  it('handleSubmit 失败提示', async () => {
    const { ElMessage } = await import('element-plus')
    vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
    const wrapper = await createWrapper(() => Promise.resolve(true))
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleSubmit()
    expect(ElMessage.error).toHaveBeenCalled()
    expect(wrapper.vm.submitLoading).toBe(false)
  })

  it('handleModuleSubmit 失败提示', async () => {
    const { ElMessage } = await import('element-plus')
    vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
    const wrapper = await createWrapper(() => Promise.resolve(true))
    await wrapper.vm.$nextTick()
    wrapper.vm.moduleFormVisible = true
    await wrapper.vm.$nextTick()
    await wrapper.vm.handleModuleSubmit()
    expect(ElMessage.error).toHaveBeenCalled()
    expect(wrapper.vm.moduleSubmitLoading).toBe(false)
  })

  it('toggleProduct 收起已展开产品', async () => {
    wbsApi.createProductApi.mockResolvedValue({ code: 200 })
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()
    wrapper.vm.expandedProductId = 'p1'
    wrapper.vm.toggleProduct('p1')
    expect(wrapper.vm.expandedProductId).toBeNull()
    expect(wrapper.vm.moduleList).toEqual([])
  })
})
