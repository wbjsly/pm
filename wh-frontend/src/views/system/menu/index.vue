<template>
  <div class="menu-management">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span style="font-weight: bold; font-size: 16px;">菜单管理</span>
          <el-button type="primary" :icon="Plus" @click="handleAdd()" />
        </div>
      </template>

      <el-table
        :data="treeData" v-loading="loading" stripe
        row-key="id" default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="title" label="菜单名称" min-width="140" />
        <el-table-column prop="path" label="路由路径" width="180" />
        <el-table-column prop="icon" label="图标" width="100" />
        <el-table-column prop="sortOrder" label="排序" width="70" />
        <el-table-column prop="perm" label="角色" width="200">
          <template #default="{ row }">
            <el-tag v-for="r in (row.perm || '').split(',').filter(Boolean)" :key="r" size="small" style="margin-right: 4px;">{{ r.trim() }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === '1' ? 'success' : 'info'" size="small">{{ row.status === '1' ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <template v-if="!row.parentId">
              <el-tooltip content="新增子菜单">
                <el-button link type="success" :icon="Plus" @click="handleAdd(row.id)" />
              </el-tooltip>
            </template>
            <el-tooltip content="编辑">
              <el-button link type="primary" :icon="Edit" @click="handleEdit(row)" />
            </el-tooltip>
            <el-tooltip content="删除">
              <el-button link type="danger" :icon="Delete" @click="handleDelete(row.id)" />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 表单弹窗 -->
    <el-dialog v-model="formVisible" :title="dialogTitle" width="520px">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px;">
        新建菜单项需同时在 <code>router/index.js</code> 中定义对应路由，否则点击菜单将无法跳转。
      </el-alert>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item v-if="form.parentId" label="上级菜单">
          <el-select v-model="form.parentId" style="width: 100%;">
            <el-option
              v-for="p in parentOptions"
              :key="p.id"
              :label="p.title"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="菜单名称" prop="title">
          <el-input v-model="form.title" placeholder="如：项目立项" />
        </el-form-item>
        <el-form-item label="路由路径" prop="path">
          <el-input v-model="form.path" placeholder="如：/pm/charter" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="Element Plus 图标名，如：Document" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="角色权限">
          <el-select v-model="form.perm" multiple placeholder="选择角色" style="width: 100%;">
            <el-option v-for="r in roleOptions" :key="r.roleCode" :label="r.roleName" :value="r.roleCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="1">启用</el-radio>
            <el-radio value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onActivated } from 'vue'
import { getMenuListApi, createMenuApi, updateMenuApi, deleteMenuApi } from '@/api/system/menu'
import { getAllRolesApi } from '@/api/system/user'
import { useMenuStore } from '@/store/menu'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'

const menuStore = useMenuStore()
const menuList = ref([])
const loading = ref(false)
const formVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)
const editId = ref('')
const isEdit = ref(false)
const roleOptions = ref([])

const form = reactive({
  title: '', path: '', icon: '', sortOrder: 0, perm: [], status: '1', parentId: ''
})

const treeData = computed(() => {
  const list = menuList.value
  const map = {}
  const roots = []

  list.forEach(item => {
    map[item.id] = { ...item, children: [] }
  })

  list.forEach(item => {
    const node = map[item.id]
    if (item.parentId && map[item.parentId]) {
      map[item.parentId].children.push(node)
    } else {
      roots.push(node)
    }
  })

  return roots
})

const dialogTitle = computed(() => {
  if (isEdit.value) return '编辑菜单'
  return form.parentId ? '新增子菜单' : '新增菜单'
})

const parentOptions = computed(() => {
  return menuList.value.filter(m => !m.parentId && m.id !== editId.value)
})

const rules = {
  title: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  path: [{ required: true, message: '请输入路由路径', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMenuListApi()
    menuList.value = res.data || []
  } finally {
    loading.value = false
  }
}

const fetchRoles = async () => {
  try {
    const res = await getAllRolesApi()
    roleOptions.value = res.data || []
  } catch (e) {
    // ignore
  }
}

const handleAdd = (parentId = '') => {
  Object.assign(form, {
    title: '', path: '', icon: '', sortOrder: 0, perm: [], status: '1', parentId
  })
  editId.value = ''
  isEdit.value = false
  formVisible.value = true
}

const handleEdit = (row) => {
  Object.assign(form, {
    title: row.title,
    path: row.path,
    icon: row.icon || '',
    sortOrder: row.sortOrder || 0,
    perm: (row.perm || '').split(',').filter(Boolean).map(s => s.trim()),
    status: row.status,
    parentId: row.parentId || ''
  })
  editId.value = row.id
  isEdit.value = true
  formVisible.value = true
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确认删除该菜单？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteMenuApi(id)
    ElMessage.success('删除成功')
    menuStore.reset()
    loadData()
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '删除失败')
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    const data = {
      title: form.title,
      path: form.path,
      parentId: form.parentId,
      icon: form.icon,
      sortOrder: form.sortOrder,
      perm: form.perm.join(','),
      status: form.status
    }
    if (isEdit.value) {
      data.id = editId.value
      await updateMenuApi(data)
    } else {
      await createMenuApi(data)
    }
    ElMessage.success('保存成功')
    formVisible.value = false
    menuStore.reset()
    loadData()
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '保存失败')
  } finally {
    submitLoading.value = false
  }
}

onActivated(async () => {
  await loadData()
  fetchRoles()
})
</script>
