<template>
  <div class="wbs-history">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span style="font-weight: bold; font-size: 16px;">版本历史: {{ wbsName }} ({{ wbsCode }})</span>
          <el-button @click="$router.back()">返回</el-button>
        </div>
      </template>

      <el-table :data="versions" stripe v-loading="loading">
        <el-table-column prop="versionNumber" label="版本号" width="100">
          <template #default="{ row }">
            {{ row.versionNumber }}
            <el-tag v-if="row.actualStartDate || row.actualEndDate" type="success" size="small" style="margin-left: 4px;">含实际数据</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="plannedStartDate" label="计划开始" width="120" />
        <el-table-column prop="plannedEndDate" label="计划结束" width="120" />
        <el-table-column prop="actualStartDate" label="实际开始" width="120">
          <template #default="{ row }">{{ row.actualStartDate || '-' }}</template>
        </el-table-column>
        <el-table-column prop="actualEndDate" label="实际结束" width="120">
          <template #default="{ row }">{{ row.actualEndDate || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createDate" label="创建时间" width="160" />
        <el-table-column prop="createBy" label="创建人" width="100" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getWbsVersionsApi, getWbsDetailApi } from '@/api/pm/wbs'
import { ElMessage } from 'element-plus'

const route = useRoute()
const versions = ref([])
const loading = ref(false)
const wbsName = ref('')
const wbsCode = ref('')

const loadVersions = async () => {
  loading.value = true
  try {
    const detailRes = await getWbsDetailApi(route.params.id)
    wbsName.value = detailRes.data.name
    wbsCode.value = detailRes.data.wbsCode

    const res = await getWbsVersionsApi(route.params.id)
    versions.value = res.data || []
  } catch {
    ElMessage.error('加载版本历史失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadVersions)
</script>
