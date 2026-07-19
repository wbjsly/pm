<template>
  <!-- 预算汇总 -->
  <div class="category-section">
    <div class="section-header">
      <span class="section-title">预算汇总</span>
      <span class="diff-badge" :style="diffStyle(diff.diff)">
        {{ diffSign(diff.diff) }}¥{{ formatMoney(Math.abs(diff.diff)) }}
        <span class="diff-percent">({{ diffSign(diff.percent) }}{{ Math.abs(diff.percent).toFixed(1) }}%)</span>
      </span>
    </div>
    <div class="section-body">
      <div class="section-left">
        <div class="budget-summary-row">
          <div class="summary-item">
            <label>总预算</label>
            <div class="summary-value">¥ {{ formatMoney(leftTotalBudget) }}</div>
          </div>
          <div class="summary-operator">=</div>
          <div class="summary-item">
            <label>项目直接预算</label>
            <div class="summary-value">¥ {{ formatMoney(leftCostBaseline) }}</div>
          </div>
          <div class="summary-operator">+</div>
          <div class="summary-item">
            <label>项目管理预算</label>
            <div class="summary-value">¥ {{ formatMoney(leftReserve) }}</div>
          </div>
        </div>
        <div class="budget-summary-row summary-actual-row">
          <div class="summary-item">
            <label>实际成本总额</label>
            <div class="summary-value highlight" style="color: #e6a23c;">¥ {{ formatMoney(totalActualCost) }}</div>
          </div>
          <div class="summary-operator">=</div>
          <div class="summary-item">
            <label>项目直接实际成本</label>
            <div class="summary-value" style="color: #e6a23c;">¥ {{ formatMoney(totalActualCost) }}</div>
          </div>
          <div class="summary-operator">+</div>
          <div class="summary-item">
            <label>管理实际成本</label>
            <div class="summary-value" style="color: #e6a23c;">¥ 0.00</div>
          </div>
        </div>
      </div>
      <div class="section-right">
        <div class="budget-summary-row">
          <div class="summary-item">
            <label>总预算</label>
            <div class="summary-value">¥ {{ formatMoney(totalBudget) }}</div>
          </div>
          <div class="summary-operator">=</div>
          <div class="summary-item">
            <label>项目直接预算</label>
            <div class="summary-value">¥ {{ formatMoney(costBaseline) }}</div>
          </div>
          <div class="summary-operator">+</div>
          <div class="summary-item input-item">
            <label>项目管理预算</label>
            <el-input-number v-model="reserve" :min="0" :precision="2" class="reserve-input" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { formatMoney, diffSign, diffStyle } from './format'

const props = defineProps({
  leftTotalBudget: { type: Number, default: 0 },
  leftCostBaseline: { type: Number, default: 0 },
  leftReserve: { type: Number, default: 0 },
  totalActualCost: { type: Number, default: 0 },
  totalBudget: { type: Number, default: 0 },
  costBaseline: { type: Number, default: 0 },
  diff: { type: Object, required: true },
  modelValue: { type: Number, default: 0 }
})

const emit = defineEmits(['update:modelValue'])

const reserve = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})
</script>

<style scoped>
@import './section-common.css';

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
</style>
