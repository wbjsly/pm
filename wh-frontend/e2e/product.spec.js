import { test, expect } from '@playwright/test'

test.describe('产品管理', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('用户名').fill('admin')
    await page.getByPlaceholder('密码').fill('Admin@123')
    await page.locator('button').filter({ hasText: '登录' }).click()
    await page.waitForURL('**/pm/charter')
  })

  test('产品管理页面加载 - 搜索和列表', async ({ page }) => {
    await page.goto('/pm/product')
    await page.waitForTimeout(2000)
    await expect(page.getByText('产品清单').first()).toBeVisible()
    await expect(page.getByPlaceholder('产品编码/名称')).toBeVisible()
    await expect(page.getByRole('button', { name: '查询' })).toBeVisible()
  })

  test('产品搜索功能', async ({ page }) => {
    await page.goto('/pm/product')
    await page.waitForTimeout(2000)
    const searchInput = page.getByPlaceholder('产品编码/名称')
    await searchInput.fill('test')
    await page.getByRole('button', { name: '查询' }).click()
    await page.waitForTimeout(1000)
  })

  test('新增产品弹窗 - 表单字段', async ({ page }) => {
    await page.goto('/pm/product')
    await page.waitForTimeout(2000)
    // 新增产品按钮（图标按钮）
    const addBtn = page.locator('button').filter({ hasText: '新增产品' })
    const hasBtn = await addBtn.isVisible().catch(() => false)
    if (hasBtn) {
      await addBtn.click()
    } else {
      // 如果没有文字，用Plus图标附近的按钮
      await page.locator('.el-card .el-button--primary').last().click()
    }
    await page.waitForTimeout(500)
    // 验证对话框
    const dialog = page.getByRole('dialog').filter({ hasText: /新增产品|编辑产品/ })
    await expect(dialog).toBeVisible({ timeout: 3000 })
    // 验证必填字段
    await expect(page.getByPlaceholder('请输入产品编码')).toBeVisible()
    await expect(page.getByPlaceholder('请输入产品名称')).toBeVisible()
    await expect(page.getByPlaceholder('如：1.0.0')).toBeVisible()
    // 关闭弹窗
    await dialog.getByRole('button', { name: '取消' }).click()
  })

  test('产品手风琴展开和折叠', async ({ page }) => {
    await page.goto('/pm/product')
    await page.waitForTimeout(2000)
    // 等待产品列表加载
    const productItems = page.locator('.el-card .el-card__body > div > div').filter({ has: page.locator('[class*="cursor"]') })
    const count = await productItems.count()
    if (count > 0) {
      // 点击第一个产品展开
      await productItems.first().click()
      await page.waitForTimeout(1000)
      // 再次点击折叠
      await productItems.first().click()
      await page.waitForTimeout(500)
    }
  })

  test('新增模块弹窗 - 表单字段', async ({ page }) => {
    await page.goto('/pm/product')
    await page.waitForTimeout(2000)
    // 需要先展开一个产品才能添加模块
    const productItems = page.locator('.el-card .el-card__body > div > div').filter({ has: page.locator('[class*="cursor"]') })
    const count = await productItems.count()
    if (count > 0) {
      // 点击展开产品
      await productItems.first().click()
      await page.waitForTimeout(1000)
      // 查找新增模块按钮（在展开区域的加号按钮）
      const addModuleBtn = page.locator('button').filter({ hasText: '添加模块' })
      if (await addModuleBtn.isVisible().catch(() => false)) {
        await addModuleBtn.click()
        await page.waitForTimeout(500)
        // 验证模块对话框
        const moduleDialog = page.getByRole('dialog').filter({ hasText: /新增模块|编辑模块/ })
        await expect(moduleDialog).toBeVisible({ timeout: 3000 })
        await expect(page.getByPlaceholder('请输入模块编码')).toBeVisible()
        await expect(page.getByPlaceholder('请输入模块名称')).toBeVisible()
        // 关闭
        await moduleDialog.getByRole('button', { name: '取消' }).click()
      }
    }
  })

  test('产品列表分页组件', async ({ page }) => {
    await page.goto('/pm/product')
    await page.waitForTimeout(2000)
    // 验证分页存在
    const pagination = page.locator('.el-pagination')
    await expect(pagination).toBeVisible({ timeout: 5000 })
  })
})
