<template>
  <div class="cost-quota">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="card-title">交付成本定额</span>
          <div class="header-actions">
            <el-select
              v-model="selectedYearId"
              placeholder="选择年份"
              @change="loadQuotas"
              style="width: 240px"
            >
              <el-option
                v-for="y in years"
                :key="y.id"
                :label="y.name + ' (' + y.startDate + ' ~ ' + y.endDate + ')'"
                :value="y.id"
              />
            </el-select>
            <el-button type="primary" @click="showYearDialog = true">
              <el-icon><Plus /></el-icon>年份
            </el-button>
            <el-button @click="showPositionDialog = true">
              <el-icon><Setting /></el-icon>岗位
            </el-button>
          </div>
        </div>
      </template>

      <div v-loading="loading">
        <el-empty v-if="!selectedYearId" description="请先选择或创建年份" />
        <el-table v-else :data="quotaList" stripe row-key="positionId">
          <el-table-column prop="positionName" label="岗位名称" width="100" />
          <el-table-column label="交付成本定额(元/人天)" width="200" align="right">
            <template #default="{ row }">
              <span class="amount-cell" v-if="row.dailyRate">
                ¥ {{ formatMoney(row.dailyRate) }}
              </span>
              <el-tag v-else type="info" size="small">未设置</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="版本号" width="100" align="center">
            <template #default="{ row }">
              <span v-if="row.versionNo">v{{ row.versionNo }}</span>
              <span v-else style="color: #999;">-</span>
            </template>
          </el-table-column>
          <el-table-column label="最近更新时间" width="180">
            <template #default="{ row }">
              {{ row.updateDate ? row.updateDate.substring(0, 19) : '-' }}
            </template>
          </el-table-column>
          <el-table-column label="生效日期" width="140">
            <template #default="{ row }">
              {{ row.effectiveDate || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-tooltip content="调价" placement="top">
                <el-button link type="primary" @click="openAdjust(row)">
                  <el-icon><Edit /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="历史版本" placement="top">
                <el-button link type="success" @click="openHistory(row)" :disabled="!row.quotaId">
                  <el-icon><Clock /></el-icon>
                </el-button>
              </el-tooltip>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- 调价弹窗 -->
    <el-dialog v-model="adjustVisible" title="调价" width="480px" @closed="resetAdjustForm">
      <el-form :model="adjustForm" label-width="100px">
        <el-form-item label="岗位">
          <el-input :model-value="adjustForm.positionName" disabled />
        </el-form-item>
        <el-form-item label="当前定额">
          <el-input :model-value="formatMoney(adjustForm.currentRate)" disabled>
            <template #append>元/人天</template>
          </el-input>
        </el-form-item>
        <el-form-item label="调价后定额" required>
          <el-input-number
            v-model="adjustForm.dailyRate"
            :min="0"
            :precision="2"
            :step="100"
            style="width: calc(100% - 88px)"
            placeholder="请输入"
          />
          <span style="margin-left: 8px; color: #606266;">元/人天</span>
        </el-form-item>
        <el-form-item label="生效日期">
          <el-date-picker
            v-model="adjustForm.effectiveDate"
            type="date"
            placeholder="默认当天"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="调价原因">
          <el-input v-model="adjustForm.changeReason" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" @click="doAdjust" :loading="adjustLoading">确认调价</el-button>
      </template>
    </el-dialog>

    <!-- 历史版本抽屉 -->
    <el-drawer v-model="historyVisible" title="历史版本" size="640px">
      <div v-loading="historyLoading">
        <el-table :data="historyList" stripe size="small">
          <el-table-column label="版本" width="80" align="center">
            <template #default="{ row }">v{{ row.versionNo }}</template>
          </el-table-column>
          <el-table-column label="单价" width="140" align="right">
            <template #default="{ row }">¥ {{ formatMoney(row.dailyRate) }}</template>
          </el-table-column>
          <el-table-column label="生效日期" width="120">
            <template #default="{ row }">{{ row.effectiveDate }}</template>
          </el-table-column>
          <el-table-column label="调价原因" min-width="140">
            <template #default="{ row }">{{ row.changeReason || '-' }}</template>
          </el-table-column>
          <el-table-column label="调整人" width="100">
            <template #default="{ row }">{{ row.createByName || '-' }}</template>
          </el-table-column>
          <el-table-column label="调整日期" width="170">
            <template #default="{ row }">{{ row.createDate ? row.createDate.substring(0, 19) : '-' }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>

    <!-- 年份管理弹窗 -->
    <el-dialog v-model="showYearDialog" title="年份管理" width="640px">
      <div class="dialog-actions">
        <el-button type="primary" @click="openYearForm()">
          <el-icon><Plus /></el-icon>创建年份
        </el-button>
      </div>
      <el-table :data="years" stripe size="small" style="margin-top: 12px">
        <el-table-column prop="name" label="年份名称" min-width="140" />
        <el-table-column prop="startDate" label="开始日期" width="120" />
        <el-table-column prop="endDate" label="结束日期" width="120" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openYearForm(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDeleteYear(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 年份表单弹窗 -->
      <el-dialog
        v-model="yearFormVisible"
        :title="editingYear.id ? '编辑年份' : '创建年份'"
        width="420px"
        append-to-body
      >
        <el-form :model="yearForm" label-width="100px">
          <el-form-item label="年份名称" required>
            <el-input v-model="yearForm.name" placeholder="如：2025财年" />
          </el-form-item>
          <el-form-item label="开始日期" required>
            <el-date-picker
              v-model="yearForm.startDate"
              type="date"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="结束日期" required>
            <el-date-picker
              v-model="yearForm.endDate"
              type="date"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="yearFormVisible = false">取消</el-button>
          <el-button type="primary" @click="saveYear" :loading="yearSaving">保存</el-button>
        </template>
      </el-dialog>
    </el-dialog>

    <!-- 岗位管理弹窗 -->
    <el-dialog v-model="showPositionDialog" title="岗位管理" width="560px" @opened="loadPositions">
      <div class="dialog-actions">
        <el-tooltip content="新增岗位" placement="top">
          <el-button type="primary" circle @click="openPositionForm()">
            <el-icon><Plus /></el-icon>
          </el-button>
        </el-tooltip>
      </div>
      <el-table :data="positions" stripe size="small" style="margin-top: 12px" v-loading="posLoading">
        <el-table-column prop="name" label="岗位名称" min-width="120" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.isDefault === '1' ? '' : 'info'">
              {{ row.isDefault === '1' ? '默认' : '自定义' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-tooltip content="编辑" placement="top">
              <el-button link type="primary" @click="openPositionForm(row)">
                <el-icon><Edit /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button link type="danger" @click="handleDeletePosition(row)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog
        v-model="posFormVisible"
        :title="editingPosition.id ? '编辑岗位' : '新增岗位'"
        width="400px"
        append-to-body
      >
        <el-form :model="posForm" label-width="80px">
          <el-form-item label="名称" required>
            <el-input v-model="posForm.name" placeholder="岗位名称" />
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="posForm.sortOrder" :min="0" style="width: 100%" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="posFormVisible = false">取消</el-button>
          <el-button type="primary" @click="savePosition" :loading="posSaving">保存</el-button>
        </template>
      </el-dialog>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onActivated, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Setting, Edit, Clock, Delete } from '@element-plus/icons-vue'
import {
  getPositionListApi, createPositionApi, updatePositionApi, deletePositionApi,
  getYearListApi, createYearApi, updateYearApi, deleteYearApi, getDefaultStartDateApi,
  getQuotaListApi, adjustQuotaApi, getQuotaHistoryApi
} from '@/api/system/costQuota'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const loading = ref(false)
const years = ref([])
const selectedYearId = ref('')
const quotaList = ref([])
const positions = ref([])

const isAdmin = computed(() => {
  return userStore.userInfo?.roles?.includes('ROLE_ADMIN')
})

const formatMoney = (val) => {
  const num = parseFloat(val) || 0
  if (num === 0) return '0.00'
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ==================== 年份 ====================

const showYearDialog = ref(false)
const yearFormVisible = ref(false)
const editingYear = ref({})
const yearForm = ref({ name: '', startDate: '', endDate: '' })
const yearSaving = ref(false)

const loadYears = async () => {
  try {
    const res = await getYearListApi()
    years.value = res.data || []
    if (years.value.length > 0 && !selectedYearId.value) {
      selectedYearId.value = years.value[0].id
    }
  } catch {
    // ignore
  }
}

const openYearForm = async (row) => {
  if (row?.id) {
    editingYear.value = { ...row }
    yearForm.value = { name: row.name, startDate: row.startDate, endDate: row.endDate }
  } else {
    editingYear.value = {}
    yearForm.value = { name: '', startDate: '', endDate: '' }
    try {
      const res = await getDefaultStartDateApi()
      yearForm.value.startDate = res.data?.defaultStartDate || ''
    } catch { /* ignore */ }
  }
  yearFormVisible.value = true
}

const saveYear = async () => {
  if (!yearForm.value.name || !yearForm.value.startDate || !yearForm.value.endDate) {
    ElMessage.warning('请填写完整信息')
    return
  }
  yearSaving.value = true
  try {
    if (editingYear.value.id) {
      await updateYearApi(editingYear.value.id, yearForm.value)
      ElMessage.success('更新成功')
    } else {
      await createYearApi(yearForm.value)
      ElMessage.success('创建成功')
    }
    yearFormVisible.value = false
    await loadYears()
  } catch (e) {
    ElMessage.error('操作失败')
  } finally {
    yearSaving.value = false
  }
}

const handleDeleteYear = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除此年份吗？', '提示', { type: 'warning' })
    await deleteYearApi(row.id)
    ElMessage.success('删除成功')
    if (selectedYearId.value === row.id) selectedYearId.value = ''
    await loadYears()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '删除失败')
  }
}

// ==================== 岗位 ====================

const showPositionDialog = ref(false)
const posFormVisible = ref(false)
const editingPosition = ref({})
const posForm = ref({ name: '', sortOrder: 0 })
const posLoading = ref(false)
const posSaving = ref(false)

const loadPositions = async () => {
  posLoading.value = true
  try {
    const res = await getPositionListApi()
    positions.value = res.data || []
  } catch {
    // ignore
  } finally {
    posLoading.value = false
  }
}

const openPositionForm = (row) => {
  if (row?.id) {
    editingPosition.value = { ...row }
    posForm.value = { name: row.name, sortOrder: row.sortOrder }
  } else {
    editingPosition.value = {}
    posForm.value = { name: '', sortOrder: (positions.value.length || 0) + 1 }
  }
  posFormVisible.value = true
}

const savePosition = async () => {
  if (!posForm.value.name) {
    ElMessage.warning('请输入岗位名称')
    return
  }
  posSaving.value = true
  try {
    if (editingPosition.value.id) {
      await updatePositionApi(editingPosition.value.id, posForm.value)
      ElMessage.success('更新成功')
    } else {
      await createPositionApi(posForm.value)
      ElMessage.success('创建成功')
    }
    posFormVisible.value = false
    await loadPositions()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    posSaving.value = false
  }
}

const handleDeletePosition = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除此岗位吗？', '提示', { type: 'warning' })
    await deletePositionApi(row.id)
    ElMessage.success('删除成功')
    await loadPositions()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '删除失败')
  }
}

