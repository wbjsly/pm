<template>
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
            <span class="info-value">{{ budgetCode }}</span>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="info-item">
            <label>版本号</label>
            <span class="info-value">{{ version }}</span>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="info-item">
            <label>状态</label>
            <el-tag :type="dictStore.getTagType('BUDGET_STATUS', status)">
              {{ dictStore.getLabel('BUDGET_STATUS', status) }}
            </el-tag>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { useDictStore } from '@/store/dict'

defineProps({
  projectName: { type: String, default: '' },
  budgetCode: { type: String, default: '' },
  version: { type: String, default: '' },
  status: { type: String, default: '' }
})

const dictStore = useDictStore()
</script>

<style scoped>
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
</style>
