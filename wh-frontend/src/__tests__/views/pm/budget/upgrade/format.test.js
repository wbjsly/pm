import { describe, it, expect } from 'vitest'
import {
  formatMoney, diffColor, diffSign, diffLabel, diffStyle,
  roleLabel, otherCategoryList, allCategories,
} from '@/views/pm/budget/upgrade/format'

describe('budget upgrade format helpers', () => {
  it('formatMoney 格式化金额', () => {
    expect(formatMoney(1000)).toBe('1,000.00')
    expect(formatMoney(0)).toBe('0.00')
    expect(formatMoney(null)).toBe('0.00')
    expect(formatMoney(1234.5)).toBe('1,234.50')
  })

  it('diffColor 返回差异颜色', () => {
    expect(diffColor(10)).toBe('#F56C6C')
    expect(diffColor(-10)).toBe('#67C23A')
    expect(diffColor(0)).toBe('#303133')
  })

  it('diffSign 返回符号', () => {
    expect(diffSign(10)).toBe('+')
    expect(diffSign(-10)).toBe('-')
    expect(diffSign(0)).toBe('')
  })

  it('diffLabel 返回中文差异标签', () => {
    expect(diffLabel(10)).toBe('增加')
    expect(diffLabel(-10)).toBe('降低')
    expect(diffLabel(0)).toBe('')
  })

  it('diffStyle 返回颜色样式', () => {
    expect(diffStyle(5)).toEqual({ color: '#F56C6C' })
    expect(diffStyle(-5)).toEqual({ color: '#67C23A' })
  })

  it('roleLabel 返回角色中文', () => {
    expect(roleLabel('DEV')).toBe('开发(DEV)')
    expect(roleLabel('QA')).toBe('测试(QA)')
    expect(roleLabel('UNKNOWN')).toBe('UNKNOWN')
  })

  it('otherCategoryList 与 allCategories 定义完整', () => {
    expect(otherCategoryList.map(c => c.value)).toEqual(['TRAVEL', 'BUSINESS', 'ENTERTAINMENT', 'ACTIVITY', 'OTHER'])
    expect(allCategories[0].value).toBe('LABOR')
    expect(allCategories[1].value).toBe('PROCUREMENT')
    expect(allCategories.length).toBe(7)
  })
})
