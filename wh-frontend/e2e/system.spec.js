import { test, expect } from '@playwright/test'

// ==================== 系统管理 - 用户管理 ====================
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
    await expect(page.getByRole('table').first()).toBeVisible({ timeout: 5000 })
  })

  test('用户列表加载 - 表格和筛选条件', async ({ page }) => {
    await page.goto('/system/user')
    await page.waitForTimeout(2000)
    await expect(page.getByText('用户管理').first()).toBeVisible()
    await expect(page.getByPlaceholder('用户名/姓名')).toBeVisible()
    await expect(page.getByRole('button', { name: '查询' })).toBeVisible()
    await expect(page.getByRole('button', { name: '重置' })).toBeVisible()
  })

  test('用户搜索和重置', async ({ page }) => {
    await page.goto('/system/user')
    await page.waitForTimeout(2000)
    const searchInput = page.getByPlaceholder('用户名/姓名')
    await searchInput.fill('admin')
    await page.getByRole('button', { name: '查询' }).click()
    await page.waitForTimeout(1000)
    await page.getByRole('button', { name: '重置' }).click()
    await page.waitForTimeout(500)
    await expect(searchInput).toHaveValue('')
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

  test('重置密码弹窗', async ({ page }) => {
    await page.goto('/system/user')
    await page.waitForTimeout(2000)
    const firstRow = page.locator('table tbody tr').first()
    const hasRows = await firstRow.isVisible().catch(() => false)
    if (hasRows) {
      // 重置密码按钮通常在操作列的第三个位置
      const resetBtn = firstRow.locator('button').nth(2)
      if (await resetBtn.isVisible()) {
        await resetBtn.click()
        await expect(page.getByRole('dialog', { name: '重置密码' })).toBeVisible({ timeout: 3000 })
        await expect(page.getByPlaceholder('请输入新密码')).toBeVisible()
        // 取消关闭弹窗
        await page.getByRole('button', { name: '取消' }).click()
      }
    }
  })
})

// ==================== 系统管理 - 字典管理 ====================
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

  test('字典管理页面结构 - 左右表格和按钮', async ({ page }) => {
    await page.goto('/system/dict')
    await page.waitForTimeout(2000)
    // 左栏字典类型
    await expect(page.getByText('字典类型').first()).toBeVisible()
    await expect(page.getByRole('button', { name: '新增类型' })).toBeVisible()
    // 右栏字典条目（初始状态）
    await expect(page.getByText('请选择字典类型')).toBeVisible()
  })

  test('新增字典类型弹窗 - 表单字段', async ({ page }) => {
    await page.goto('/system/dict')
    await page.waitForTimeout(2000)
    // 打开新增类型弹窗
    await page.getByRole('button', { name: '新增类型' }).click()
    await expect(page.getByRole('dialog', { name: '新增字典类型' })).toBeVisible()
    // 验证表单字段
    await expect(page.getByPlaceholder('请输入类型编码')).toBeVisible()
    await expect(page.getByPlaceholder('请输入类型名称')).toBeVisible()
    await expect(page.getByPlaceholder('请输入描述')).toBeVisible()
    // 填写表单
    await page.getByPlaceholder('请输入类型编码').fill('E2E_TEST_TYPE')
    await page.getByPlaceholder('请输入类型名称').fill('E2E测试类型')
    // 取消关闭弹窗
    await page.getByRole('button', { name: '取消' }).click()
  })

  test('选择字典类型后新增条目按钮可用', async ({ page }) => {
    await page.goto('/system/dict')
    await page.waitForTimeout(2000)
    // 检查左侧类型表格是否有行
    const leftTable = page.getByRole('table').first()
    const typeRows = leftTable.locator('tbody tr')
    const rowCount = await typeRows.count()
    // 新增条目按钮初始应禁用
    const addItemBtn = page.getByRole('button', { name: '新增条目' })
    if (rowCount === 0) {
      await expect(addItemBtn).toBeDisabled()
    } else {
      // 点击第一行选择类型
      await typeRows.first().click()
      await page.waitForTimeout(500)
      await expect(addItemBtn).toBeEnabled()
    }
  })

  test('新增字典条目弹窗 - 表单字段', async ({ page }) => {
    await page.goto('/system/dict')
    await page.waitForTimeout(2000)
    // 先选择一个类型
    const firstTypeRow = page.getByRole('table').first().locator('tbody tr').first()
    const hasTypes = await firstTypeRow.isVisible().catch(() => false)
    if (hasTypes) {
      await firstTypeRow.click()
      await page.waitForTimeout(500)
      // 打开新增条目弹窗
      await page.getByRole('button', { name: '新增条目' }).click()
      await expect(page.getByRole('dialog', { name: '新增字典条目' })).toBeVisible()
      await expect(page.getByPlaceholder('请输入条目编码')).toBeVisible()
      await expect(page.getByPlaceholder('请输入条目名称')).toBeVisible()
      // 取消关闭
      await page.getByRole('button', { name: '取消' }).click()
    }
  })

  test('字典类型操作按钮 - 编辑和删除', async ({ page }) => {
    await page.goto('/system/dict')
    await page.waitForTimeout(2000)
    const firstRow = page.getByRole('table').first().locator('tbody tr').first()
    const hasRows = await firstRow.isVisible().catch(() => false)
    if (hasRows) {
      // 编辑按钮（link type）
      const editBtn = firstRow.locator('button').filter({ hasText: '编辑' })
      await expect(editBtn).toBeVisible()
      // 删除按钮（link type）
      const delBtn = firstRow.locator('button').filter({ hasText: '删除' })
      await expect(delBtn).toBeVisible()
    }
  })
})

