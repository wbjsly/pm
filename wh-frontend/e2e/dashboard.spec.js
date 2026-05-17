import { test, expect } from '@playwright/test'

test.describe('Dashboard 主页', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('用户名').fill('admin')
    await page.getByPlaceholder('密码').fill('Admin@123')
    await page.locator('button').filter({ hasText: '登录' }).click()
    await page.waitForURL('**/pm/charter')
  })

  test('导航到主页', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForTimeout(2000)
    await expect(page.getByRole('tab', { name: '主页' })).toBeVisible({ timeout: 5000 })
  })
})
