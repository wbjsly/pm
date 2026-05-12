<template>
  <div class="dict-page">
    <div class="dict-container">
      <!-- 左栏：字典类型 -->
      <div class="dict-left">
        <el-card>
          <template #header>
            <div class="card-header">
              <span style="font-weight: bold; font-size: 16px;">字典类型</span>
              <el-button type="primary" @click="handleAddType">新增类型</el-button>
            </div>
          </template>
          <el-table
            :data="typeList"
            v-loading="typeLoading"
            highlight-current-row
            @current-change="handleTypeSelect"
            stripe
          >
            <el-table-column prop="typeCode" label="类型编码" min-width="120" />
            <el-table-column prop="typeName" label="类型名称" min-width="120" />
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click.stop="handleEditType(row)">编辑</el-button>
                <el-button link type="danger" @click.stop="handleDeleteType(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>

      <!-- 右栏：字典条目 -->
      <div class="dict-right">
        <el-card>
          <template #header>
            <div class="card-header">
              <span style="font-weight: bold; font-size: 16px;">
                {{ selectedType ? '字典条目 - ' + selectedType.typeName : '请选择字典类型' }}
              </span>
              <el-button type="primary" :disabled="!selectedType" @click="handleAddItem">新增条目</el-button>
            </div>
          </template>
          <el-table :data="itemList" v-loading="itemLoading" stripe>
            <el-table-column prop="itemCode" label="条目编码" min-width="120" />
            <el-table-column prop="itemLabel" label="条目名称" min-width="120" />
            <el-table-column prop="tagType" label="标签类型" width="100">
              <template #default="{ row }">
                <el-tag :type="row.tagType" size="small">{{ row.tagType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="sortOrder" label="排序" width="80" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                  {{ row.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleEditItem(row)">编辑</el-button>
                <el-button link type="danger" @click="handleDeleteItem(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>
    </div>

    <!-- 类型弹窗 -->
    <el-dialog v-model="typeDialogVisible" :title="typeDialogTitle" width="500px">
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="80px">
        <el-form-item label="类型编码" prop="typeCode">
          <el-input v-model="typeForm.typeCode" :disabled="!!typeForm.id" placeholder="请输入类型编码" />
        </el-form-item>
        <el-form-item label="类型名称" prop="typeName">
          <el-input v-model="typeForm.typeName" placeholder="请输入类型名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="typeForm.description" type="textarea" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="typeForm.sortOrder" :min="0" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="typeSubmitting" @click="submitType">确定</el-button>
      </template>
    </el-dialog>

    <!-- 条目弹窗 -->
    <el-dialog v-model="itemDialogVisible" :title="itemDialogTitle" width="500px">
      <el-form ref="itemFormRef" :model="itemForm" :rules="itemRules" label-width="80px">
        <el-form-item label="条目编码" prop="itemCode">
          <el-input v-model="itemForm.itemCode" :disabled="!!itemForm.id" placeholder="请输入条目编码" />
        </el-form-item>
        <el-form-item label="条目名称" prop="itemLabel">
          <el-input v-model="itemForm.itemLabel" placeholder="请输入条目名称" />
        </el-form-item>
        <el-form-item label="标签类型" prop="tagType">
          <el-select v-model="itemForm.tagType" placeholder="请选择标签类型">
            <el-option label="primary" value="primary" />
            <el-option label="success" value="success" />
            <el-option label="warning" value="warning" />
            <el-option label="danger" value="danger" />
            <el-option label="info" value="info" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="itemForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="itemForm.sortOrder" :min="0" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="itemSubmitting" @click="submitItem">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getDictTypesApi,
  getDictItemsApi,
  createDictTypeApi,
  updateDictTypeApi,
  deleteDictTypeApi,
  createDictItemApi,
  updateDictItemApi,
  deleteDictItemApi
} from '@/api/system/dict'

// ========== 类型相关 ==========
const typeLoading = ref(false)
const typeList = ref([])
const selectedType = ref(null)

const typeDialogVisible = ref(false)
const typeDialogTitle = ref('新增字典类型')
const typeSubmitting = ref(false)
const typeFormRef = ref(null)
const typeForm = reactive({
  id: '',
  typeCode: '',
  typeName: '',
  description: '',
  sortOrder: 0
})
const typeRules = {
  typeCode: [{ required: true, message: '请输入类型编码', trigger: 'blur' }],
  typeName: [{ required: true, message: '请输入类型名称', trigger: 'blur' }]
}

// ========== 条目相关 ==========
const itemLoading = ref(false)
const itemList = ref([])

const itemDialogVisible = ref(false)
const itemDialogTitle = ref('新增字典条目')
const itemSubmitting = ref(false)
const itemFormRef = ref(null)
const itemForm = reactive({
  id: '',
  typeCode: '',
  itemCode: '',
  itemLabel: '',
  tagType: 'primary',
  status: 1,
  sortOrder: 0
})
const itemRules = {
  itemCode: [{ required: true, message: '请输入条目编码', trigger: 'blur' }],
  itemLabel: [{ required: true, message: '请输入条目名称', trigger: 'blur' }],
  tagType: [{ required: true, message: '请选择标签类型', trigger: 'change' }]
}

// ========== 类型操作 ==========
async function loadTypes() {
  typeLoading.value = true
  try {
    const res = await getDictTypesApi()
    typeList.value = res.data || []
  } catch {
    // handled by interceptor
  } finally {
    typeLoading.value = false
  }
}

function handleTypeSelect(row) {
  selectedType.value = row
  if (row) {
    loadItems(row.typeCode)
  } else {
    itemList.value = []
  }
}

function handleAddType() {
  typeDialogTitle.value = '新增字典类型'
  typeForm.id = ''
  typeForm.typeCode = ''
  typeForm.typeName = ''
  typeForm.description = ''
  typeForm.sortOrder = 0
  typeDialogVisible.value = true
}

function handleEditType(row) {
  typeDialogTitle.value = '编辑字典类型'
  typeForm.id = row.id
  typeForm.typeCode = row.typeCode
  typeForm.typeName = row.typeName
  typeForm.description = row.description || ''
  typeForm.sortOrder = row.sortOrder || 0
  typeDialogVisible.value = true
}

async function submitType() {
  const valid = await typeFormRef.value.validate().catch(() => false)
  if (!valid) return
  typeSubmitting.value = true
  try {
    const data = {
      typeCode: typeForm.typeCode,
      typeName: typeForm.typeName,
      description: typeForm.description,
      sortOrder: typeForm.sortOrder
    }
    if (typeForm.id) {
      data.id = typeForm.id
      await updateDictTypeApi(data)
      ElMessage.success('更新成功')
    } else {
      await createDictTypeApi(data)
      ElMessage.success('新增成功')
    }
    typeDialogVisible.value = false
    loadTypes()
  } catch {
    // handled by interceptor
  } finally {
    typeSubmitting.value = false
  }
}

async function handleDeleteType(row) {
  await ElMessageBox.confirm(
    `确定要删除字典类型 "${row.typeName}" 吗？删除后该类型下的所有条目也将被删除。`,
    '提示',
    { type: 'warning' }
  )
  try {
    await deleteDictTypeApi(row.id)
    ElMessage.success('删除成功')
    if (selectedType.value && selectedType.value.id === row.id) {
      selectedType.value = null
      itemList.value = []
    }
    loadTypes()
  } catch {
    // handled by interceptor
  }
}

// ========== 条目操作 ==========
async function loadItems(typeCode) {
  itemLoading.value = true
  try {
    const res = await getDictItemsApi(typeCode)
    itemList.value = res.data || []
  } catch {
    // handled by interceptor
  } finally {
    itemLoading.value = false
  }
}

function handleAddItem() {
  itemDialogTitle.value = '新增字典条目'
  itemForm.id = ''
  itemForm.typeCode = selectedType.value.typeCode
  itemForm.itemCode = ''
  itemForm.itemLabel = ''
  itemForm.tagType = 'primary'
  itemForm.status = 1
  itemForm.sortOrder = 0
  itemDialogVisible.value = true
}

function handleEditItem(row) {
  itemDialogTitle.value = '编辑字典条目'
  itemForm.id = row.id
  itemForm.typeCode = selectedType.value.typeCode
  itemForm.itemCode = row.itemCode
  itemForm.itemLabel = row.itemLabel
  itemForm.tagType = row.tagType || 'primary'
  itemForm.status = row.status !== undefined ? row.status : 1
  itemForm.sortOrder = row.sortOrder || 0
  itemDialogVisible.value = true
}

async function submitItem() {
  const valid = await itemFormRef.value.validate().catch(() => false)
  if (!valid) return
  itemSubmitting.value = true
  try {
    const data = {
      typeCode: itemForm.typeCode,
      itemCode: itemForm.itemCode,
      itemLabel: itemForm.itemLabel,
      tagType: itemForm.tagType,
      status: itemForm.status,
      sortOrder: itemForm.sortOrder
    }
    if (itemForm.id) {
      data.id = itemForm.id
      await updateDictItemApi(data)
      ElMessage.success('更新成功')
    } else {
      await createDictItemApi(data)
      ElMessage.success('新增成功')
    }
    itemDialogVisible.value = false
    loadItems(selectedType.value.typeCode)
  } catch {
    // handled by interceptor
  } finally {
    itemSubmitting.value = false
  }
}

async function handleDeleteItem(row) {
  await ElMessageBox.confirm(
    `确定要删除字典条目 "${row.itemLabel}" 吗？`,
    '提示',
    { type: 'warning' }
  )
  try {
    await deleteDictItemApi(row.id)
    ElMessage.success('删除成功')
    loadItems(selectedType.value.typeCode)
  } catch {
    // handled by interceptor
  }
}

onMounted(() => {
  loadTypes()
})
</script>

<style scoped>
.dict-page {
  height: 100%;
}
.dict-container {
  display: flex;
  gap: 16px;
  height: 100%;
}
.dict-left {
  width: 300px;
  min-width: 300px;
}
.dict-right {
  flex: 1;
  min-width: 0;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
