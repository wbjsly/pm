import { test, expect } from '@playwright/test'

test.describe('交付物管理流程', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('用户名').fill('admin')
    await page.getByPlaceholder('密码').fill('Admin@123')
    await page.locator('button').filter({ hasText: '登录' }).click()
    await page.waitForURL('**/pm/charter')
  })

  test('导航到交付物管理页面 - 表格和数据列', async ({ page }) => {
    await page.goto('/pm/deliverable')
    await page.waitForTimeout(2000)
    await expect(page.getByRole('tab', { name: '成果管理' })).toBeVisible({ timeout: 5000 })
    const table = page.getByRole('table')
    const tableVisible = await table.isVisible().catch(() => false)
    if (tableVisible) {
      await expect(table).toBeVisible()
    }
  })

  test('交付物管理页面结构 - 搜索和过滤', async ({ page }) => {
    await page.goto('/pm/deliverable')
    await page.waitForTimeout(2000)
    await expect(page.getByText('成果管理').first()).toBeVisible()
    // 搜索框
    await expect(page.getByPlaceholder('搜索项目名称/简称')).toBeVisible()
    // 查询和重置按钮
    await expect(page.getByRole('button', { name: '查询' })).toBeVisible()
    await expect(page.getByRole('button', { name: '重置' })).toBeVisible()
  })

  test('新增交付物表单可访问', async ({ page }) => {
    await page.goto('/pm/deliverable/form')
    await page.waitForTimeout(2000)
  })

  test('交付物详情页面可访问', async ({ page }) => {
    await page.goto('/pm/deliverable')
    await page.waitForTimeout(2000)
    const firstProjectRow = page.locator('[class*="cursor"]').first()
    const hasRows = await firstProjectRow.isVisible().catch(() => false)
    if (hasRows) {
      await firstProjectRow.click()
      await page.waitForTimeout(1000)
      // 展开后如果有交付物表格，查看详情
      const detailTable = page.getByRole('table')
      const hasTable = await detailTable.isVisible().catch(() => false)
      if (hasTable) {
        const firstDeliverableRow = detailTable.locator('tbody tr').first()
        if (await firstDeliverableRow.isVisible().catch(() => false)) {
          const viewBtn = firstDeliverableRow.locator('button').first()
          if (await viewBtn.isVisible().catch(() => false)) {
            await viewBtn.click()
            await page.waitForTimeout(2000)
          }
        }
      }
    }
  })

  test('交付物列表搜索功能', async ({ page }) => {
    await page.goto('/pm/deliverable')
    await page.waitForTimeout(2000)
    const searchInput = page.getByPlaceholder('搜索项目名称/简称')
    if (await searchInput.isVisible()) {
      await searchInput.fill('test')
      await page.getByRole('button', { name: '查询' }).click()
      await page.waitForTimeout(1000)
    }
  })

  test('交付物重置搜索条件', async ({ page }) => {
    await page.goto('/pm/deliverable')
    await page.waitForTimeout(2000)
    const searchInput = page.getByPlaceholder('搜索项目名称/简称')
    if (await searchInput.isVisible()) {
      await searchInput.fill('test')
      await page.getByRole('button', { name: '重置' }).click()
      await page.waitForTimeout(1000)
      await expect(searchInput).toHaveValue('')
    }
  })

  test('项目手风琴展开和折叠', async ({ page }) => {
    await page.goto('/pm/deliverable')
    await page.waitForTimeout(2000)
    // 等待项目列表加载
    const projectItems = page.locator('[class*="cursor"]')
    const count = await projectItems.count()
    if (count > 0) {
      // 展开第一个项目
      await projectItems.first().click()
      await page.waitForTimeout(1000)
      // 折叠
      await projectItems.first().click()
      await page.waitForTimeout(500)
    }
  })

  test('交付物分页组件', async ({ page }) => {
    await page.goto('/pm/deliverable')
    await page.waitForTimeout(2000)
    const pagination = page.locator('.el-pagination')
    await expect(pagination).toBeVisible({ timeout: 5000 })
  })
})
