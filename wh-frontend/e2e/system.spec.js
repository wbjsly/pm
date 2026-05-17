import { test, expect } from '@playwright/test'

test.describe('系统管理 - 用户管理', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('用户名').fill('admin')
    await page.getByPlaceholder('密码').fill('Admin@123')
    await page.locator('button').filter({ hasText: '登录' }).click()
    await page.waitForURL('**/pm/charter')
  })

  test('导航到用户管理页面', async ({ page }) => {
    await page.goto('/system/user')
    await page.waitForTimeout(2000)
    await expect(page.getByRole('table')).toBeVisible({ timeout: 5000 })
  })

  test('用户编辑页面可访问', async ({ page }) => {
    await page.goto('/system/user')
    await page.waitForTimeout(2000)
    const firstRow = page.locator('table tbody tr').first()
    const hasRows = await firstRow.isVisible().catch(() => false)
    if (hasRows) {
      const editBtn = firstRow.locator('button').nth(1)
      if (await editBtn.isVisible()) {
        await editBtn.click()
        await page.waitForTimeout(1000)
      }
    }
  })
})

test.describe('系统管理 - 字典管理', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('用户名').fill('admin')
    await page.getByPlaceholder('密码').fill('Admin@123')
    await page.locator('button').filter({ hasText: '登录' }).click()
    await page.waitForURL('**/pm/charter')
  })

  test('导航到字典管理页面', async ({ page }) => {
    await page.goto('/system/dict')
    await page.waitForTimeout(2000)
    await expect(page.getByRole('table').first()).toBeVisible({ timeout: 5000 })
  })
})

test.describe('系统管理 - 工作日历', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('用户名').fill('admin')
    await page.getByPlaceholder('密码').fill('Admin@123')
    await page.locator('button').filter({ hasText: '登录' }).click()
    await page.waitForURL('**/pm/charter')
  })

  test('导航到工作日历页面', async ({ page }) => {
    await page.goto('/system/calendar')
    await page.waitForTimeout(2000)
  })
})

test.describe('系统管理 - 成本定额', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('用户名').fill('admin')
    await page.getByPlaceholder('密码').fill('Admin@123')
    await page.locator('button').filter({ hasText: '登录' }).click()
    await page.waitForURL('**/pm/charter')
  })

  test('导航到成本定额页面', async ({ page }) => {
    await page.goto('/system/cost-quota')
    await page.waitForTimeout(2000)
    await expect(page.getByRole('table').first()).toBeVisible({ timeout: 5000 })
  })
})
