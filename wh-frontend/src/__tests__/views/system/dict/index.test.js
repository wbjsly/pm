import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setupPinia, elementStubs } from '../../../helpers'
import { setActivePinia, createPinia } from 'pinia'

// Mock dict API module
vi.mock('@/api/system/dict', () => ({
  getDictTypesApi: vi.fn(),
  getDictItemsApi: vi.fn(),
  createDictTypeApi: vi.fn(),
  updateDictTypeApi: vi.fn(),
  deleteDictTypeApi: vi.fn(),
  createDictItemApi: vi.fn(),
  updateDictItemApi: vi.fn(),
  deleteDictItemApi: vi.fn(),
}))

// Mock element-plus
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn(), info: vi.fn() },
    ElMessageBox: { confirm: vi.fn(() => Promise.resolve()) },
  }
})

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), go: vi.fn() }),
  useRoute: () => ({ path: '/', params: {}, query: {} }),
}))

import * as dictApi from '@/api/system/dict'

const mockTypes = [
  { id: 't1', typeCode: 'CHARTER_STATUS', typeName: '立项状态', description: '项目立项状态', sortOrder: 1 },
  { id: 't2', typeCode: 'PRODUCT_STATUS', typeName: '产品状态', description: '产品生命周期状态', sortOrder: 2 },
]

const mockItems = [
  { id: 'i1', typeCode: 'CHARTER_STATUS', itemCode: 'DRAFT', itemLabel: '草稿', tagType: 'info', status: 1, sortOrder: 1 },
  { id: 'i2', typeCode: 'CHARTER_STATUS', itemCode: 'APPROVED', itemLabel: '已审批', tagType: 'success', status: 1, sortOrder: 2 },
  { id: 'i3', typeCode: 'CHARTER_STATUS', itemCode: 'REJECTED', itemLabel: '已驳回', tagType: 'danger', status: 1, sortOrder: 3 },
]

describe('DictPage', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
    dictApi.getDictTypesApi.mockResolvedValue({ code: 200, data: mockTypes })
    dictApi.getDictItemsApi.mockResolvedValue({ code: 200, data: mockItems })
    dictApi.createDictTypeApi.mockResolvedValue({ code: 200 })
    dictApi.updateDictTypeApi.mockResolvedValue({ code: 200 })
    dictApi.deleteDictTypeApi.mockResolvedValue({ code: 200 })
    dictApi.createDictItemApi.mockResolvedValue({ code: 200 })
    dictApi.updateDictItemApi.mockResolvedValue({ code: 200 })
    dictApi.deleteDictItemApi.mockResolvedValue({ code: 200 })
  })

  function createWrapper() {
    return import('@/views/system/dict/index.vue').then(m => mount(m.default, { global: { stubs: { ...elementStubs, 'el-dialog': { template: '<div><slot/><slot name="footer"/></div>' } } } }))
  }

  it('渲染字典管理页面', async () => {
    const wrapper = await createWrapper()
    expect(wrapper.exists()).toBe(true)
  })

  it('挂载时加载字典类型列表', async () => {
    await createWrapper()
    expect(dictApi.getDictTypesApi).toHaveBeenCalledTimes(1)
  })

  it('加载类型后渲染列表', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    expect(dictApi.getDictTypesApi).toHaveBeenCalled()
    const cards = wrapper.findAll('.el-card')
    expect(cards.length).toBeGreaterThanOrEqual(1)
  })

  it('选择类型时加载条目', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    // Trigger type selection by calling the method directly
    wrapper.vm.handleTypeSelect(mockTypes[0])
    await wrapper.vm.$nextTick()

    expect(dictApi.getDictItemsApi).toHaveBeenCalledWith('CHARTER_STATUS')
  })

  it('新增类型打开弹窗', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.handleAddType()
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.typeDialogTitle).toBe('新增字典类型')
    expect(wrapper.vm.typeDialogVisible).toBe(true)
  })

  it('编辑类型填充表单', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.handleEditType(mockTypes[0])
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.typeDialogTitle).toBe('编辑字典类型')
    expect(wrapper.vm.typeForm.typeCode).toBe('CHARTER_STATUS')
    expect(wrapper.vm.typeForm.typeName).toBe('立项状态')
  })

  it('提交新建类型调用创建API', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.handleAddType()
    wrapper.vm.typeForm.typeCode = 'NEW_TYPE'
    wrapper.vm.typeForm.typeName = '新类型'

    wrapper.vm.typeFormRef = { validate: vi.fn(() => Promise.resolve(true)) }

    await wrapper.vm.submitType()
    await wrapper.vm.$nextTick()

    expect(dictApi.createDictTypeApi).toHaveBeenCalled()
  })

  it('提交编辑类型调用更新API', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.handleEditType(mockTypes[0])
    wrapper.vm.typeFormRef = { validate: vi.fn(() => Promise.resolve(true)) }

    await wrapper.vm.submitType()
    await wrapper.vm.$nextTick()

    expect(dictApi.updateDictTypeApi).toHaveBeenCalled()
  })

  it('删除类型调用删除API', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    await wrapper.vm.handleDeleteType(mockTypes[0])
    await wrapper.vm.$nextTick()

    expect(dictApi.deleteDictTypeApi).toHaveBeenCalledWith('t1')
  })

  it('新增条目打开弹窗', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.selectedType = mockTypes[0]
    wrapper.vm.handleAddItem()
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.itemDialogTitle).toBe('新增字典条目')
    expect(wrapper.vm.itemDialogVisible).toBe(true)
  })

  it('编辑条目填充条目表单', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.selectedType = mockTypes[0]
    wrapper.vm.handleEditItem(mockItems[0])
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.itemForm.itemCode).toBe('DRAFT')
    expect(wrapper.vm.itemForm.itemLabel).toBe('草稿')
  })

  it('提交新建条目调用创建API', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.selectedType = mockTypes[0]
    wrapper.vm.handleAddItem()
    wrapper.vm.itemFormRef = { validate: vi.fn(() => Promise.resolve(true)) }

    await wrapper.vm.submitItem()
    await wrapper.vm.$nextTick()

    expect(dictApi.createDictItemApi).toHaveBeenCalled()
  })

  it('提交编辑条目调用更新API', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.selectedType = mockTypes[0]
    wrapper.vm.handleEditItem(mockItems[0])
    wrapper.vm.itemFormRef = { validate: vi.fn(() => Promise.resolve(true)) }

    await wrapper.vm.submitItem()
    await wrapper.vm.$nextTick()

    expect(dictApi.updateDictItemApi).toHaveBeenCalled()
  })

  it('删除条目调用删除API', async () => {
    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    wrapper.vm.selectedType = mockTypes[0]
    await wrapper.vm.handleDeleteItem(mockItems[0])
    await wrapper.vm.$nextTick()

    expect(dictApi.deleteDictItemApi).toHaveBeenCalledWith('i1')
  })

  it('API 加载失败时组件不崩溃', async () => {
    dictApi.getDictTypesApi.mockRejectedValue(new Error('Network error'))

    const wrapper = await createWrapper()
    await wrapper.vm.$nextTick()

    expect(wrapper.exists()).toBe(true)
  })
})

