import { test, expect } from '@playwright/test'

test.describe('立项管理流程', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('用户名').fill('admin')
    await page.getByPlaceholder('密码').fill('Admin@123')
    await page.locator('button').filter({ hasText: '登录' }).click()
    await page.waitForURL('**/pm/charter')
  })

  test('查看立项列表', async ({ page }) => {
    await expect(page.getByRole('tab', { name: '项目立项' })).toBeVisible({ timeout: 5000 })
    await expect(page.getByRole('table').first()).toBeVisible({ timeout: 3000 })
  })

  test('打开新建立项表单', async ({ page }) => {
    await page.goto('/pm/charter/form')
    await page.waitForTimeout(2000)
    await expect(page.getByText('新增项目')).toBeVisible({ timeout: 5000 })
    await expect(page.getByPlaceholder('请输入项目名称')).toBeVisible()
  })
})
