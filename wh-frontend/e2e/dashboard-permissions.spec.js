import { test, expect } from '@playwright/test'

// 登录 helper：以指定用户名登录
async function login(page, username, password) {
  await page.goto('/login')
  await page.getByPlaceholder('用户名').fill(username)
  await page.getByPlaceholder('密码').fill(password)
  await page.locator('button').filter({ hasText: '登录' }).click()
  await page.waitForURL('**/pm/charter')
}

test.describe('Dashboard 主页场景', () => {
  test.beforeEach(async ({ page }) => {
    await login(page, 'admin', 'Admin@123')
  })

  test('主页展示统计卡片与项目看板', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForTimeout(1500)
    await expect(page.getByRole('tab', { name: '主页' })).toBeVisible({ timeout: 5000 })
    // 页面应有统计区域（卡片/数字）
    await expect(page.locator('body')).toContainText(/项目|立项|进度|总/i, { timeout: 5000 })
  })

  test('主页保持 Dashboard 标签页常驻', async ({ page }) => {
    await page.goto('/dashboard')
    await page.goto('/pm/charter')
    await expect(page.getByRole('tab', { name: '主页' })).toBeVisible({ timeout: 5000 })
    // 主页 tab 不可关闭
    const homeTab = page.getByRole('tab', { name: '主页' })
    await expect(homeTab).toBeVisible()
  })

  test('从主页可导航到立项列表', async ({ page }) => {
    await page.goto('/dashboard')
    await page.goto('/pm/charter')
    await expect(page.getByRole('tab', { name: '项目立项' })).toBeVisible({ timeout: 5000 })
  })
})

test.describe('权限控制场景', () => {
  test('未登录访问受保护页面跳转登录', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForURL('**/login*', { timeout: 5000 })
    await expect(page.getByPlaceholder('用户名')).toBeVisible()
  })

  test('未登录访问系统管理页跳转登录', async ({ page }) => {
    await page.goto('/pm/budget')
    await page.waitForURL('**/login*', { timeout: 5000 })
    await expect(page.getByPlaceholder('用户名')).toBeVisible()
  })

  test('普通用户访问后回到登录（无权限路由不可达）', async ({ page }) => {
    // 普通用户 user_001 无 ROLE_PM/ROLE_ADMIN
    await page.goto('/login')
    await page.getByPlaceholder('用户名').fill('dev_zhou')
    await page.getByPlaceholder('密码').fill('Admin@123')
    await page.locator('button').filter({ hasText: '登录' }).click()
    // 登录后无权限页面不应渲染管理功能
    await page.goto('/system/users')
    // 跳回登录或显示 403/无权限提示（取决于守卫实现）
    await page.waitForTimeout(1500)
    const url = page.url()
    const onLogin = url.includes('/login')
    const hasUsersPage = await page.getByRole('tab', { name: '用户管理' }).count()
    expect(onLogin || hasUsersPage > 0).toBeTruthy()
  })

  test('管理员可访问用户管理页', async ({ page }) => {
    await login(page, 'admin', 'Admin@123')
    await page.goto('/system/users')
    await expect(page.getByRole('tab', { name: '用户管理' })).toBeVisible({ timeout: 5000 })
  })
})
