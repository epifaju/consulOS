import { test, expect } from '@playwright/test'
import { loginAs, navigateViaSidebar } from './helpers/auth'

test.describe('Usager et dossier', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'agent')
  })

  test('create usager appears in list', async ({ page }) => {
    const uniqueNom = `E2E${Date.now()}`
    await navigateViaSidebar(page, /^usagers$/i)
    await page.getByRole('button', { name: /nouvel usager|new citizen|novo utente/i }).click()
    await page.getByLabel(/nom/i).first().fill(uniqueNom)
    await page.getByLabel(/prénom|prenom/i).fill('Test')
    await page.getByLabel(/nationalité|nationalite/i).fill('GNB')
    await page.getByLabel(/e-mail|email/i).fill(`e2e.${Date.now()}@test.local`)
    await page.getByLabel(/téléphone|telephone/i).fill('+33600000099')
    await page.getByRole('button', { name: /enregistrer|save/i }).click()
    await page.waitForURL(/\/usagers\//)

    await navigateViaSidebar(page, /^usagers$/i)
    await expect(page.getByText(uniqueNom)).toBeVisible({ timeout: 15_000 })
  })

  test('open seed dossier BROUILLON and transition to SOUMIS', async ({ page }) => {
    await navigateViaSidebar(page, /^dossiers$/i)
    await page.locator('tr').filter({ hasText: 'DOS-2026-00001' }).getByRole('link').click()
    await page.waitForURL(/\/dossiers\/[a-f0-9-]+/)

    await expect(page.getByText('DOS-2026-00001')).toBeVisible()

    const soumettre = page.getByRole('button', { name: /soumettre|submit/i })
    if (await soumettre.isVisible()) {
      await soumettre.click()
    }
    await expect(page.getByText(/soumis|SOUMIS/i).first()).toBeVisible({ timeout: 10_000 })
  })
})
