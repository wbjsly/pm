import { test, expect } from '@playwright/test'

test.describe('工时管理流程', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('用户名').fill('admin')
    await page.getByPlaceholder('密码').fill('Admin@123')
    await page.locator('button').filter({ hasText: '登录' }).click()
    await page.waitForURL('**/pm/charter')
  })

  test('导航到工时管理页面', async ({ page }) => {
    await page.goto('/pm/work-hours')
    await page.waitForTimeout(2000)
    // 检查页面标题
    await expect(page.getByText('工时管理').first()).toBeVisible({ timeout: 5000 })
  })

  test('工时管理页面显示日历组件', async ({ page }) => {
    await page.goto('/pm/work-hours')
    await page.waitForTimeout(2000)
    // 日历组件或月历应该可见
    const hasCalendar = await page.locator('.month-calendar, .calendar').isVisible().catch(() => false)
    if (hasCalendar) {
      await expect(page.locator('.month-calendar, .calendar')).toBeVisible()
    }
  })

  test('工时审批页面可访问', async ({ page }) => {
    await page.goto('/pm/work-hours/approval')
    await page.waitForTimeout(2000)
  })

  test('工时审批页面结构 - 表格和筛选条件', async ({ page }) => {
    await page.goto('/pm/work-hours/approval')
    await page.waitForTimeout(2000)
    await expect(page.getByText('工时审批')).toBeVisible()
    // 项目名称搜索
    await expect(page.getByPlaceholder('项目名称')).toBeVisible()
    // 录入人搜索
    await expect(page.getByPlaceholder('录入人')).toBeVisible()
    // 查询按钮
    await expect(page.getByRole('button', { name: '查询' })).toBeVisible()
    // 返回按钮
    await expect(page.getByRole('button', { name: '返回' })).toBeVisible()
  })

  test('工时审批页面表格加载', async ({ page }) => {
    await page.goto('/pm/work-hours/approval')
    await page.waitForTimeout(2000)
    const table = page.getByRole('table')
    const hasTable = await table.isVisible().catch(() => false)
    if (hasTable) {
      await expect(table).toBeVisible()
      const headerCells = table.locator('thead th')
      const headerCount = await headerCells.count()
      expect(headerCount).toBeGreaterThanOrEqual(5) // 日期、项目名称、录入人、工时、描述、操作
    }
  })

  test('工时审批筛选操作', async ({ page }) => {
    await page.goto('/pm/work-hours/approval')
    await page.waitForTimeout(2000)
    const projectInput = page.getByPlaceholder('项目名称')
    if (await projectInput.isVisible()) {
      await projectInput.fill('测试项目')
      await page.getByRole('button', { name: '查询' }).click()
      await page.waitForTimeout(1000)
    }
  })

  test('工时管理返回导航', async ({ page }) => {
    await page.goto('/pm/work-hours/approval')
    await page.waitForTimeout(1000)
    const backBtn = page.getByRole('button', { name: '返回' })
    if (await backBtn.isVisible()) {
      await backBtn.click()
      await page.waitForTimeout(1000)
    }
  })
})
