<template>
  <!-- 采购科目 -->
  <div class="category-section">
    <div class="section-header">
      <span class="section-title">采购</span>
      <span class="diff-badge" :style="diffStyle(diff.diff)">
        调整后预算-调整前预算：{{ diffLabel(diff.diff) }}¥{{ formatMoney(Math.abs(diff.diff)) }}
        <span class="diff-percent">({{ diffSign(diff.percent) }}{{ Math.abs(diff.percent).toFixed(1) }}%)</span>
      </span>
    </div>
    <div class="section-body">
      <div class="section-left">
        <div class="section-subtitle">
          <span>调整前预算</span>
          <span>调整前预算合计：¥ {{ formatMoney(leftTotal) }}</span>
        </div>
        <el-table :data="leftItems" size="small">
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
            <span class="actual-cost-title">实际已发生成本：¥{{ formatMoney(actualCost) }}</span>
            <span> | 调整后预算合计：¥ {{ formatMoney(rightTotal) }}</span>
          </div>
        </div>
        <el-table v-if="items.length > 0" :data="items" size="small" style="width: 100%">
          <el-table-column label="BOM项" min-width="140">
            <template #default="{ row }">
              <el-input v-model="row.bomItem" size="small" placeholder="BOM项名称" />
            </template>
          </el-table-column>
          <el-table-column label="数量" min-width="80">
            <template #default="{ row }">
              <el-input-number v-model="row.qty" :min="0" size="small" @change="emit('calc', row)" />
            </template>
          </el-table-column>
          <el-table-column label="单价" min-width="100">
            <template #default="{ row }">
              <el-input-number v-model="row.unitPrice" :min="0" :precision="2" size="small" @change="emit('calc', row)" />
            </template>
          </el-table-column>
          <el-table-column label="金额" min-width="110">
            <template #default="{ row }">
              <el-input-number v-model="row.amount" :min="0" :step="1000" size="small" controls-position="right" @change="emit('amount-change', row)" />
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="70">
            <template #default="{ $index }">
              <el-button link type="danger" size="small" @click="emit('remove', $index)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="empty-placeholder">暂未调整</div>
        <el-button size="small" @click="emit('add')" style="margin-top: 8px">
          <el-icon><Plus /></el-icon> 添加采购项
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Plus } from '@element-plus/icons-vue'
import { formatMoney, diffSign, diffLabel, diffStyle } from './format'

defineProps({
  leftItems: { type: Array, default: () => [] },
  leftTotal: { type: Number, default: 0 },
  diff: { type: Object, required: true },
  items: { type: Array, default: () => [] },
  actualCost: { type: Number, default: 0 },
  rightTotal: { type: Number, default: 0 }
})

const emit = defineEmits(['add', 'remove', 'calc', 'amount-change'])
</script>

<style scoped>
@import './section-common.css';
</style>
