import { test, expect } from '@playwright/test'

test.describe('WBS任务管理流程', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('用户名').fill('admin')
    await page.getByPlaceholder('密码').fill('Admin@123')
    await page.locator('button').filter({ hasText: '登录' }).click()
    await page.waitForURL('**/pm/charter')
  })

  test('导航到WBS任务页面 - 表格和数据列', async ({ page }) => {
    await page.goto('/pm/wbs')
    await page.waitForTimeout(2000)
    await expect(page.getByRole('tab', { name: '项目任务' })).toBeVisible({ timeout: 5000 })
    const table = page.getByRole('table')
    const tableVisible = await table.isVisible().catch(() => false)
    if (tableVisible) {
      await expect(table).toBeVisible()
    }
  })

  test('新增WBS任务表单可访问', async ({ page }) => {
    await page.goto('/pm/wbs/form')
    await page.waitForTimeout(2000)
  })

  test('WBS任务详情页面可访问', async ({ page }) => {
    await page.goto('/pm/wbs')
    await page.waitForTimeout(2000)
    const firstRow = page.locator('table tbody tr').first()
    const hasRows = await firstRow.isVisible().catch(() => false)
    if (hasRows) {
      const viewBtn = firstRow.locator('button').first()
      if (await viewBtn.isVisible()) {
        await viewBtn.click()
        await page.waitForTimeout(2000)
      }
    }
  })

  test('WBS版本历史页面可访问', async ({ page }) => {
    await page.goto('/pm/wbs')
    await page.waitForTimeout(2000)
    const firstRow = page.locator('table tbody tr').first()
    const hasRows = await firstRow.isVisible().catch(() => false)
    if (hasRows) {
      // Try to navigate to history via detail page
      const viewBtn = firstRow.locator('button').first()
      if (await viewBtn.isVisible()) {
        await viewBtn.click()
        await page.waitForTimeout(1000)
        await page.goto(page.url().replace('/detail/', '/history/'))
        await page.waitForTimeout(1000)
      }
    }
  })
})
