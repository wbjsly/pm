<template>
  <div class="budget-upgrade">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>升级预算</span>
          <el-button @click="$router.back()">返回</el-button>
        </div>
      </template>

      <!-- 项目信息区 + 全局差异面板 -->
      <div class="project-info-bar">
        <div class="info-readonly">
          <el-row :gutter="16">
            <el-col :span="6">
              <div class="info-item">
                <label>项目名称</label>
                <span class="info-value">{{ projectName }}</span>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="info-item">
                <label>预算编码</label>
                <span class="info-value">{{ originalData.budget?.budgetCode || '' }}</span>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="info-item">
                <label>版本号</label>
                <span class="info-value">{{ originalVersion }}</span>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="info-item">
                <label>状态</label>
                <el-tag :type="dictStore.getTagType('BUDGET_STATUS', originalData.budget?.status)">
                  {{ dictStore.getLabel('BUDGET_STATUS', originalData.budget?.status) }}
                </el-tag>
              </div>
            </el-col>
          </el-row>
        </div>
      </div>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <!-- 预算变更汇总 -->
        <div class="waterfall-panel">
          <div class="waterfall-title">预算变更汇总</div>
          <div class="waterfall-body">
            <div class="wf-row wf-header">
              <span class="wf-label"></span>
              <span class="wf-amount-label">调整前</span>
              <span class="wf-arrow"></span>
              <span class="wf-amount-label">调整后</span>
              <span class="wf-diff-label">差异</span>
            </div>
            <div class="wf-row">
              <span class="wf-label">人工</span>
              <span class="wf-amount">¥{{ formatMoney(leftLaborTotal) }}</span>
              <span class="wf-arrow">→</span>
              <span class="wf-amount">¥{{ formatMoney(categoryTotal('LABOR')) }}</span>
              <span class="wf-diff-col" :style="diffStyle(diffData.labor.diff)">
                {{ diffSign(diffData.labor.diff) }}¥{{ formatMoney(Math.abs(diffData.labor.diff)) }}
                ({{ diffSign(diffData.labor.percent) }}{{ Math.abs(diffData.labor.percent).toFixed(1) }}%)
              </span>
            </div>
            <div class="wf-row">
              <span class="wf-label">采购</span>
              <span class="wf-amount">¥{{ formatMoney(leftProcurementTotal) }}</span>
              <span class="wf-arrow">→</span>
              <span class="wf-amount">¥{{ formatMoney(categoryTotal('PROCUREMENT')) }}</span>
              <span class="wf-diff-col" :style="diffStyle(diffData.procurement.diff)">
                {{ diffSign(diffData.procurement.diff) }}¥{{ formatMoney(Math.abs(diffData.procurement.diff)) }}
                ({{ diffSign(diffData.procurement.percent) }}{{ Math.abs(diffData.procurement.percent).toFixed(1) }}%)
              </span>
            </div>
            <div class="wf-row">
              <span class="wf-label">其他</span>
              <span class="wf-amount">¥{{ formatMoney(leftOtherTotal) }}</span>
              <span class="wf-arrow">→</span>
              <span class="wf-amount">¥{{ formatMoney(otherTotal) }}</span>
              <span class="wf-diff-col" :style="diffStyle(diffData.other.diff)">
                {{ diffSign(diffData.other.diff) }}¥{{ formatMoney(Math.abs(diffData.other.diff)) }}
                ({{ diffSign(diffData.other.percent) }}{{ Math.abs(diffData.other.percent).toFixed(1) }}%)
              </span>
            </div>
            <div class="wf-row wf-subtotal">
              <span class="wf-label">科目差异合计</span>
              <span class="wf-amount">¥{{ formatMoney(leftCostBaseline) }}</span>
              <span class="wf-arrow">→</span>
              <span class="wf-amount">¥{{ formatMoney(costBaseline) }}</span>
              <span class="wf-diff-col" :style="diffStyle(diffData.subTotal.diff)">
                {{ diffSign(diffData.subTotal.diff) }}¥{{ formatMoney(Math.abs(diffData.subTotal.diff)) }}
                ({{ diffSign(diffData.subTotal.percent) }}{{ Math.abs(diffData.subTotal.percent).toFixed(1) }}%)
              </span>
            </div>
            <div class="wf-row" v-if="Math.abs(diffData.managementReserve.diff) > 0.01">
              <span class="wf-label">管理储备</span>
              <span class="wf-amount">¥{{ formatMoney(parseFloat(originalData.budget?.managementReserve) || 0) }}</span>
              <span class="wf-arrow">→</span>
              <span class="wf-amount">¥{{ formatMoney(form.managementReserve || 0) }}</span>
              <span class="wf-diff-col" :style="diffStyle(diffData.managementReserve.diff)">
                {{ diffSign(diffData.managementReserve.diff) }}¥{{ formatMoney(Math.abs(diffData.managementReserve.diff)) }}
                ({{ diffSign(diffData.managementReserve.percent) }}{{ Math.abs(diffData.managementReserve.percent).toFixed(1) }}%)
              </span>
            </div>
            <div class="wf-row wf-total">
              <span class="wf-label">总预算差异</span>
              <span class="wf-amount">¥{{ formatMoney(leftTotalBudget) }}</span>
              <span class="wf-arrow">→</span>
              <span class="wf-amount">¥{{ formatMoney(totalBudget) }}</span>
              <span class="wf-diff-col" :style="diffStyle(diffData.total.diff)">
                {{ diffSign(diffData.total.diff) }}¥{{ formatMoney(Math.abs(diffData.total.diff)) }}
                ({{ diffSign(diffData.total.percent) }}{{ Math.abs(diffData.total.percent).toFixed(1) }}%)
              </span>
            </div>
          </div>
        </div>

        <el-divider>预算科目</el-divider>

        <!-- 人工科目 -->
        <div class="category-section">
          <div class="section-header">
            <span class="section-title">人工</span>
            <span class="diff-badge" :style="diffStyle(diffData.labor.diff)">
              {{ diffSign(diffData.labor.diff) }}¥{{ formatMoney(Math.abs(diffData.labor.diff)) }}
              <span class="diff-percent">({{ diffSign(diffData.labor.percent) }}{{ Math.abs(diffData.labor.percent).toFixed(1) }}%)</span>
            </span>
          </div>
          <div class="section-body">
            <div class="section-left">
              <div class="section-subtitle">
                <span>调整前预算</span>
                <span>调整前预算合计：¥ {{ formatMoney(leftLaborTotal) }}</span>
              </div>
              <el-table :data="leftLaborItems" size="small">
                <el-table-column label="岗位" width="140">
                  <template #default="{ row }">
                    <span>{{ row.positionName || roleLabel(row.roleCode) }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="工时(小时)" width="100">
                  <template #default="{ row }">{{ row.hours }}</template>
                </el-table-column>
                <el-table-column label="金额" width="120">
                  <template #default="{ row }">¥ {{ formatMoney(row.budgetAmount) }}</template>
                </el-table-column>
              </el-table>
            </div>
            <div class="section-right">
              <div class="section-subtitle">
                <span>调整后预算</span>
                <div>
                  <span class="actual-cost-title">实际已发生成本：¥{{ formatMoney(actualCostByCategory('LABOR')) }}</span>
                  <span> | 调整后预算合计：¥ {{ formatMoney(categoryTotal('LABOR')) }}</span>
                </div>
              </div>
              <el-table v-if="adjustedItems.LABOR.length > 0" :data="adjustedItems.LABOR" size="small" style="width: 100%">
                <el-table-column label="岗位" min-width="160">
                  <template #default="{ row }">
                    <el-select v-model="row.positionId" size="small" filterable placeholder="请选择岗位"
                      @change="handlePositionChange(row)">
                      <el-option v-for="p in positionList" :key="p.id" :label="p.name" :value="p.id" />
                    </el-select>
                  </template>
                </el-table-column>
                <el-table-column label="工时(小时)" min-width="100">
                  <template #default="{ row }">
                    <el-input-number v-model="row.hours" :min="0" :step="8" size="small" controls-position="right" @change="calcLaborAmount(row)" />
                  </template>
                </el-table-column>
                <el-table-column label="成本定额(元/时)" min-width="120">
                  <template #default="{ row }">
                    <el-input-number v-model="row.costRate" :min="0" :precision="2" size="small" disabled
                      :class="{ 'zero-rate': row.costRate === 0 }" />
                  </template>
                </el-table-column>
                <el-table-column label="金额" min-width="100">
                  <template #default="{ row }">¥ {{ formatMoney(row.amount) }}</template>
                </el-table-column>
                <el-table-column label="操作" min-width="70">
                  <template #default="{ $index }">
                    <el-button link type="danger" size="small" @click="removeFromAdjust('LABOR', $index)">移除</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <div v-else class="empty-placeholder">暂未调整</div>
              <el-button size="small" @click="addItem('LABOR')" style="margin-top: 8px">
                <el-icon><Plus /></el-icon> 添加角色
              </el-button>
            </div>
          </div>
        </div>

        <!-- 采购科目 -->
        <div class="category-section">
          <div class="section-header">
            <span class="section-title">采购</span>
            <span class="diff-badge" :style="diffStyle(diffData.procurement.diff)">
              {{ diffSign(diffData.procurement.diff) }}¥{{ formatMoney(Math.abs(diffData.procurement.diff)) }}
              <span class="diff-percent">({{ diffSign(diffData.procurement.percent) }}{{ Math.abs(diffData.procurement.percent).toFixed(1) }}%)</span>
            </span>
          </div>
          <div class="section-body">
            <div class="section-left">
              <div class="section-subtitle">
                <span>调整前预算</span>
                <span>调整前预算合计：¥ {{ formatMoney(leftProcurementTotal) }}</span>
              </div>
              <el-table :data="leftProcurementItems" size="small">
                <el-table-column label="BOM项" width="140">
                  <template #default="{ row }">{{ row.bomItem }}</template>
                </el-table-column>
                <el-table-column label="数量" width="80">
                  <template #default="{ row }">{{ row.qty }}</template>
                </el-table-column>
                <el-table-column label="单价" width="110">
                  <template #default="{ row }">{{ formatMoney(row.unitPrice) }}</template>
                </el-table-column>
                <el-table-column label="金额" width="110">
                  <template #default="{ row }">¥ {{ formatMoney(row.budgetAmount) }}</template>
                </el-table-column>
              </el-table>
            </div>
            <div class="section-right">
              <div class="section-subtitle">
                <span>调整后预算</span>
                <div>
                  <span class="actual-cost-title">实际已发生成本：¥{{ formatMoney(actualCostByCategory('PROCUREMENT')) }}</span>
                  <span> | 调整后预算合计：¥ {{ formatMoney(categoryTotal('PROCUREMENT')) }}</span>
                </div>
              </div>
              <el-table v-if="adjustedItems.PROCUREMENT.length > 0" :data="adjustedItems.PROCUREMENT" size="small" style="width: 100%">
                <el-table-column label="BOM项" min-width="140">
                  <template #default="{ row }">
                    <el-input v-model="row.bomItem" size="small" placeholder="BOM项名称" />
                  </template>
                </el-table-column>
                <el-table-column label="数量" min-width="80">
                  <template #default="{ row }">
                    <el-input-number v-model="row.qty" :min="0" size="small" @change="calcProcurementAmount(row)" />
                  </template>
                </el-table-column>
                <el-table-column label="单价" min-width="100">
                  <template #default="{ row }">
                    <el-input-number v-model="row.unitPrice" :min="0" :precision="2" size="small" @change="calcProcurementAmount(row)" />
                  </template>
                </el-table-column>
                <el-table-column label="金额" min-width="110">
                  <template #default="{ row }">
                    <el-input-number v-model="row.amount" :min="0" :step="1000" size="small" controls-position="right" @change="onProcurementAmountChange(row)" />
                  </template>
                </el-table-column>
                <el-table-column label="操作" min-width="70">
                  <template #default="{ $index }">
                    <el-button link type="danger" size="small" @click="removeFromAdjust('PROCUREMENT', $index)">移除</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <div v-else class="empty-placeholder">暂未调整</div>
              <el-button size="small" @click="addItem('PROCUREMENT')" style="margin-top: 8px">
                <el-icon><Plus /></el-icon> 添加采购项
              </el-button>
            </div>
          </div>
        </div>

        <!-- 其他科目 -->
        <div class="category-section">
          <div class="section-header">
            <span class="section-title">其他科目</span>
            <span class="diff-badge" :style="diffStyle(diffData.other.diff)">
              {{ diffSign(diffData.other.diff) }}¥{{ formatMoney(Math.abs(diffData.other.diff)) }}
              <span class="diff-percent">({{ diffSign(diffData.other.percent) }}{{ Math.abs(diffData.other.percent).toFixed(1) }}%)</span>
            </span>
          </div>
          <div class="section-body">
            <div class="section-left">
              <div class="section-subtitle">
                <span>调整前预算</span>
                <span>调整前预算合计：¥ {{ formatMoney(leftOtherTotal) }}</span>
              </div>
              <div class="other-categories-row">
                <template v-for="(cat, idx) in otherCategoryList.slice(0, 3)" :key="cat.value">
                  <div class="other-cat-item">
                    <label class="other-cat-label">{{ cat.label }}</label>
                    <span class="other-cat-value">¥ {{ formatMoney(getLeftOtherAmount(cat.value)) }}</span>
                  </div>
                  <span class="other-plus-operator">+</span>
                </template>
              </div>
              <div class="other-categories-row" style="margin-top: 8px;">
                <template v-for="(cat, idx) in otherCategoryList.slice(3)" :key="cat.value">
                  <div class="other-cat-item">
                    <label class="other-cat-label">{{ cat.label }}</label>
                    <span class="other-cat-value">¥ {{ formatMoney(getLeftOtherAmount(cat.value)) }}</span>
                  </div>
                  <span v-if="idx < 1" class="other-plus-operator">+</span>
                </template>
                <div class="other-cat-item other-cat-spacer"></div>
                <span class="other-plus-operator other-plus-spacer">+</span>
              </div>
            </div>
            <div class="section-right">
              <div class="section-subtitle">
                <span>调整后预算</span>
                <div>
                  <span class="actual-cost-title">实际已发生成本：¥{{ formatMoney(totalOtherActualCost()) }}</span>
                  <span> | 调整后预算合计：¥ {{ formatMoney(otherTotal) }}</span>
                </div>
              </div>
              <div class="other-categories-row" v-if="visibleOtherCategories.length > 0">
                <template v-for="(cat, idx) in otherCategoryList.slice(0, 3)" :key="cat.value">
                  <div v-if="adjustedItems[cat.value].length > 0" class="other-cat-item other-cat-adjust-item">
                    <label class="other-cat-label">{{ cat.label }}（实际已发生：¥{{ formatMoney(actualCostByCategory(cat.value)) }}）</label>
                    <el-input-number
                      v-model="adjustedItems[cat.value][0].amount"
                      :min="parseFloat(actualCostByCategory(cat.value))"
                      :precision="2" :step="1000" size="small" controls-position="right"
                    />
                  </div>
                  <span class="other-plus-operator">+</span>
                </template>
              </div>
              <div class="other-categories-row" style="margin-top: 8px;">
                <template v-for="(cat, idx) in otherCategoryList.slice(3)" :key="cat.value">
                  <div v-if="adjustedItems[cat.value].length > 0" class="other-cat-item other-cat-adjust-item">
                    <label class="other-cat-label">{{ cat.label }}（实际已发生：¥{{ formatMoney(actualCostByCategory(cat.value)) }}）</label>
                    <el-input-number
                      v-model="adjustedItems[cat.value][0].amount"
                      :min="parseFloat(actualCostByCategory(cat.value))"
                      :precision="2" :step="1000" size="small" controls-position="right"
                    />
                  </div>
                  <span v-if="idx < 1" class="other-plus-operator">+</span>
                </template>
                <div class="other-cat-item other-cat-spacer"></div>
                <span class="other-plus-operator other-plus-spacer">+</span>
              </div>
            </div>
          </div>
        </div>

        <el-divider>预算汇总</el-divider>

        <div class="category-section">
          <div class="section-header">
            <span class="section-title">预算汇总</span>
            <span class="diff-badge" :style="diffStyle(diffData.total.diff)">
              {{ diffSign(diffData.total.diff) }}¥{{ formatMoney(Math.abs(diffData.total.diff)) }}
              <span class="diff-percent">({{ diffSign(diffData.total.percent) }}{{ Math.abs(diffData.total.percent).toFixed(1) }}%)</span>
            </span>
          </div>
          <div class="section-body">
            <div class="section-left">
              <div class="budget-summary-row">
                <div class="summary-item">
                  <label>总预算</label>
                  <div class="summary-value highlight">¥ {{ formatMoney(leftTotalBudget) }}</div>
                </div>
                <div class="summary-operator">=</div>
                <div class="summary-item">
                  <label>项目直接预算</label>
                  <div class="summary-value">¥ {{ formatMoney(leftCostBaseline) }}</div>
                </div>
                <div class="summary-operator">+</div>
                <div class="summary-item">
                  <label>项目管理预算</label>
                  <div class="summary-value">¥ {{ formatMoney(parseFloat(originalData.budget?.managementReserve) || 0) }}</div>
                </div>
              </div>
              <div class="budget-summary-row summary-actual-row">
                <div class="summary-item">
                  <label>实际成本总额</label>
                  <div class="summary-value highlight" style="color: #67C23A;">¥ {{ formatMoney(totalActualCost) }}</div>
                </div>
                <div class="summary-operator">=</div>
                <div class="summary-item">
                  <label>项目直接实际成本</label>
                  <div class="summary-value" style="color: #67C23A;">¥ {{ formatMoney(totalActualCost) }}</div>
                </div>
                <div class="summary-operator">+</div>
                <div class="summary-item">
                  <label>管理实际成本</label>
                  <div class="summary-value" style="color: #67C23A;">¥ 0.00</div>
                </div>
              </div>
            </div>
            <div class="section-right">
              <div class="budget-summary-row">
                <div class="summary-item">
                  <label>总预算</label>
                  <div class="summary-value highlight">¥ {{ formatMoney(totalBudget) }}</div>
                </div>
                <div class="summary-operator">=</div>
                <div class="summary-item">
                  <label>项目直接预算</label>
                  <div class="summary-value">¥ {{ formatMoney(costBaseline) }}</div>
                </div>
                <div class="summary-operator">+</div>
                <div class="summary-item input-item">
                  <label>项目管理预算</label>
                  <el-input-number v-model="form.managementReserve" :min="0" :precision="2" class="reserve-input" />
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="form-actions">
          <el-button type="primary" @click="handleUpgrade" :loading="submitting">升级</el-button>
          <el-button @click="$router.back()">返回</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, onUnmounted, onActivated, watch } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getBudgetDetailWithItemsApi, upgradeBudgetApi } from '@/api/pm/budget'
