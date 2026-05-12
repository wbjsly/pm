import { test as setup, expect } from '@playwright/test'

setup('authenticate', async ({ page }) => {
  await page.goto('/login')
  await page.fill('input[type="text"]', 'admin')
  await page.getByPlaceholder('密码').fill('Admin@123')
  await page.locator('button').filter({ hasText: '登录' }).click()
  await page.waitForURL('**/pm/charter')
})