// ==================== 系统管理 - 工作日历 ====================
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

// ==================== 系统管理 - 菜单管理 ====================
test.describe('系统管理 - 菜单管理', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('用户名').fill('admin')
    await page.getByPlaceholder('密码').fill('Admin@123')
    await page.locator('button').filter({ hasText: '登录' }).click()
    await page.waitForURL('**/pm/charter')
  })

  test('菜单管理页面加载 - 树形表格', async ({ page }) => {
    await page.goto('/system/menu')
    await page.waitForTimeout(2000)
    await expect(page.getByText('菜单管理').first()).toBeVisible()
    await expect(page.getByRole('table').first()).toBeVisible({ timeout: 5000 })
  })

  test('新增菜单弹窗 - 表单字段验证', async ({ page }) => {
    await page.goto('/system/menu')
    await page.waitForTimeout(2000)
    // 点击新增按钮（图标按钮，在卡片头部）
    const addBtn = page.locator('.el-card .el-button--primary').first()
    await expect(addBtn).toBeVisible()
    await addBtn.click()
    await page.waitForTimeout(500)
    // 验证对话框
    const dialog = page.getByRole('dialog').first()
    await expect(dialog).toBeVisible()
    // 验证表单字段
    await expect(page.getByPlaceholder('如：项目立项')).toBeVisible()
    await expect(page.getByPlaceholder('如：/pm/charter')).toBeVisible()
    // 取消关闭弹窗
    await dialog.getByRole('button', { name: '取消' }).click()
  })

  test('菜单表格行操作按钮可见', async ({ page }) => {
    await page.goto('/system/menu')
    await page.waitForTimeout(2000)
    const firstRow = page.locator('table tbody tr').first()
    const hasRows = await firstRow.isVisible().catch(() => false)
    if (hasRows) {
      // 每行至少有一个操作按钮（编辑/删除/新增子菜单）
      await expect(firstRow.locator('button').first()).toBeVisible()
    }
  })
})

// ==================== 系统管理 - 成本定额 ====================
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
    await expect(page.getByText('成本定额').first()).toBeVisible({ timeout: 5000 })
  })

  test('成本定额页面结构 - 年份选择和操作按钮', async ({ page }) => {
    await page.goto('/system/cost-quota')
    await page.waitForTimeout(2000)
    await expect(page.getByText('成本定额').first()).toBeVisible()
    // 年份选择器
    await expect(page.locator('.el-select').first()).toBeVisible()
    // 年份管理按钮
    await expect(page.locator('button').filter({ hasText: '年份' })).toBeVisible()
    // 岗位管理按钮
    await expect(page.locator('button').filter({ hasText: '岗位' })).toBeVisible()
  })

  test('年份管理弹窗 - 打开和关闭', async ({ page }) => {
    await page.goto('/system/cost-quota')
    await page.waitForTimeout(2000)
    // 打开年份管理
    await page.locator('button').filter({ hasText: '年份' }).click()
    await expect(page.getByRole('dialog', { name: '年份管理' })).toBeVisible({ timeout: 3000 })
    // 有创建年份按钮
    await expect(page.locator('button').filter({ hasText: '创建年份' })).toBeVisible()
    // 关闭弹窗（按 Escape 关闭）
    await page.keyboard.press('Escape')
    await page.waitForTimeout(500)
  })

  test('岗位管理弹窗 - 打开和验证', async ({ page }) => {
    await page.goto('/system/cost-quota')
    await page.waitForTimeout(2000)
    // 打开岗位管理
    await page.locator('button').filter({ hasText: '岗位' }).click()
    await expect(page.getByRole('dialog', { name: '岗位管理' })).toBeVisible({ timeout: 3000 })
    // 有新增加按钮（icon按钮）
    await expect(page.getByRole('dialog', { name: '岗位管理' }).locator('.el-button--primary').first()).toBeVisible()
    // 关闭弹窗（按 Escape 关闭）
    await page.keyboard.press('Escape')
    await page.waitForTimeout(500)
  })

  test('定额表格行操作 - 调价和历史版本按钮', async ({ page }) => {
    await page.goto('/system/cost-quota')
    await page.waitForTimeout(2000)
    const table = page.getByRole('table').first()
    const hasTable = await table.isVisible().catch(() => false)
    if (hasTable) {
      const firstRow = table.locator('tbody tr').first()
      const hasRows = await firstRow.isVisible().catch(() => false)
      if (hasRows) {
        // 操作列应该有调价和历史版本按钮
        const actionBtns = firstRow.locator('button')
        const count = await actionBtns.count()
        expect(count).toBeGreaterThanOrEqual(1)
      }
    }
  })
})
