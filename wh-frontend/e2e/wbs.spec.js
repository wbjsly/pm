import { test, expect } from '@playwright/test'

test.describe('WBS任务管理流程', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('用户名').fill('admin')
    await page.getByPlaceholder('密码').fill('Admin@123')
    await page.locator('button').filter({ hasText: '登录' }).click()
    await page.waitForURL('**/pm/charter')
  })

  test('导航到WBS任务页面', async ({ page }) => {
    await page.goto('/pm/wbs')
    await page.waitForTimeout(2000)
    await expect(page.getByRole('tab', { name: '项目任务' })).toBeVisible({ timeout: 5000 })
  })
})
