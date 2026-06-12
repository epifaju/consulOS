import { test, expect } from '@playwright/test'
import { loginAs, navigateViaSidebar } from './helpers/auth'

test.describe('Exports MVP', () => {
  test('export RGPD from usager detail returns JSON payload', async ({ page }) => {
    await loginAs(page, 'agent')
    await navigateViaSidebar(page, /^usagers$/i)

    await page.locator('tr').filter({ hasText: 'Aminata' }).filter({ hasText: 'Camara' }).getByRole('link').click()
    await page.waitForURL(/\/usagers\/[a-f0-9-]+/)

    const exportResponse = page.waitForResponse(
      (res) => res.url().includes('/export-rgpd') && res.request().method() === 'GET',
    )
    await page.getByRole('button', { name: /export(er)?\s*(rgpd|gdpr)|exportar dados rgpd/i }).click()
    const response = await exportResponse

    expect(response.status()).toBe(200)
    expect(response.headers()['content-type'] ?? '').toMatch(/json/i)

    const json = JSON.parse(await response.text()) as {
      exportVersion?: string
      usager?: { nom?: string; prenom?: string }
    }
    expect(json.exportVersion).toBeTruthy()
    expect(json.usager?.nom).toBe('Camara')
    expect(json.usager?.prenom).toBe('Aminata')
  })

  test('export Excel journal caisse returns xlsx file', async ({ page }) => {
    await loginAs(page, 'caissier')
    await navigateViaSidebar(page, /^caisse$/i)

    await expect(page.getByText(/journal de caisse|cash journal|diário de caixa/i)).toBeVisible({
      timeout: 15_000,
    })

    const exportResponse = page.waitForResponse(
      (res) => res.url().includes('/export/excel') && res.request().method() === 'GET',
    )
    await page.getByRole('button', { name: /export(er)?\s*excel|exportar excel/i }).click()
    const response = await exportResponse

    expect(response.status()).toBe(200)
    expect(response.headers()['content-type'] ?? '').toMatch(/spreadsheetml|excel|octet-stream/i)

    const body = await response.body()
    expect(body.subarray(0, 2).toString()).toBe('PK')
    expect(body.length).toBeGreaterThan(100)
  })
})
