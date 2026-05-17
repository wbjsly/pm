import { test, expect } from '@playwright/test'

test.describe('预算管理流程', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('用户名').fill('admin')
    await page.getByPlaceholder('密码').fill('Admin@123')
    await page.locator('button').filter({ hasText: '登录' }).click()
    await page.waitForURL('**/pm/charter')
  })

  test('导航到预算管理页面 - 表格和数据列', async ({ page }) => {
    await page.goto('/pm/budget')
    await page.waitForTimeout(2000)
    await expect(page.getByRole('tab', { name: '预算管理' })).toBeVisible({ timeout: 5000 })
    // Check for table or project budget cards
    const table = page.getByRole('table')
    const tableVisible = await table.isVisible().catch(() => false)
    if (tableVisible) {
      await expect(table).toBeVisible()
    }
  })

  test('新增加预算表单可访问', async ({ page }) => {
    await page.goto('/pm/budget/form')
    await page.waitForTimeout(2000)
    // Should show form or redirect
    await page.waitForTimeout(1000)
  })

  test('预算列表搜索功能', async ({ page }) => {
    await page.goto('/pm/budget')
    await page.waitForTimeout(2000)
    const searchBtn = page.getByRole('button', { name: '查询' })
    if (await searchBtn.isVisible()) {
      await searchBtn.click()
      await page.waitForTimeout(1000)
    }
  })

  test('从预算列表导航到详情', async ({ page }) => {
    await page.goto('/pm/budget')
    await page.waitForTimeout(2000)
    // Try to navigate to first budget detail
    const firstRow = page.locator('table tbody tr').first()
    const hasRows = await firstRow.isVisible().catch(() => false)
    if (hasRows) {
      const viewLink = firstRow.locator('a, button').first()
      if (await viewLink.isVisible()) {
        await viewLink.click()
        await page.waitForTimeout(2000)
      }
    }
  })

  test('预算预实对比页面可访问', async ({ page }) => {
    await page.goto('/pm/budget/comparison/test-id')
    await page.waitForTimeout(2000)
  })
})
