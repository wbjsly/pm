<template>
  <div class="product-list">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span style="font-weight: bold; font-size: 16px;">产品清单</span>
          <div style="display: flex; align-items: center; gap: 8px;">
            <el-input v-model="keyword" placeholder="产品编码/名称" clearable style="width: 180px" @keyup.enter="loadData" />
            <el-button type="primary" @click="loadData">查询</el-button>
            <el-tooltip content="新增产品" placement="top">
              <el-button type="primary" @click="handleAdd">
                <el-icon><Plus /></el-icon>
              </el-button>
            </el-tooltip>
          </div>
        </div>
      </template>

      <!-- Product accordion list -->
      <div v-for="product in productList" :key="product.id" style="margin-bottom: 8px;">
        <div style="display: flex; align-items: center; padding: 12px 16px; background: #f5f7fa; cursor: pointer; border-radius: 4px;"
             @click="toggleProduct(product.id)">
          <el-icon style="transition: transform 0.2s; margin-right: 14px; flex-shrink: 0;" :style="{ transform: expandedProductId === product.id ? 'rotate(90deg)' : '' }">
            <ArrowRight />
          </el-icon>
          <span style="font-weight: bold; margin-right: 24px; font-size: 14px; flex-shrink: 0; min-width: 100px;">{{ product.productName }}</span>
          <el-tag size="small" style="flex-shrink: 0;">{{ product.productCode }}</el-tag>
          <el-tag size="small" :type="dictStore.getTagType('PRODUCT_STATUS', product.status)" style="margin-left: 16px; flex-shrink: 0;">{{ dictStore.getLabel('PRODUCT_STATUS', product.status) }}</el-tag>
          <el-tag v-if="product.productVersion" size="small" type="warning" style="margin-left: 16px; flex-shrink: 0;">v{{ product.productVersion }}</el-tag>
          <span v-if="product.versionReleaseDate" style="margin-left: 16px; color: #606266; font-size: 13px; white-space: nowrap; flex-shrink: 0;">上架日期: {{ product.versionReleaseDate.substring(0, 10) }}</span>
          <span style="margin-left: 16px; color: #409eff; font-size: 13px; white-space: nowrap; flex-shrink: 0;">模块: {{ product.moduleCount || 0 }}个</span>
          <span style="margin-left: auto; display: flex; gap: 4px;" v-if="expandedProductId === product.id">
            <el-tooltip content="编辑" placement="top">
              <el-button link type="primary" @click.stop="handleEdit(product)">
                <el-icon><Edit /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button link type="danger" @click.stop="handleDelete(product.id)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="添加模块" placement="top">
              <el-button link type="success" @click.stop="handleAddModule(product.id, product.productName)">
                <el-icon><Plus /></el-icon>
              </el-button>
            </el-tooltip>
          </span>
        </div>

        <!-- Module table for expanded product -->
        <div v-if="expandedProductId === product.id" style="padding: 12px 16px;">
          <el-table :data="moduleList" v-loading="moduleLoading" stripe>
            <el-table-column prop="moduleCode" label="模块编码" width="140" />
            <el-table-column prop="moduleName" label="模块名称" min-width="120" />
            <el-table-column prop="moduleVersion" label="模块版本" width="100" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag size="small" :type="dictStore.getTagType('MODULE_STATUS', row.status)">{{ dictStore.getLabel('MODULE_STATUS', row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="描述" min-width="150" />
            <el-table-column label="模块上架日期" width="120">
              <template #default="{ row }">
                {{ row.versionReleaseDate ? row.versionReleaseDate.substring(0, 10) : '' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-tooltip content="编辑" placement="top">
                  <el-button link type="primary" @click="handleEditModule(row)">
                    <el-icon><Edit /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-tooltip content="删除" placement="top">
                  <el-button link type="danger" @click="handleDeleteModule(row.id)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </el-tooltip>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- Pagination -->
      <el-pagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[5, 10, 20]"
        layout="total, sizes, prev, pager, next"
        @current-change="loadData"
        @size-change="loadData"
        style="margin-top: 16px; justify-content: flex-end;"
      />
    </el-card>

    <!-- Add/Edit Product Dialog -->
    <el-dialog v-model="formVisible" :title="formTitle" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="产品编码" prop="productCode">
          <el-input v-model="form.productCode" :disabled="isEdit" placeholder="请输入产品编码" />
        </el-form-item>
        <el-form-item label="产品名称" prop="productName">
          <el-input v-model="form.productName" placeholder="请输入产品名称" />
        </el-form-item>
        <el-form-item label="产品版本" prop="productVersion">
          <el-input v-model="form.productVersion" placeholder="如：1.0.0" />
        </el-form-item>
        <el-form-item label="版本上架日期">
          <el-date-picker v-model="form.versionReleaseDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="item in dictStore.getDictItems('PRODUCT_STATUS')" :key="item.itemCode" :label="item.label" :value="item.itemCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- Add/Edit Module Dialog -->
    <el-dialog v-model="moduleFormVisible" :title="moduleFormTitle" width="500px">
      <el-form :model="moduleForm" :rules="moduleRules" ref="moduleFormRef" label-width="100px">
        <el-form-item label="模块编码" prop="moduleCode">
          <el-input v-model="moduleForm.moduleCode" :disabled="isModuleEdit" placeholder="请输入模块编码" />
        </el-form-item>
        <el-form-item label="模块名称" prop="moduleName">
          <el-input v-model="moduleForm.moduleName" placeholder="请输入模块名称" />
        </el-form-item>
        <el-form-item label="模块版本">
          <el-input v-model="moduleForm.moduleVersion" placeholder="如：1.0.0" />
        </el-form-item>
        <el-form-item label="上架日期">
          <el-date-picker v-model="moduleForm.versionReleaseDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="moduleForm.status" style="width: 100%">
            <el-option v-for="item in dictStore.getDictItems('MODULE_STATUS')" :key="item.itemCode" :label="item.label" :value="item.itemCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="moduleForm.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="moduleFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="moduleSubmitLoading" @click="handleModuleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onActivated } from 'vue'
import { getProductListApi, createProductApi, updateProductApi, deleteProductApi, getModulesByProductApi, createModuleApi, updateModuleApi, deleteModuleApi } from '@/api/pm/wbs'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, ArrowRight } from '@element-plus/icons-vue'

import { useDictStore } from '@/store/dict'

const dictStore = useDictStore()
const productList = ref([])
const total = ref(0)
const loading = ref(false)
const keyword = ref('')
const expandedProductId = ref(null)
const moduleList = ref([])
const moduleLoading = ref(false)

// Product form
const formVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)
const editId = ref('')
const isEdit = computed(() => !!editId.value)
const formTitle = computed(() => isEdit.value ? '编辑产品' : '新增产品')

const queryParams = reactive({ pageNum: 1, pageSize: 5, keyword: '' })

const form = reactive({ productCode: '', productName: '', productVersion: '', versionReleaseDate: '', status: 'ACTIVE', description: '' })
const rules = {
  productCode: [{ required: true, message: '请输入产品编码', trigger: 'blur' }],
  productName: [{ required: true, message: '请输入产品名称', trigger: 'blur' }]
}

// Module form
const moduleFormVisible = ref(false)
const moduleSubmitLoading = ref(false)
const moduleFormRef = ref(null)
const moduleEditId = ref('')
const isModuleEdit = computed(() => !!moduleEditId.value)
const moduleFormTitle = computed(() => isModuleEdit.value ? '编辑模块' : '新增模块')

const moduleForm = reactive({ moduleId: '', moduleCode: '', moduleName: '', moduleVersion: '', versionReleaseDate: '', status: 'ACTIVE', description: '', productId: '' })
const moduleRules = {
  moduleCode: [{ required: true, message: '请输入模块编码', trigger: 'blur' }],
  moduleName: [{ required: true, message: '请输入模块名称', trigger: 'blur' }]
}

// Product CRUD
const loadData = async () => {
  loading.value = true
  queryParams.keyword = keyword.value
  try {
    const res = await getProductListApi(queryParams)
    productList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const toggleProduct = (productId) => {
  if (expandedProductId.value === productId) {
    expandedProductId.value = null
    moduleList.value = []
  } else {
    handleExpandProduct(productId)
  }
}

const handleExpandProduct = async (productId) => {
  expandedProductId.value = productId
  moduleList.value = []
  moduleLoading.value = true
  try {
    const res = await getModulesByProductApi(productId)
    moduleList.value = res.data || []
  } catch {
    ElMessage.error('加载模块数据失败')
  } finally {
    moduleLoading.value = false
  }
}

const handleAdd = () => {
  Object.assign(form, { productCode: '', productName: '', productVersion: '', versionReleaseDate: '', status: 'ACTIVE', description: '' })
  editId.value = ''
  formVisible.value = true
}

const handleEdit = (row) => {
  Object.assign(form, {
    productCode: row.productCode, productName: row.productName,
    productVersion: row.productVersion || '', versionReleaseDate: row.versionReleaseDate || '',
    status: row.status, description: row.description
  })
  editId.value = row.id
  formVisible.value = true
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确认删除该产品？', '提示', { type: 'warning' })
  await deleteProductApi(id)
  ElMessage.success('删除成功')
  loadData()
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateProductApi(editId.value, form)
    } else {
      await createProductApi(form)
    }
    ElMessage.success('保存成功')
    formVisible.value = false
    loadData()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    submitLoading.value = false
  }
}

// Module CRUD
const handleAddModule = (productId, productName) => {
  Object.assign(moduleForm, { moduleId: '', moduleCode: '', moduleName: '', moduleVersion: '', versionReleaseDate: '', status: 'ACTIVE', description: '', productId })
  moduleEditId.value = ''
  moduleFormVisible.value = true
}

const handleEditModule = (row) => {
  Object.assign(moduleForm, {
    moduleId: row.id, moduleCode: row.moduleCode, moduleName: row.moduleName,
    moduleVersion: row.moduleVersion || '', versionReleaseDate: row.versionReleaseDate || '',
    status: row.status || 'ACTIVE', description: row.description, productId: row.productId
  })
  moduleEditId.value = row.id
  moduleFormVisible.value = true
}

const handleDeleteModule = async (id) => {
  await ElMessageBox.confirm('确认删除该模块？', '提示', { type: 'warning' })
  await deleteModuleApi(id)
  ElMessage.success('删除成功')
  await handleExpandProduct(expandedProductId.value)
  await loadData()
}

const handleModuleSubmit = async () => {
  const valid = await moduleFormRef.value.validate().catch(() => false)
  if (!valid) return
  moduleSubmitLoading.value = true
  try {
    if (isModuleEdit.value) {
      await updateModuleApi(moduleEditId.value, moduleForm)
    } else {
      await createModuleApi(moduleForm)
    }
    ElMessage.success('保存成功')
    moduleFormVisible.value = false
    await handleExpandProduct(expandedProductId.value)
    await loadData()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    moduleSubmitLoading.value = false
  }
}

// Product/module status: migrated to dictStore (PRODUCT_STATUS, MODULE_STATUS)

onMounted(loadData)
onActivated(loadData)
</script>