// ==================== 定额 ====================

const loadQuotas = async () => {
  if (!selectedYearId.value) return
  loading.value = true
  try {
    const res = await getQuotaListApi(selectedYearId.value)
    quotaList.value = res.data || []
  } catch {
    ElMessage.error('加载定额数据失败')
  } finally {
    loading.value = false
  }
}

// 调价
const adjustVisible = ref(false)
const adjustForm = ref({
  positionId: '', positionName: '', currentRate: 0,
  dailyRate: null, effectiveDate: '', changeReason: ''
})
const adjustLoading = ref(false)

const openAdjust = (row) => {
  adjustForm.value = {
    positionId: row.positionId,
    positionName: row.positionName,
    currentRate: row.dailyRate || 0,
    dailyRate: null,
    effectiveDate: '',
    changeReason: ''
  }
  adjustVisible.value = true
}

const resetAdjustForm = () => {
  adjustForm.value = {
    positionId: '', positionName: '', currentRate: 0,
    dailyRate: null, effectiveDate: '', changeReason: ''
  }
}

const doAdjust = async () => {
  if (!adjustForm.value.dailyRate || adjustForm.value.dailyRate <= 0) {
    ElMessage.warning('请输入有效的单价')
    return
  }
  adjustLoading.value = true
  try {
    await adjustQuotaApi({
      positionId: adjustForm.value.positionId,
      yearId: selectedYearId.value,
      dailyRate: adjustForm.value.dailyRate,
      effectiveDate: adjustForm.value.effectiveDate || new Date().toISOString().substring(0, 10),
      changeReason: adjustForm.value.changeReason
    })
    ElMessage.success('调价成功')
    adjustVisible.value = false
    await loadQuotas()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '调价失败')
  } finally {
    adjustLoading.value = false
  }
}

// 历史版本
const historyVisible = ref(false)
const historyList = ref([])
const historyLoading = ref(false)
const currentHistoryPosition = ref({})

const openHistory = async (row) => {
  historyVisible.value = true
  historyLoading.value = true
  try {
    const res = await getQuotaHistoryApi(row.positionId, selectedYearId.value)
    historyList.value = res.data || []
  } catch {
    ElMessage.error('加载历史版本失败')
  } finally {
    historyLoading.value = false
  }
}

watch(selectedYearId, () => {
  if (selectedYearId.value) loadQuotas()
})

onMounted(async () => {
  await loadYears()
  if (selectedYearId.value) await loadQuotas()
})

onActivated(() => {
  loadYears()
  if (selectedYearId.value) loadQuotas()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-title {
  font-weight: bold;
  font-size: 16px;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.dialog-actions {
  display: flex;
  justify-content: flex-end;
}
.amount-cell {
  font-variant-numeric: tabular-nums;
}

</style>
