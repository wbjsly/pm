import { test, expect } from '@playwright/test'

test.describe('认证流程', () => {
  test('成功登录并跳转到首页', async ({ page }) => {
    await page.goto('/login')
    await expect(page.getByText('WH管理系统')).toBeVisible()
    await page.getByPlaceholder('用户名').fill('admin')
    await page.getByPlaceholder('密码').fill('Admin@123')
    await page.locator('button').filter({ hasText: '登录' }).click()
    await page.waitForURL('**/pm/charter')
    await expect(page.getByRole('tab', { name: '项目立项' })).toBeVisible({ timeout: 5000 })
  })

  test('错误密码显示错误消息', async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('用户名').fill('admin')
    await page.getByPlaceholder('密码').fill('wrong_password')
    await page.locator('button').filter({ hasText: '登录' }).click()
    await expect(page.getByText('用户名或密码错误')).toBeVisible({ timeout: 3000 })
  })

  test('登录页面可正常访问', async ({ page }) => {
    await page.goto('/login')
    await expect(page.getByPlaceholder('用户名')).toBeVisible()
    await expect(page.getByPlaceholder('密码')).toBeVisible()
    await expect(page.locator('button').filter({ hasText: '登录' })).toBeVisible()
  })
})
