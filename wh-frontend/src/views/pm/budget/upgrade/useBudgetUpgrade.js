import { ref, computed, reactive, onMounted, onUnmounted, onActivated, watch } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBudgetDetailWithItemsApi, upgradeBudgetApi } from '@/api/pm/budget'
import { getCharterListApi } from '@/api/pm/charter'
import { getPositionListApi, getCurrentRateApi } from '@/api/system/costQuota'
import { formatMoney, diffColor, diffSign, diffLabel, diffStyle, otherCategoryList, allCategories } from './format'

/**
 * 预算升级页的全部业务逻辑（数据加载、调整项编辑、差异计算、未保存检测、提交）。
 * 视图组件仅负责展示，通过返回的响应式状态与方法驱动。
 */
export function useBudgetUpgrade() {
  const route = useRoute()
  const router = useRouter()
  const submitting = ref(false)
  const projects = ref([])
  const positionList = ref([])

  const originalData = ref({ budget: {}, items: [] })

  const originalVersion = computed(() => originalData.value.budget?.version || '')

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

  const getLeftOtherAmount = (category) => {
    const items = allItems.value.filter(i => i.category === category)
    return items.reduce((s, i) => s + (i.budgetAmount || 0), 0)
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

  // --- 瀑布面板行数据（预算变更汇总） ---
  const waterfallRows = computed(() => {
    const d = diffData.value
    const rows = [
      { key: 'labor', label: '人工', left: leftLaborTotal.value, right: categoryTotal('LABOR'),
        diff: d.labor.diff, percent: d.labor.percent, actual: actualCostByCategory('LABOR') },
      { key: 'procurement', label: '采购', left: leftProcurementTotal.value, right: categoryTotal('PROCUREMENT'),
        diff: d.procurement.diff, percent: d.procurement.percent, actual: actualCostByCategory('PROCUREMENT') },
      { key: 'other', label: '其他', left: leftOtherTotal.value, right: otherTotal.value,
        diff: d.other.diff, percent: d.other.percent, actual: totalOtherActualCost() },
      { key: 'subTotal', label: '科目差异合计', left: leftCostBaseline.value, right: costBaseline.value,
        diff: d.subTotal.diff, percent: d.subTotal.percent, actual: totalActualCost.value, rowClass: 'wf-subtotal' },
      { key: 'managementReserve', label: '管理储备',
        left: parseFloat(originalData.value.budget?.managementReserve) || 0,
        right: form.value.managementReserve || 0,
        diff: d.managementReserve.diff, percent: d.managementReserve.percent, actual: 0,
        visible: Math.abs(d.managementReserve.diff) > 0.01 },
      { key: 'total', label: '总预算差异', left: leftTotalBudget.value, right: totalBudget.value,
        diff: d.total.diff, percent: d.total.percent, actual: totalActualCost.value, rowClass: 'wf-total' }
    ]
    for (const r of rows) {
      r.bmaDiff = r.right - r.actual
      r.bmaPercent = r.actual === 0 ? 0 : (r.bmaDiff / r.actual) * 100
      if (r.visible === undefined) r.visible = true
    }
    return rows
  })

  // --- 其他科目左右两侧金额映射（供 OtherSection 使用） ---
  const leftOtherAmounts = computed(() => {
    const map = {}
    for (const cat of otherCategoryList) {
      map[cat.value] = getLeftOtherAmount(cat.value)
    }
    return map
  })

  const actualAmountsByCategory = computed(() => {
    const map = {}
    for (const cat of otherCategoryList) {
      map[cat.value] = actualCostByCategory(cat.value)
    }
    return map
  })

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

  const handleUpgrade = async (formEl) => {
    try {
      await formEl.validate()

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

  return {
    // 状态
    form, rules, submitting, projects, positionList, originalData, adjustedItems, isDirty,
    // 计算属性
    originalVersion, allItems, leftLaborItems, leftProcurementItems,
    leftLaborTotal, leftProcurementTotal, leftOtherTotal, leftCostBaseline, leftTotalBudget,
    projectName, visibleOtherCategories, totalActualCost, actualAmountMap, adjustedOriginalIds,
    costBaseline, totalBudget, otherTotal, diffData, waterfallRows,
    leftOtherAmounts, actualAmountsByCategory,
    // 方法
    actualCostByCategory, totalOtherActualCost, getActualAmount, mergedCategoryTotal,
    getLeftOtherAmount, categoryTotal,
    calcLaborAmount, calcProcurementAmount, handlePositionChange, removeFromAdjust,
    onProcurementAmountChange, addItem, populateAllAdjustItems, findItemsBelowActual,
    handleUpgrade, buildItemsTree, loadPositions, loadProjects, loadOriginalData,
    // 展示辅助（透传自 format.js，便于模板与测试直接使用）
    formatMoney, diffColor, diffSign, diffLabel, diffStyle,
    otherCategoryList, allCategories
  }
}
