import { test, expect } from '@playwright/test'

test.describe('预算管理流程', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('http://localhost:8090/login')
    await page.fill('input[type="text"]', 'admin')
    await page.fill('input[type="password"]', 'admin123')
    await page.click('button:has-text("登录")')
    await page.waitForURL('**/dashboard')
  })

  test('查看预算列表', async ({ page }) => {
    await page.goto('http://localhost:8090/pm/budgets')
    await expect(page.locator('text=预算')).toBeVisible({ timeout: 5000 })
  })

  test('预算编制页面加载', async ({ page }) => {
    await page.goto('http://localhost:8090/pm/budgets/form')
    await expect(page.locator('text=预算编制').or(page.locator('text=预算'))).toBeVisible({ timeout: 5000 })
  })
})
