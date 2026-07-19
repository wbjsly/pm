/**
 * 预算升级页共享的展示辅助函数与常量。
 */

export const formatMoney = (val) => {
  const num = val || 0
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export const diffColor = (val) => val > 0 ? '#F56C6C' : val < 0 ? '#67C23A' : '#303133'
export const diffSign = (val) => val > 0 ? '+' : val < 0 ? '-' : ''
export const diffLabel = (val) => val > 0 ? '增加' : val < 0 ? '降低' : ''
export const diffStyle = (val) => ({ color: diffColor(val) })

export const roleLabel = (code) => {
  const map = { DEV: '开发(DEV)', QA: '测试(QA)', BA: '产品经理(BA)', ARCH: '架构师(ARCH)', PM: '项目经理(PM)' }
  return map[code] || code
}

export const otherCategoryList = [
  { label: '差旅', value: 'TRAVEL' },
  { label: '商务费用', value: 'BUSINESS' },
  { label: '客户招待费', value: 'ENTERTAINMENT' },
  { label: '活动费', value: 'ACTIVITY' },
  { label: '其他', value: 'OTHER' }
]

export const allCategories = [
  { label: '人工', value: 'LABOR' },
  { label: '采购', value: 'PROCUREMENT' },
  ...otherCategoryList
]
