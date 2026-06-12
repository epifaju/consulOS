import { test, expect } from '@playwright/test'
import { CREDENTIALS, loginAs, logout } from './helpers/auth'
import { clearLoginRateLimit } from './helpers/rateLimit'

test.describe('Authentication', () => {
  test('login with valid credentials redirects after auth', async ({ page }) => {
    await loginAs(page, 'superviseur')
    await expect(page).not.toHaveURL(/\/login/)
    await expect(page.getByRole('heading', { name: /tableau de bord|dashboard|painel/i })).toBeVisible()
  })

  test('admin login redirects to administration', async ({ page }) => {
    await loginAs(page, 'admin')
    await expect(page).toHaveURL(/\/administration/)
  })

  test('login with wrong password shows error', async ({ page }) => {
    clearLoginRateLimit()
    await page.goto('/login')
    await page.getByRole('textbox', { name: /adresse e-mail|email/i }).fill(CREDENTIALS.agent.email)
    await page.getByLabel(/mot de passe|password/i).fill('WrongPassword1!')
    await page.getByRole('button', { name: /se connecter|connecter|login/i }).click()
    await expect(page.getByRole('alert')).toBeVisible({ timeout: 10_000 })
    await expect(page.getByText(/incorrect|invalid|erreur|error/i)).toBeVisible()
    await expect(page).toHaveURL(/\/login/)
  })

  test('access /usagers without auth redirects to login', async ({ page }) => {
    await page.goto('/usagers')
    await expect(page).toHaveURL(/\/login/)
  })

  test('logout clears session and redirects on protected access', async ({ page }) => {
    await loginAs(page, 'admin')
    await logout(page)
    await page.goto('/usagers')
    await expect(page).toHaveURL(/\/login/)
  })
})