import { getCharterListApi } from '@/api/pm/charter'
import { getPositionListApi, getCurrentRateApi } from '@/api/system/costQuota'

import { useDictStore } from '@/store/dict'

const route = useRoute()
const router = useRouter()
const dictStore = useDictStore()
const formRef = ref(null)
const submitting = ref(false)
const projects = ref([])
const positionList = ref([])

const originalData = ref({ budget: {}, items: [] })

const originalVersion = computed(() => originalData.value.budget?.version || '')

const otherCategoryList = [
  { label: '差旅', value: 'TRAVEL' },
  { label: '商务费用', value: 'BUSINESS' },
  { label: '客户招待费', value: 'ENTERTAINMENT' },
  { label: '活动费', value: 'ACTIVITY' },
  { label: '其他', value: 'OTHER' }
]

const singleValueCategories = new Set(otherCategoryList.map(c => c.value))

let nextId = 0
const adjustedItems = reactive({
  LABOR: [],
  PROCUREMENT: [],
  TRAVEL: [],
  BUSINESS: [],
  ENTERTAINMENT: [],
  ACTIVITY: [],
  OTHER: []
})

const createAdjustedItem = (category, originalItem) => {
  const base = { _key: nextId++, originalId: originalItem?.id || null }
  if (category === 'LABOR') {
    return {
      ...base,
      roleCode: originalItem?.roleCode || 'DEV',
      positionId: originalItem?.positionId || '',
      hours: originalItem?.hours || 0,
      costRate: originalItem?.costRate || 0,
      amount: originalItem?.budgetAmount || 0
    }
  }
  if (category === 'PROCUREMENT') {
    return {
      ...base,
      bomItem: originalItem?.bomItem || '',
      qty: originalItem?.qty || 0,
      unitPrice: originalItem?.unitPrice || 0,
      amount: originalItem?.budgetAmount || 0,
      manualAdjustment: 0
    }
  }
  return {
    ...base,
    amount: originalItem?.budgetAmount || 0
  }
}

