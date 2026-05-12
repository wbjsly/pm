import { test, expect } from '@playwright/test'

test.describe('认证流程', () => {
  test('成功登录进入仪表盘', async ({ page }) => {
    await page.goto('http://localhost:8090/login')
    await expect(page.locator('text=登录')).toBeVisible()
    await page.fill('input[type="text"]', 'admin')
    await page.fill('input[type="password"]', 'admin123')
    await page.click('button:has-text("登录")')
    await page.waitForURL('**/dashboard')
    await expect(page.locator('text=仪表盘').or(page.locator('text=Dashboard'))).toBeVisible({ timeout: 5000 })
  })

  test('错误密码显示错误消息', async ({ page }) => {
    await page.goto('http://localhost:8090/login')
    await page.fill('input[type="text"]', 'admin')
    await page.fill('input[type="password"]', 'wrong_password')
    await page.click('button:has-text("登录")')
    await expect(page.locator('.el-message--error').or(page.locator('text=错误'))).toBeVisible({ timeout: 3000 })
  })

  test('未登录访问受保护页面跳转到登录', async ({ page }) => {
    await page.goto('http://localhost:8090/pm/charters')
    await page.waitForURL('**/login')
  })
})
