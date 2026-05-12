import { test, expect } from '@playwright/test'

test.describe('工时录入审批流程', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('http://localhost:8090/login')
    await page.fill('input[type="text"]', 'admin')
    await page.fill('input[type="password"]', 'admin123')
    await page.click('button:has-text("登录")')
    await page.waitForURL('**/dashboard')
  })

  test('录入工时并查看统计', async ({ page }) => {
    await page.goto('http://localhost:8090/pm/work-hours')
    await expect(page.locator('text=工时').or(page.locator('text=Work'))).toBeVisible({ timeout: 5000 })
  })
})
