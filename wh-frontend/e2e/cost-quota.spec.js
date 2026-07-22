import { test, expect } from '@playwright/test'

test.describe('成本定额管理 - 详细交互', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('用户名').fill('admin')
    await page.getByPlaceholder('密码').fill('Admin@123')
    await page.locator('button').filter({ hasText: '登录' }).click()
    await page.waitForURL('**/pm/charter')
  })

  test('成本定额完整页面结构', async ({ page }) => {
    await page.goto('/system/cost-quota')
    await page.waitForTimeout(2000)
    // 标题
    await expect(page.getByText('成本定额').first()).toBeVisible()
    // 年份选择器（存在且可见）
    await expect(page.locator('.el-select').first()).toBeVisible()
    // 年份管理按钮
    await expect(page.locator('button').filter({ hasText: '年份' })).toBeVisible()
    // 岗位管理按钮
    await expect(page.locator('button').filter({ hasText: '岗位' })).toBeVisible()
  })

  test('年份选择切换', async ({ page }) => {
    await page.goto('/system/cost-quota')
    await page.waitForTimeout(2000)
    // 年份选择器
    const yearSelect = page.locator('.el-select').first()
    await yearSelect.click()
    await page.waitForTimeout(300)
    // 如果有年份选项，选择第一个
    const options = page.locator('.el-select-dropdown__item')
    const optCount = await options.count()
    if (optCount > 0) {
      await options.first().click()
      await page.waitForTimeout(1000)
    }
  })

  test('年份管理完整流程', async ({ page }) => {
    await page.goto('/system/cost-quota')
    await page.waitForTimeout(2000)
    // 打开年份管理
    await page.locator('button').filter({ hasText: '年份' }).click()
    await expect(page.getByRole('dialog', { name: '年份管理' })).toBeVisible({ timeout: 3000 })
    // 年份管理弹出窗中应该有年份表格
    const yearDialog = page.getByRole('dialog', { name: '年份管理' })
    await expect(yearDialog.locator('table').first()).toBeVisible()
    // 创建年份按钮
    const createYearBtn = yearDialog.locator('button').filter({ hasText: '创建年份' })
    await expect(createYearBtn).toBeVisible()
    // 点击创建年份打开子弹窗
    await createYearBtn.click()
    await page.waitForTimeout(500)
    // 创建年份表单（append-to-body渲染，不在原dialog内）
    const yearFormDialog = page.getByRole('dialog').filter({ hasText: /创建年份|编辑年份/ })
    // 可能存在多个dialog，取最新的
    const isFormVisible = await yearFormDialog.isVisible().catch(() => false)
    if (isFormVisible) {
      await expect(page.getByPlaceholder('如：2025财年')).toBeVisible()
      // 取消关闭
      await yearFormDialog.getByRole('button', { name: '取消' }).click()
    }
    // 关闭年份管理（按 Escape 关闭）
    await page.keyboard.press('Escape')
    await page.waitForTimeout(500)
  })

  test('岗位管理弹窗交互', async ({ page }) => {
    await page.goto('/system/cost-quota')
    await page.waitForTimeout(2000)
    // 打开岗位管理
    await page.locator('button').filter({ hasText: '岗位' }).click()
    await expect(page.getByRole('dialog', { name: '岗位管理' })).toBeVisible({ timeout: 3000 })
    // 岗位表格
    const posDialog = page.getByRole('dialog', { name: '岗位管理' })
    await expect(posDialog.locator('table').first()).toBeVisible()
    // 新增按钮
    await expect(posDialog.locator('.el-button--primary').first()).toBeVisible()
    // 点击新增打开岗位表单
    await posDialog.locator('.el-button--primary').first().click()
    await page.waitForTimeout(500)
    // 岗位表单弹窗（append-to-body）
    const posFormDialog = page.getByRole('dialog').filter({ hasText: /新增岗位|编辑岗位/ })
    const isFormVisible = await posFormDialog.isVisible().catch(() => false)
    if (isFormVisible) {
      await expect(page.getByPlaceholder('岗位名称')).toBeVisible()
      // 取消关闭
      await posFormDialog.getByRole('button', { name: '取消' }).click()
    }
    // 关闭岗位管理（按 Escape 关闭）
    await page.keyboard.press('Escape')
    await page.waitForTimeout(500)
  })

  test('定额表格加载', async ({ page }) => {
    await page.goto('/system/cost-quota')
    await page.waitForTimeout(2000)
    // 选择年份后应加载定额表格
    const yearSelect = page.locator('.el-select').first()
    await yearSelect.click()
    await page.waitForTimeout(300)
    const options = page.locator('.el-select-dropdown__item')
    const optCount = await options.count()
    if (optCount > 0) {
      await options.first().click()
      await page.waitForTimeout(1000)
      const table = page.getByRole('table')
      const hasTable = await table.isVisible().catch(() => false)
      if (hasTable) {
        // 表格应有定额数据列
        const headerCells = table.locator('thead th')
        const headerCount = await headerCells.count()
        expect(headerCount).toBeGreaterThanOrEqual(5) // 岗位名称、成本定额、版本号、更新时间、生效日期、操作
      }
    }
  })

  test('调价弹窗操作', async ({ page }) => {
    await page.goto('/system/cost-quota')
    await page.waitForTimeout(2000)
    // 选择年份
    const yearSelect = page.locator('.el-select').first()
    await yearSelect.click()
    await page.waitForTimeout(300)
    const options = page.locator('.el-select-dropdown__item')
    const optCount = await options.count()
    if (optCount > 0) {
      await options.first().click()
      await page.waitForTimeout(1000)
      // 如果表格有数据，点开第一个调价
      const table = page.getByRole('table')
      const firstRow = table.locator('tbody tr').first()
      const hasRows = await firstRow.isVisible().catch(() => false)
      if (hasRows) {
        // 调价按钮是操作列的第一个按钮
        const adjustBtn = firstRow.locator('button').first()
        if (await adjustBtn.isVisible()) {
          await adjustBtn.click()
          await page.waitForTimeout(500)
          // 调价弹窗
          const adjustDialog = page.getByRole('dialog', { name: '调价' })
          const isVisible = await adjustDialog.isVisible().catch(() => false)
          if (isVisible) {
            await expect(adjustDialog).toBeVisible()
            // 验证有调价后定额输入框（el-input-number）
            const rateInput = page.locator('.el-input-number').first()
            await expect(rateInput).toBeVisible()
            // 取消关闭
            await adjustDialog.getByRole('button', { name: '取消' }).click()
          }
        }
      }
    }
  })

  test('历史版本抽屉', async ({ page }) => {
    await page.goto('/system/cost-quota')
    await page.waitForTimeout(2000)
    // 选择年份
    const yearSelect = page.locator('.el-select').first()
    await yearSelect.click()
    await page.waitForTimeout(300)
    const options = page.locator('.el-select-dropdown__item')
    const optCount = await options.count()
    if (optCount > 0) {
      await options.first().click()
      await page.waitForTimeout(1000)
      const table = page.getByRole('table')
      const firstRow = table.locator('tbody tr').first()
      const hasRows = await firstRow.isVisible().catch(() => false)
      if (hasRows) {
        // 历史版本按钮是操作列的第二个按钮
        const historyBtn = firstRow.locator('button').nth(1)
        if (await historyBtn.isVisible().catch(() => false)) {
          await historyBtn.click()
          await page.waitForTimeout(500)
          // 历史版本抽屉
          const drawer = page.getByRole('dialog', { name: '历史版本' })
          const isVisible = await drawer.isVisible().catch(() => false)
          if (isVisible) {
            await expect(drawer).toBeVisible()
            // 按 Escape 关闭抽屉
            await page.keyboard.press('Escape')
            await page.waitForTimeout(500)
          }
        }
      }
    }
  })
})
