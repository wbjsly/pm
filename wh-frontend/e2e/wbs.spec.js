import { test, expect } from '@playwright/test'

test.describe('WBS分解流程', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('http://localhost:8090/login')
    await page.fill('input[type="text"]', 'admin')
    await page.fill('input[type="password"]', 'admin123')
    await page.click('button:has-text("登录")')
    await page.waitForURL('**/dashboard')
  })

  test('查看WBS列表', async ({ page }) => {
    await page.goto('http://localhost:8090/pm/wbs')
    await expect(page.locator('text=WBS').or(page.locator('text=任务'))).toBeVisible({ timeout: 5000 })
  })

  test('WBS详情页面加载', async ({ page }) => {
    await page.goto('http://localhost:8090/pm/wbs/detail')
    await expect(page.locator('text=WBS').or(page.locator('text=详情'))).toBeVisible({ timeout: 5000 })
  })
})
