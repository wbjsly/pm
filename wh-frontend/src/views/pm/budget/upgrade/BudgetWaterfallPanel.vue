<template>
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
        <span class="wf-actual-label">实际成本</span>
        <span class="wf-ba-label">预算-实际</span>
      </div>
      <template v-for="row in rows" :key="row.key">
        <div v-if="row.visible" class="wf-row" :class="row.rowClass">
          <span class="wf-label">{{ row.label }}</span>
          <span class="wf-amount">¥{{ formatMoney(row.left) }}</span>
          <span class="wf-arrow">→</span>
          <span class="wf-amount">¥{{ formatMoney(row.right) }}</span>
          <span class="wf-diff-col" :style="diffStyle(row.diff)">
            {{ diffSign(row.diff) }}¥{{ formatMoney(Math.abs(row.diff)) }}
            ({{ diffSign(row.percent) }}{{ Math.abs(row.percent).toFixed(1) }}%)
          </span>
          <span class="wf-actual-col">¥{{ formatMoney(row.actual) }}</span>
          <span class="wf-ba-col" :style="diffStyle(row.bmaDiff)">
            {{ diffSign(row.bmaDiff) }}¥{{ formatMoney(Math.abs(row.bmaDiff)) }}
            ({{ diffSign(row.bmaPercent) }}{{ Math.abs(row.bmaPercent).toFixed(1) }}%)
          </span>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { formatMoney, diffSign, diffStyle } from './format'

defineProps({
  rows: { type: Array, required: true }
})
</script>

<style scoped>
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

.wf-diff-col {
  width: 180px;
  text-align: right;
  font-weight: bold;
  white-space: nowrap;
}

.wf-diff-label {
  width: 180px;
  text-align: right;
  font-size: 12px;
  color: #909399;
}

.wf-actual-label {
  width: 120px;
  text-align: right;
  font-size: 12px;
  color: #909399;
}

.wf-actual-col {
  width: 120px;
  text-align: right;
  color: #e6a23c;
  font-weight: bold;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.wf-ba-label {
  width: 180px;
  text-align: right;
  font-size: 12px;
  color: #909399;
}

.wf-ba-col {
  width: 180px;
  text-align: right;
  font-weight: bold;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
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
</style>
