import { test, expect } from '@playwright/test'

test.describe('交付物管理流程', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('http://localhost:8090/login')
    await page.fill('input[type="text"]', 'admin')
    await page.fill('input[type="password"]', 'admin123')
    await page.click('button:has-text("登录")')
    await page.waitForURL('**/dashboard')
  })

  test('查看交付物列表', async ({ page }) => {
    await page.goto('http://localhost:8090/pm/deliverables')
    await expect(page.locator('text=交付物').or(page.locator('text=成果'))).toBeVisible({ timeout: 5000 })
  })

  test('交付物详情页面加载', async ({ page }) => {
    await page.goto('http://localhost:8090/pm/deliverables/detail')
    await expect(page.locator('text=交付物').or(page.locator('text=详情'))).toBeVisible({ timeout: 5000 })
  })
})
