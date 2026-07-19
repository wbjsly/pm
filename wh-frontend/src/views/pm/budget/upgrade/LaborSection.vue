<template>
  <!-- 人工科目 -->
  <div class="category-section">
    <div class="section-header">
      <span class="section-title">人工</span>
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
          <el-table-column label="岗位" width="140">
            <template #default="{ row }">
              <span>{{ row.positionName || roleLabel(row.roleCode) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="工时(小时)" width="100">
            <template #default="{ row }">{{ row.hours }}</template>
          </el-table-column>
          <el-table-column label="成本定额(元/时)" width="120">
            <template #default="{ row }">{{ formatMoney(row.costRate) }}</template>
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
            <span class="actual-cost-title">实际已发生成本：¥{{ formatMoney(actualCost) }}</span>
            <span> | 调整后预算合计：¥ {{ formatMoney(rightTotal) }}</span>
          </div>
        </div>
        <el-table v-if="items.length > 0" :data="items" size="small" style="width: 100%">
          <el-table-column label="岗位" min-width="160">
            <template #default="{ row }">
              <el-select v-model="row.positionId" size="small" filterable placeholder="请选择岗位"
                @change="emit('position-change', row)">
                <el-option v-for="p in positionList" :key="p.id" :label="p.name" :value="p.id" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="工时(小时)" min-width="100">
            <template #default="{ row }">
              <el-input-number v-model="row.hours" :min="0" :step="8" size="small" controls-position="right" @change="emit('calc', row)" />
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
              <el-button link type="danger" size="small" @click="emit('remove', $index)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="empty-placeholder">暂未调整</div>
        <el-button size="small" @click="emit('add')" style="margin-top: 8px">
          <el-icon><Plus /></el-icon> 添加角色
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Plus } from '@element-plus/icons-vue'
import { formatMoney, diffSign, diffLabel, diffStyle, roleLabel } from './format'

defineProps({
  leftItems: { type: Array, default: () => [] },
  leftTotal: { type: Number, default: 0 },
  diff: { type: Object, required: true },
  items: { type: Array, default: () => [] },
  positionList: { type: Array, default: () => [] },
  actualCost: { type: Number, default: 0 },
  rightTotal: { type: Number, default: 0 }
})

const emit = defineEmits(['add', 'remove', 'position-change', 'calc'])
</script>

<style scoped>
@import './section-common.css';

:deep(.zero-rate .el-input__inner) {
  color: #f56c6c;
  font-weight: bold;
}
</style>
