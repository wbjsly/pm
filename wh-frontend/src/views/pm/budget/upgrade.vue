<template>
  <div class="budget-upgrade">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>升级预算</span>
          <el-button @click="$router.back()">返回</el-button>
        </div>
      </template>

      <!-- 项目信息区 -->
      <UpgradeInfoBar
        :project-name="projectName"
        :budget-code="originalData.budget?.budgetCode || ''"
        :version="originalVersion"
        :status="originalData.budget?.status"
      />

      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <!-- 预算变更汇总 -->
        <BudgetWaterfallPanel :rows="waterfallRows" />

        <el-divider>预算科目</el-divider>

        <!-- 人工科目 -->
        <LaborSection
          :left-items="leftLaborItems"
          :left-total="leftLaborTotal"
          :diff="diffData.labor"
          :items="adjustedItems.LABOR"
          :position-list="positionList"
          :actual-cost="actualCostByCategory('LABOR')"
          :right-total="categoryTotal('LABOR')"
          @add="addItem('LABOR')"
          @remove="(index) => removeFromAdjust('LABOR', index)"
          @position-change="handlePositionChange"
          @calc="calcLaborAmount"
        />

        <!-- 采购科目 -->
        <ProcurementSection
          :left-items="leftProcurementItems"
          :left-total="leftProcurementTotal"
          :diff="diffData.procurement"
          :items="adjustedItems.PROCUREMENT"
          :actual-cost="actualCostByCategory('PROCUREMENT')"
          :right-total="categoryTotal('PROCUREMENT')"
          @add="addItem('PROCUREMENT')"
          @remove="(index) => removeFromAdjust('PROCUREMENT', index)"
          @calc="calcProcurementAmount"
          @amount-change="onProcurementAmountChange"
        />

        <!-- 其他科目 -->
        <OtherSection
          :left-amounts="leftOtherAmounts"
          :left-total="leftOtherTotal"
          :diff="diffData.other"
          :items="adjustedItems"
          :actual-amounts="actualAmountsByCategory"
          :total-actual="totalOtherActualCost()"
          :right-total="otherTotal"
          :visible-other-categories="visibleOtherCategories"
        />

        <el-divider>预算汇总</el-divider>

        <BudgetSummarySection
          v-model="form.managementReserve"
          :left-total-budget="leftTotalBudget"
          :left-cost-baseline="leftCostBaseline"
          :left-reserve="parseFloat(originalData.budget?.managementReserve) || 0"
          :total-actual-cost="totalActualCost"
          :total-budget="totalBudget"
          :cost-baseline="costBaseline"
          :diff="diffData.total"
        />

        <div class="form-actions">
          <el-button type="primary" @click="handleUpgrade(formRef)" :loading="submitting">升级</el-button>
          <el-button @click="$router.back()">返回</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import UpgradeInfoBar from './upgrade/UpgradeInfoBar.vue'
import BudgetWaterfallPanel from './upgrade/BudgetWaterfallPanel.vue'
import LaborSection from './upgrade/LaborSection.vue'
import ProcurementSection from './upgrade/ProcurementSection.vue'
import OtherSection from './upgrade/OtherSection.vue'
import BudgetSummarySection from './upgrade/BudgetSummarySection.vue'
import { useBudgetUpgrade } from './upgrade/useBudgetUpgrade'

const formRef = ref(null)

const {
  // 状态
  form, rules, submitting, positionList, originalData, adjustedItems, isDirty,
  // 计算属性
  originalVersion, projectName,
  leftLaborItems, leftProcurementItems,
  leftLaborTotal, leftProcurementTotal, leftOtherTotal, leftCostBaseline, leftTotalBudget,
  visibleOtherCategories, totalActualCost, costBaseline, totalBudget, otherTotal,
  diffData, waterfallRows, leftOtherAmounts, actualAmountsByCategory,
  // 方法
  actualCostByCategory, totalOtherActualCost, categoryTotal,
  calcLaborAmount, calcProcurementAmount, handlePositionChange, removeFromAdjust,
  onProcurementAmountChange, addItem, handleUpgrade,
  // 展示辅助
  diffColor, diffSign, diffStyle // eslint-disable-line no-unused-vars
} = useBudgetUpgrade()
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.form-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}
</style>