const form = ref({
  projectId: '',
  managementReserve: 0
})

const rules = {
  projectId: [{ required: true, message: '请选择项目', trigger: 'change' }]
}

const allItems = computed(() => originalData.value.items || [])

const leftLaborItems = computed(() => allItems.value.filter(i => i.category === 'LABOR'))
const leftProcurementItems = computed(() => allItems.value.filter(i => i.category === 'PROCUREMENT'))

const leftLaborTotal = computed(() => leftLaborItems.value.reduce((s, i) => s + (i.budgetAmount || 0), 0))
const leftProcurementTotal = computed(() => leftProcurementItems.value.reduce((s, i) => s + (i.budgetAmount || 0), 0))
const leftOtherTotal = computed(() => {
  let total = 0
  for (const cat of otherCategoryList) {
    total += allItems.value.filter(i => i.category === cat.value).reduce((s, i) => s + (i.budgetAmount || 0), 0)
  }
  return total
})
const leftCostBaseline = computed(() => allItems.value.reduce((s, i) => s + (i.budgetAmount || 0), 0))
const leftTotalBudget = computed(() => leftCostBaseline.value + (parseFloat(originalData.value.budget?.managementReserve) || 0))

const projectName = computed(() => {
  const projectId = originalData.value.budget?.projectId
  const p = projects.value.find(x => x.id === projectId)
  return p?.projectName || ''
})