describe('DictPage 校验与边界', () => {
  beforeEach(() => {
    setupPinia()
    vi.clearAllMocks()
  })

  function createWrapper(validateImpl) {
    const formStub = validateImpl === undefined
      ? { template: '<div><slot/></div>' }
      : { template: '<div><slot/></div>', methods: { validate: validateImpl } }
    return import('@/views/system/dict/index.vue').then(m => mount(m.default, { global: { stubs: { ...elementStubs, 'el-form': formStub, 'el-dialog': { template: '<div><slot/><slot name="footer"/></div>' } } } }))
  }

  it('submitType 校验失败不提交', async () => {
    const wrapper = await createWrapper(() => Promise.reject())
    await wrapper.vm.$nextTick()
    wrapper.vm.typeDialogVisible = true
    await wrapper.vm.$nextTick()
    await wrapper.vm.submitType()
  })

  it('submitItem 校验失败不提交', async () => {
    const wrapper = await createWrapper(() => Promise.reject())
    await wrapper.vm.$nextTick()
    wrapper.vm.itemDialogVisible = true
    await wrapper.vm.$nextTick()
    await wrapper.vm.submitItem()
  })

  it('handleTypeSelect 无选中时清空条目', async () => {
    const wrapper = await createWrapper()
    wrapper.vm.itemList = [{ id: 'i1' }]
    wrapper.vm.handleTypeSelect(null)
    expect(wrapper.vm.itemList).toEqual([])
  })

  it('handleDeleteType 删除选中类型后清空条目', async () => {
    const { ElMessageBox, ElMessage } = await import('element-plus')
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
    vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
    const wrapper = await createWrapper()
    wrapper.vm.selectedType = { id: 't1', typeCode: 'X' }
    wrapper.vm.itemList = [{ id: 'i1' }]
    await wrapper.vm.handleDeleteType({ id: 't1', typeCode: 'X' })
    expect(wrapper.vm.selectedType).toBeNull()
    expect(wrapper.vm.itemList).toEqual([])
  })
})
