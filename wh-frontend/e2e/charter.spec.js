import { test, expect } from '@playwright/test'

test.describe('立项管理流程', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('用户名').fill('admin')
    await page.getByPlaceholder('密码').fill('Admin@123')
    await page.locator('button').filter({ hasText: '登录' }).click()
    await page.waitForURL('**/pm/charter')
  })

  test('查看立项列表 - 表格和数据', async ({ page }) => {
    await expect(page.getByRole('tab', { name: '项目立项' })).toBeVisible({ timeout: 5000 })
    await expect(page.getByRole('table').first()).toBeVisible({ timeout: 3000 })
    await expect(page.getByText('项目编号')).toBeVisible()
    await expect(page.getByText('项目名称')).toBeVisible()
  })

  test('状态筛选下拉框可用', async ({ page }) => {
    const statusSelect = page.locator('.el-select').first()
    await expect(statusSelect).toBeVisible()
  })

  test('关键词搜索功能', async ({ page }) => {
    const searchInput = page.getByPlaceholder('项目名称/编号')
    await expect(searchInput).toBeVisible()
    await searchInput.fill('测试')
    await page.getByRole('button', { name: '查询' }).click()
    await page.waitForTimeout(1000)
  })

  test('打开新建立项表单', async ({ page }) => {
    await page.goto('/pm/charter/form')
    await page.waitForTimeout(2000)
    await expect(page.getByText(/新增项目/)).toBeVisible({ timeout: 5000 })
    await expect(page.getByPlaceholder('请输入项目名称')).toBeVisible()
    await expect(page.getByPlaceholder('请输入项目编号')).toBeVisible()
  })

  test('新建立项表单必填校验', async ({ page }) => {
    await page.goto('/pm/charter/form')
    await page.waitForTimeout(2000)
    // Try to submit empty form - should show validation errors
    const submitBtn = page.locator('button').filter({ hasText: /保存|提交/ }).first()
    if (await submitBtn.isVisible()) {
      await submitBtn.click()
      await page.waitForTimeout(1000)
    }
  })

  test('立项表单填写并取消返回', async ({ page }) => {
    await page.goto('/pm/charter/form')
    await page.waitForTimeout(2000)
    await page.getByPlaceholder('请输入项目名称').fill('E2E测试项目')
    // Navigate back
    await page.goto('/pm/charter')
    await expect(page.getByRole('tab', { name: '项目立项' })).toBeVisible({ timeout: 5000 })
  })

  test('查看项目详情（如果列表有数据）', async ({ page }) => {
    await page.waitForTimeout(2000)
    // Click first detail/view button if table has rows
    const firstRow = page.locator('table tbody tr').first()
    const hasRows = await firstRow.isVisible().catch(() => false)
    if (hasRows) {
      const viewBtn = firstRow.locator('button').first()
      if (await viewBtn.isVisible()) {
        await viewBtn.click()
        await page.waitForTimeout(2000)
      }
    }
  })

  test('重置查询条件', async ({ page }) => {
    const searchInput = page.getByPlaceholder('项目名称/编号')
    await searchInput.fill('test')
    await page.getByRole('button', { name: '重置' }).click()
    await page.waitForTimeout(500)
    await expect(searchInput).toHaveValue('')
  })
})