const visibleOtherCategories = computed(() =>
  otherCategoryList.filter(cat => adjustedItems[cat.value].length > 0)
)

// Actual cost by category
const actualCostByCategory = (category) => {
  if (category === 'OTHER') {
    let total = 0
    for (const cat of otherCategoryList) {
      total += allItems.value.filter(i => i.category === cat.value).reduce((s, i) => s + (i.actualAmount || 0), 0)
    }
    return total
  }
  return allItems.value.filter(i => i.category === category).reduce((s, i) => s + (i.actualAmount || 0), 0)
}

const totalActualCost = computed(() => {
  return allItems.value.reduce((s, i) => s + (i.actualAmount || 0), 0)
})

const totalOtherActualCost = () => {
  let total = 0
  for (const cat of otherCategoryList) {
    total += actualCostByCategory(cat.value)
  }
  return total
}

const actualAmountMap = computed(() => {
  const map = {}
  for (const item of (originalData.value.items || [])) {
    if (item.id) {
      map[item.id] = parseFloat(item.actualAmount) || 0
    }
  }
  return map
})

const getActualAmount = (originalId) => {
  if (originalId && actualAmountMap.value[originalId] !== undefined) {
    return actualAmountMap.value[originalId]
  }
  return 0
}

const adjustedOriginalIds = computed(() => {
  const ids = new Set()
  for (const cat of allCategories) {
    for (const item of adjustedItems[cat.value] || []) {
      if (item.originalId) ids.add(item.originalId)
    }
  }
  return ids
})

