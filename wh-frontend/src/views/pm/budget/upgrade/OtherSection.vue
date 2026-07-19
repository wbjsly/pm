<template>
  <!-- 其他科目 -->
  <div class="category-section">
    <div class="section-header">
      <span class="section-title">其他科目</span>
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
        <div class="other-categories-row">
          <template v-for="(cat, idx) in otherCategoryList.slice(0, 3)" :key="cat.value">
            <div class="other-cat-item">
              <label class="other-cat-label">{{ cat.label }}</label>
              <span class="other-cat-value">¥ {{ formatMoney(leftAmounts[cat.value]) }}</span>
            </div>
            <span class="other-plus-operator">+</span>
          </template>
        </div>
        <div class="other-categories-row" style="margin-top: 8px;">
          <template v-for="(cat, idx) in otherCategoryList.slice(3)" :key="cat.value">
            <div class="other-cat-item">
              <label class="other-cat-label">{{ cat.label }}</label>
              <span class="other-cat-value">¥ {{ formatMoney(leftAmounts[cat.value]) }}</span>
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
            <span class="actual-cost-title">实际已发生成本：¥{{ formatMoney(totalActual) }}</span>
            <span> | 调整后预算合计：¥ {{ formatMoney(rightTotal) }}</span>
          </div>
        </div>
        <div class="other-categories-row" v-if="visibleOtherCategories.length > 0">
          <template v-for="(cat, idx) in otherCategoryList.slice(0, 3)" :key="cat.value">
            <div v-if="items[cat.value].length > 0" class="other-cat-item other-cat-adjust-item">
              <label class="other-cat-label">{{ cat.label }}（<span class="actual-cost-inline">实际已发生：¥{{ formatMoney(actualAmounts[cat.value]) }}</span>）</label>
              <el-input-number
                v-model="items[cat.value][0].amount"
                :min="parseFloat(actualAmounts[cat.value])"
                :precision="2" :step="1000" size="small" controls-position="right"
              />
            </div>
            <span class="other-plus-operator">+</span>
          </template>
        </div>
        <div class="other-categories-row" style="margin-top: 8px;">
          <template v-for="(cat, idx) in otherCategoryList.slice(3)" :key="cat.value">
            <div v-if="items[cat.value].length > 0" class="other-cat-item other-cat-adjust-item">
              <label class="other-cat-label">{{ cat.label }}（<span class="actual-cost-inline">实际已发生：¥{{ formatMoney(actualAmounts[cat.value]) }}</span>）</label>
              <el-input-number
                v-model="items[cat.value][0].amount"
                :min="parseFloat(actualAmounts[cat.value])"
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
</template>

<script setup>
import { formatMoney, diffSign, diffLabel, diffStyle, otherCategoryList } from './format'

defineProps({
  leftAmounts: { type: Object, default: () => ({}) },
  leftTotal: { type: Number, default: 0 },
  diff: { type: Object, required: true },
  items: { type: Object, required: true },
  actualAmounts: { type: Object, default: () => ({}) },
  totalActual: { type: Number, default: 0 },
  rightTotal: { type: Number, default: 0 },
  visibleOtherCategories: { type: Array, default: () => [] }
})
</script>

<style scoped>
@import './section-common.css';

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

.actual-cost-inline {
  color: #e6a23c;
  font-size: 12px;
  font-weight: normal;
}
</style>
