import { test, expect } from '@playwright/test'

test.describe('预算管理流程', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('用户名').fill('admin')
    await page.getByPlaceholder('密码').fill('Admin@123')
    await page.locator('button').filter({ hasText: '登录' }).click()
    await page.waitForURL('**/pm/charter')
  })

  test('导航到预算管理页面', async ({ page }) => {
    await page.goto('/pm/budget')
    await page.waitForTimeout(2000)
    await expect(page.getByRole('tab', { name: '预算管理' })).toBeVisible({ timeout: 5000 })
    await expect(page.getByRole('columnheader', { name: '预算编码' })).toBeVisible({ timeout: 3000 })
  })
})