const mergedCategoryTotal = (category) => {
  if (singleValueCategories.has(category) && adjustedItems[category].length > 0) {
    return adjustedItems[category].reduce((s, i) => s + (i.amount || 0), 0)
  }
  let total = 0
  for (const item of (adjustedItems[category] || [])) {
    total += item.amount || 0
  }
  for (const item of (originalData.value.items || [])) {
    if (item.category === category && !adjustedOriginalIds.value.has(item.id)) {
      total += parseFloat(item.budgetAmount) || 0
    }
  }
  return total
}

const costBaseline = computed(() => {
  let total = 0
  for (const cat of allCategories) {
    total += mergedCategoryTotal(cat.value)
  }
  return total
})

const totalBudget = computed(() => costBaseline.value + (form.value.managementReserve || 0))

const otherTotal = computed(() => {
  let total = 0
  for (const cat of otherCategoryList) {
    total += mergedCategoryTotal(cat.value)
  }
  return total
})

const formatMoney = (val) => {
  const num = val || 0
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const getLeftOtherAmount = (category) => {
  const items = allItems.value.filter(i => i.category === category)
  return items.reduce((s, i) => s + (i.budgetAmount || 0), 0)
}

const roleLabel = (code) => {
  const map = { DEV: '开发(DEV)', QA: '测试(QA)', BA: '产品经理(BA)', ARCH: '架构师(ARCH)', PM: '项目经理(PM)' }
  return map[code] || code
}

const categoryTotal = (category) => {
  return mergedCategoryTotal(category)
}

// --- 差异计算 ---
const safePercent = (diff, base) => {
  if (base) return (diff / base) * 100
  return diff !== 0 ? 100 : 0
}

const diffData = computed(() => {
  const leftLabor = leftLaborTotal.value
  const leftProcurement = leftProcurementTotal.value
  const leftOther = leftOtherTotal.value
  const leftSubTotal = leftCostBaseline.value
  const leftManagementReserve = parseFloat(originalData.value.budget?.managementReserve) || 0
  const leftTotal = leftTotalBudget.value

  const rightLabor = categoryTotal('LABOR')
  const rightProcurement = categoryTotal('PROCUREMENT')
  const rightOther = otherTotal.value
  const rightSubTotal = costBaseline.value
  const rightManagementReserve = form.value.managementReserve || 0
  const rightTotal = totalBudget.value

  const laborDiff = rightLabor - leftLabor
  const procurementDiff = rightProcurement - leftProcurement
  const otherDiff = rightOther - leftOther
  const subTotalDiff = laborDiff + procurementDiff + otherDiff
  const managementReserveDiff = rightManagementReserve - leftManagementReserve
  const totalDiff = subTotalDiff + managementReserveDiff

  return {
    labor:       { diff: laborDiff, percent: safePercent(laborDiff, leftLabor) },
    procurement: { diff: procurementDiff, percent: safePercent(procurementDiff, leftProcurement) },
    other:       { diff: otherDiff, percent: safePercent(otherDiff, leftOther) },
    subTotal:    { diff: subTotalDiff, percent: safePercent(subTotalDiff, leftSubTotal) },
    managementReserve: { diff: managementReserveDiff, percent: safePercent(managementReserveDiff, leftManagementReserve) },
    total:       { diff: totalDiff, percent: safePercent(totalDiff, leftTotal) }
  }
})

const diffColor = (val) => val > 0 ? '#F56C6C' : val < 0 ? '#67C23A' : '#909399'
const diffSign = (val) => val > 0 ? '+' : ''
const diffStyle = (val) => ({ color: diffColor(val) })

// --- 未保存修改检测 ---
const isDirty = ref(false)

const hasUnsavedChanges = () => {
  if (Math.abs((form.value.managementReserve || 0) - (parseFloat(originalData.value.budget?.managementReserve) || 0)) > 0.01) return true
  for (const cat of allCategories) {
    if ((adjustedItems[cat.value] || []).length > 0) return true
  }
  return false
}

watch(() => form.value.managementReserve, () => {
  isDirty.value = hasUnsavedChanges()
})

watch(adjustedItems, () => {
  isDirty.value = hasUnsavedChanges()
}, { deep: true })

const handleBeforeUnload = (e) => {
  if (isDirty.value) {
    e.preventDefault()
    e.returnValue = ''
  }
}

onMounted(() => {
  window.addEventListener('beforeunload', handleBeforeUnload)
  Promise.all([loadProjects(), loadPositions()]).then(() => loadOriginalData())
})

onUnmounted(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
})

