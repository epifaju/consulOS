import { test, expect } from '@playwright/test'

test.describe('Mot de passe oublié', () => {
  test('forgot password page submits email', async ({ page }) => {
    await page.goto('/forgot-password')
    await expect(page.getByText(/mot de passe oublié|forgot password/i).first()).toBeVisible()
    await page.getByRole('textbox', { name: /adresse e-mail|email/i }).fill('admin@consulos.local')
    await page.getByRole('button', { name: /envoyer|send/i }).click()
    await expect(page.getByText(/e-mail de réinitialisation|reset email|e-mail de redefinição/i)).toBeVisible({
      timeout: 10_000,
    })
  })

  test('login page links to forgot password', async ({ page }) => {
    await page.goto('/login')
    await page.getByRole('link', { name: /mot de passe oublié|forgot password/i }).click()
    await expect(page).toHaveURL(/\/forgot-password/)
  })
})
