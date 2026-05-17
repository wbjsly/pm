import { test, expect } from '@playwright/test'

test.describe('认证流程', () => {
  test('登录页面渲染完整', async ({ page }) => {
    await page.goto('/login')
    await expect(page.getByText('WH管理系统')).toBeVisible()
    await expect(page.getByPlaceholder('用户名')).toBeVisible()
    await expect(page.getByPlaceholder('密码')).toBeVisible()
    await expect(page.locator('button').filter({ hasText: '登录' })).toBeVisible()
  })

  test('成功登录并跳转到首页', async ({ page }) => {
    await page.goto('/login')
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
    await expect(page.getByText(/用户名.*密码.*错误/i)).toBeVisible({ timeout: 3000 })
  })

  test('空用户名显示校验提示', async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('密码').fill('Admin@123')
    await page.locator('button').filter({ hasText: '登录' }).click()
    await expect(page.getByText(/请输入用户名/i)).toBeVisible({ timeout: 3000 })
  })

  test('空密码显示校验提示', async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('用户名').fill('admin')
    await page.locator('button').filter({ hasText: '登录' }).click()
    await expect(page.getByText(/请输入密码/i)).toBeVisible({ timeout: 3000 })
  })

  test('未登录访问受保护页面跳转登录', async ({ page }) => {
    await page.goto('/pm/charter')
    await page.waitForURL('**/login*')
    await expect(page.getByPlaceholder('用户名')).toBeVisible()
  })

  test('登录后可以访问多个页面', async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('用户名').fill('admin')
    await page.getByPlaceholder('密码').fill('Admin@123')
    await page.locator('button').filter({ hasText: '登录' }).click()
    await page.waitForURL('**/pm/charter')

    await page.goto('/pm/budget')
    await expect(page.getByRole('tab', { name: '预算管理' })).toBeVisible({ timeout: 5000 })

    await page.goto('/pm/wbs')
    await expect(page.getByRole('tab', { name: '项目任务' })).toBeVisible({ timeout: 5000 })
  })
})