onActivated(async () => {
  await Promise.all([loadProjects(), loadPositions()])
  if (!isDirty.value) {
    loadOriginalData()
  }
})

onBeforeRouteLeave((to, from, next) => {
  if (isDirty.value) {
    ElMessageBox.confirm('您有未保存的修改，确定要离开吗？', '提示', {
      confirmButtonText: '确定离开',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => next()).catch(() => next(false))
  } else {
    next()
  }
})

watch(() => route.params.id, (newId) => {
  if (!route.path.startsWith('/pm/budget/upgrade')) return
  if (newId) {
    loadOriginalData()
  }
})

// --- 业务逻辑 ---
const calcLaborAmount = (row) => {
  row.amount = (row.hours || 0) * (row.costRate || 0)
}

const calcProcurementAmount = (row) => {
  row.amount = (row.qty || 0) * (row.unitPrice || 0) + (row.manualAdjustment || 0)
}

const handlePositionChange = async (row) => {
  if (!row.positionId) {
    row.roleCode = ''
    row.costRate = 0
    calcLaborAmount(row)
    return
  }
  try {
    const res = await getCurrentRateApi(row.positionId)
    const data = res.data
    const newRate = (data && data.costRate !== undefined && data.costRate !== null)
      ? parseFloat(data.costRate) : 0
    row.costRate = newRate
    const pos = positionList.value.find(p => p.id === row.positionId)
    if (pos) {
      row.roleCode = pos.name || ''
    }
    calcLaborAmount(row)
  } catch {
    row.costRate = 0
    calcLaborAmount(row)
  }
}

const removeFromAdjust = (category, index) => {
  adjustedItems[category].splice(index, 1)
}

const onProcurementAmountChange = (row) => {
  const baseAmount = (row.qty || 0) * (row.unitPrice || 0)
  row.manualAdjustment = (row.amount || 0) - baseAmount
}

const addItem = (category) => {
  const newItem = createAdjustedItem(category, null)
  adjustedItems[category].push(newItem)
}

// Auto-populate all original items into adjusted
const populateAllAdjustItems = () => {
  for (const cat of allCategories) {
    adjustedItems[cat.value].length = 0
    const items = allItems.value.filter(i => i.category === cat.value)
    for (const item of items) {
      const newItem = createAdjustedItem(cat.value, item)
      adjustedItems[cat.value].push(newItem)
    }
  }
}

const hasCategoryBudgetBelowActual = (category) => {
  return mergedCategoryTotal(category) < actualCostByCategory(category)
}

const findItemsBelowActual = () => {
  const belowItems = []
  // LABOR/PROCUREMENT: check category total >= actual total
  for (const cat of [{ label: '人工', value: 'LABOR' }, { label: '采购', value: 'PROCUREMENT' }]) {
    const catTotal = mergedCategoryTotal(cat.value)
    const catActual = actualCostByCategory(cat.value)
    if (catActual > 0 && catTotal < catActual) {
      belowItems.push({ category: cat.label, amount: catTotal, actual: catActual, isCategory: true })
    }
  }
  // OTHER: check per-item
  for (const cat of otherCategoryList) {
    for (const item of (adjustedItems[cat.value] || [])) {
      const actual = getActualAmount(item.originalId)
      if (actual > 0 && (item.amount || 0) < actual) {
        belowItems.push({ category: cat.label, amount: item.amount || 0, actual })
      }
    }
  }
  return belowItems
}

const handleUpgrade = async () => {
  try {
    await formRef.value.validate()

    // Validate adjusted amounts vs actual costs
    const belowItems = findItemsBelowActual()
    if (belowItems.length > 0) {
      const details = belowItems.slice(0, 3).map(i => {
        if (i.isCategory) return `${i.category}合计(调整后¥${formatMoney(i.amount)} < 实际¥${formatMoney(i.actual)})`
        return `${i.category}(调整后¥${formatMoney(i.amount)} < 实际¥${formatMoney(i.actual)})`
      }).join('；')
      const suffix = belowItems.length > 3 ? ` 等${belowItems.length}项` : ''
      ElMessage.error(`调整后预算不能低于实际已发生成本：${details}${suffix}`)
      return
    }

    submitting.value = true
    const data = {
      projectId: form.value.projectId,
      managementReserve: form.value.managementReserve.toString(),
      items: buildItemsTree()
    }

    await upgradeBudgetApi(route.params.id, data)
    ElMessage.success('预算升级草稿已保存')
    isDirty.value = false
    router.push('/pm/budget')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('升级失败')
  } finally {
    submitting.value = false
  }
}

const allCategories = [
  { label: '人工', value: 'LABOR' },
  { label: '采购', value: 'PROCUREMENT' },
  ...otherCategoryList
]

const buildItemsTree = () => {
  const items = []
  for (const cat of allCategories) {
    for (const item of (adjustedItems[cat.value] || [])) {
      items.push({
        category: cat.value,
        amount: (item.amount || 0).toString(),
        level: 1,
        roleCode: item.roleCode,
        positionId: item.positionId,
        hours: item.hours?.toString(),
        costRate: item.costRate?.toString(),
        bomItem: item.bomItem,
        qty: item.qty?.toString(),
        unitPrice: item.unitPrice?.toString()
      })
    }
    if (singleValueCategories.has(cat.value) && adjustedItems[cat.value].length > 0) continue
    for (const orig of (originalData.value.items || [])) {
      if (orig.category === cat.value && !adjustedOriginalIds.value.has(orig.id)) {
        items.push({
          category: orig.category,
          amount: (orig.budgetAmount || 0).toString(),
          level: 1,
          roleCode: orig.roleCode,
          positionId: orig.positionId,
          hours: orig.hours?.toString(),
          costRate: orig.costRate?.toString(),
          bomItem: orig.bomItem,
          qty: orig.qty?.toString(),
          unitPrice: orig.unitPrice?.toString()
        })
      }
    }
  }
  return items
}

const loadPositions = async () => {
  try {
    const res = await getPositionListApi()
    positionList.value = res.data || []
  } catch { /* ignore */ }
}

const loadProjects = async () => {
  try {
    const res = await getCharterListApi({ pageNum: 1, pageSize: 100 })
    projects.value = res.data?.records || []
  } catch { /* ignore */ }
}

const loadOriginalData = async () => {
  if (!route.params.id) return

  for (const cat of allCategories) {
    adjustedItems[cat.value].length = 0
  }

  try {
    const res = await getBudgetDetailWithItemsApi(route.params.id)
    originalData.value = res.data || { budget: {}, items: [] }

    const data = originalData.value
    form.value.projectId = data.budget?.projectId || ''
    form.value.managementReserve = parseFloat(data.budget?.managementReserve) || 0

    // Auto-populate all original items into adjusted
    populateAllAdjustItems()

    isDirty.value = false
  } catch (e) {
    ElMessage.error('加载原预算数据失败')
  }
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.project-info-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;
  margin-bottom: 8px;
}

.info-readonly {
  flex: 1;
  min-width: 0;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.info-item label {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
  min-width: 60px;
}

.info-value {
  font-size: 14px;
  color: #303133;
}

.waterfall-panel {
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 12px 16px;
  margin-bottom: 16px;
}

.waterfall-title {
  font-size: 14px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 10px;
}

.waterfall-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.wf-row {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  padding: 4px 0;
}

.wf-header {
  font-size: 12px;
  color: #909399;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 6px;
  margin-bottom: 2px;
}

.wf-label {
  width: 100px;
  flex-shrink: 0;
  color: #606266;
}

.wf-header .wf-label {
  color: #909399;
}

.wf-amount-label {
  width: 140px;
  text-align: right;
  font-size: 12px;
  color: #909399;
}

.wf-amount {
  width: 140px;
  text-align: right;
  color: #303133;
  font-variant-numeric: tabular-nums;
}

.wf-arrow {
  width: 30px;
  text-align: center;
  color: #909399;
}

.wf-diff-label {
  width: 200px;
  text-align: right;
  font-size: 12px;
  color: #909399;
}

.wf-diff-col {
  width: 200px;
  text-align: right;
  font-weight: bold;
  white-space: nowrap;
}

.wf-subtotal {
  border-top: 1px solid #ebeef5;
  padding-top: 6px;
  margin-top: 2px;
}

.wf-subtotal .wf-label,
.wf-total .wf-label {
  font-weight: bold;
  color: #303133;
}

.wf-total {
  border-top: 1px solid #dcdfe6;
  padding-top: 6px;
  margin-top: 2px;
  font-size: 14px;
  font-weight: bold;
}

.category-section {
  margin-bottom: 24px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  overflow: hidden;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
}

.section-title {
  font-size: 15px;
  font-weight: bold;
  color: #303133;
}

.section-body {
  display: flex;
  padding: 16px;
}

.section-left {
  flex: 1;
  min-width: 0;
  padding: 0 12px;
  border-right: 1px dashed #dcdfe6;
}

.section-right {
  flex: 2;
  min-width: 0;
  padding: 0 12px;
}

.section-subtitle {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  font-weight: bold;
  color: #606266;
  margin-bottom: 8px;
  padding: 2px 0 2px 10px;
  border-left: 3px solid #409eff;
}

.diff-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border-radius: 4px;
  font-weight: bold;
  font-size: 14px;
  background: #fff;
  white-space: nowrap;
}

.diff-percent {
  font-size: 12px;
  font-weight: normal;
  opacity: 0.85;
}

.category-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
  padding: 8px 0;
  border-top: 1px solid #ebeef5;
  font-size: 13px;
  color: #606266;
}

