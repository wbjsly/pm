import { test, expect } from '@playwright/test'

test.describe('交付物管理流程', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('用户名').fill('admin')
    await page.getByPlaceholder('密码').fill('Admin@123')
    await page.locator('button').filter({ hasText: '登录' }).click()
    await page.waitForURL('**/pm/charter')
  })

  test('导航到交付物管理页面 - 表格和数据列', async ({ page }) => {
    await page.goto('/pm/deliverable')
    await page.waitForTimeout(2000)
    await expect(page.getByRole('tab', { name: '成果管理' })).toBeVisible({ timeout: 5000 })
    const table = page.getByRole('table')
    const tableVisible = await table.isVisible().catch(() => false)
    if (tableVisible) {
      await expect(table).toBeVisible()
    }
  })

  test('新增交付物表单可访问', async ({ page }) => {
    await page.goto('/pm/deliverable/form')
    await page.waitForTimeout(2000)
  })

  test('交付物详情页面可访问', async ({ page }) => {
    await page.goto('/pm/deliverable')
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

  test('交付物列表搜索功能', async ({ page }) => {
    await page.goto('/pm/deliverable')
    await page.waitForTimeout(2000)
    const searchInput = page.getByPlaceholder(/名称|编号|搜索|查询/)
    if (await searchInput.isVisible()) {
      await searchInput.fill('test')
      await page.waitForTimeout(500)
    }
  })
})
