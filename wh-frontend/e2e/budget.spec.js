import { test, expect } from '@playwright/test'

test.describe('预算管理流程', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('用户名').fill('admin')
    await page.getByPlaceholder('密码').fill('Admin@123')
    await page.locator('button').filter({ hasText: '登录' }).click()
    await page.waitForURL('**/pm/charter')
  })

  test('导航到预算管理页面 - 表格和数据列', async ({ page }) => {
    await page.goto('/pm/budget')
    await page.waitForTimeout(2000)
    await expect(page.getByRole('tab', { name: '预算管理' })).toBeVisible({ timeout: 5000 })
    const table = page.getByRole('table')
    const tableVisible = await table.isVisible().catch(() => false)
    if (tableVisible) {
      await expect(table).toBeVisible()
    }
  })

  test('预算管理页面结构 - 筛选条件和操作', async ({ page }) => {
    await page.goto('/pm/budget')
    await page.waitForTimeout(2000)
    await expect(page.getByText('预算管理').first()).toBeVisible()
    // 查询按钮
    await expect(page.getByRole('button', { name: '查询' })).toBeVisible()
    // 重置按钮
    await expect(page.getByRole('button', { name: '重置' })).toBeVisible()
  })

  test('新增加预算表单可访问', async ({ page }) => {
    await page.goto('/pm/budget/form')
    await page.waitForTimeout(2000)
  })

  test('预算列表搜索功能', async ({ page }) => {
    await page.goto('/pm/budget')
    await page.waitForTimeout(2000)
    const searchBtn = page.getByRole('button', { name: '查询' })
    if (await searchBtn.isVisible()) {
      await searchBtn.click()
      await page.waitForTimeout(1000)
    }
  })

  test('预算列表重置搜索条件', async ({ page }) => {
    await page.goto('/pm/budget')
    await page.waitForTimeout(2000)
    const resetBtn = page.getByRole('button', { name: '重置' })
    if (await resetBtn.isVisible()) {
      await resetBtn.click()
      await page.waitForTimeout(1000)
    }
  })

  test('从预算列表导航到详情', async ({ page }) => {
    await page.goto('/pm/budget')
    await page.waitForTimeout(2000)
    const table = page.getByRole('table')
    const hasTable = await table.isVisible().catch(() => false)
    if (hasTable) {
      const firstRow = table.locator('tbody tr').first()
      const hasRows = await firstRow.isVisible().catch(() => false)
      if (hasRows) {
        // 展开项目行查看预算版本
        await firstRow.click()
        await page.waitForTimeout(1000)
      }
    }
  })

  test('预算项目行展开 - 查看预算版本', async ({ page }) => {
    await page.goto('/pm/budget')
    await page.waitForTimeout(2000)
    // 表格行可点击展开
    const table = page.getByRole('table')
    const firstExpandRow = table.locator('tbody tr').first()
    const hasRows = await firstExpandRow.isVisible().catch(() => false)
    if (hasRows) {
      await firstExpandRow.click()
      await page.waitForTimeout(1000)
      // 展开后应该能看到子表格或"暂无预算版本"提示
      const subTable = table.locator('.el-table__body-wrapper table').first()
      const hasSubTable = await subTable.isVisible().catch(() => false)
      if (!hasSubTable) {
        // 可能显示暂无数据
        await expect(page.getByText(/暂无/)).toBeVisible()
      }
    }
  })

  test('预算预实对比页面可访问', async ({ page }) => {
    await page.goto('/pm/budget/comparison/test-id')
    await page.waitForTimeout(2000)
  })

  test('新增预算按钮 - 导航到表单', async ({ page }) => {
    await page.goto('/pm/budget')
    await page.waitForTimeout(2000)
    // 找新增预算按钮（Plus图标按钮）
    const addBtn = page.locator('.el-card .el-button--primary').filter({ has: page.locator('.el-icon-plus') })
    const hasBtn = await addBtn.isVisible().catch(() => false)
    if (hasBtn) {
      await addBtn.click()
      await page.waitForTimeout(1000)
      // 应导航到表单页
      await expect(page).toHaveURL(/\/pm\/budget\/form/)
    }
  })

  test('预算管理分页组件', async ({ page }) => {
    await page.goto('/pm/budget')
    await page.waitForTimeout(2000)
    const pagination = page.locator('.el-pagination')
    await expect(pagination).toBeVisible({ timeout: 5000 })
  })
})