.actual-cost-tag {
  color: #e6a23c;
  font-weight: bold;
}

.validation-hint {
  font-size: 11px;
  color: #e6a23c;
  font-weight: normal;
}

.other-categories-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
  width: 100%;
}

.other-cat-item {
  flex: 1 1 0;
  min-width: 0;
}

.other-cat-spacer,
.other-plus-spacer {
  visibility: hidden;
}

.other-cat-label {
  display: block;
  font-size: 13px;
  color: #606266;
  margin-bottom: 4px;
}

.other-cat-value {
  display: block;
  font-size: 12px;
  color: #303133;
}

.other-plus-operator {
  font-size: 18px;
  font-weight: bold;
  color: #909399;
  align-self: flex-end;
  margin-bottom: 8px;
}

.other-cat-controls {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.empty-placeholder {
  color: #909399;
  font-size: 13px;
  padding: 16px 0;
  text-align: center;
  width: 100%;
}

.budget-summary-row {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 24px;
  padding: 8px 0;
}

.summary-actual-row {
  border-top: 1px dashed #dcdfe6;
  padding-top: 8px;
}

.summary-item {
  text-align: center;
  min-width: 140px;
}

.summary-item.input-item {
  min-width: 200px;
}

.summary-item label {
  display: block;
  font-size: 12px;
  color: #606266;
  margin-bottom: 6px;
}

.summary-value {
  font-size: 14px;
  font-weight: bold;
  color: #303133;
}

.summary-value.highlight {
  color: #409eff;
}

.summary-operator {
  font-size: 16px;
  font-weight: bold;
  color: #909399;
  margin-bottom: 4px;
}

.reserve-input {
  width: 100%;
}

.form-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.actual-cost-col {
  color: #67C23A;
  font-weight: bold;
}

.actual-cost-inline {
  color: #67C23A;
  font-size: 12px;
  font-weight: normal;
}

.actual-cost-title {
  color: #67C23A;
  font-size: 13px;
  font-weight: bold;
}

.actual-cost-sub {
  color: #67C23A;
  font-size: 12px;
  margin-top: 2px;
}

:deep(.zero-rate .el-input__inner) {
  color: #f56c6c;
  font-weight: bold;
}
</style>
