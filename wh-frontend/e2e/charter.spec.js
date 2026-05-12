import { test, expect } from '@playwright/test'

test.describe('立项审批流程', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('http://localhost:8090/login')
    await page.fill('input[type="text"]', 'admin')
    await page.fill('input[type="password"]', 'admin123')
    await page.click('button:has-text("登录")')
    await page.waitForURL('**/dashboard')
  })

  test('创建立项并提交审批', async ({ page }) => {
    await page.goto('http://localhost:8090/pm/charters')
    await expect(page.locator('text=立项').or(page.locator('text=项目'))).toBeVisible({ timeout: 5000 })

    await page.click('button:has-text("新建")')
    await page.waitForTimeout(500)

    const nameInput = page.locator('input').first()
    await nameInput.fill('E2E测试项目')
    await page.click('button:has-text("提交")')
  })
})
