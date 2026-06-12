import { test, expect } from '@playwright/test'
import { loginAs, navigateViaSidebar } from './helpers/auth'

test.describe('Encaissement', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'caissier')
  })

  test('create encaissement shows REC number and PDF download', async ({ page, request }) => {
    await navigateViaSidebar(page, /^caisse$/i)

    let recRow = page.locator('tr').filter({ hasText: /REC-\d{4}-\d{5}/ }).first()
    if (!(await recRow.isVisible())) {
      await page.getByPlaceholder(/rechercher un usager|search citizen|min\. 2/i).fill('Aminata')
      await page.getByRole('button', { name: /Aminata Camara/i }).click()
      await page.getByRole('spinbutton').fill('50')
      await expect(
        page.getByRole('button', { name: /créer encaissement|create payment/i }),
      ).toBeEnabled()
      await page.getByRole('button', { name: /créer encaissement|create payment/i }).click()
      recRow = page.locator('tr').filter({ hasText: /REC-\d{4}-\d{5}/ }).first()
      await expect(recRow).toBeVisible({ timeout: 15_000 })
    }

    await recRow.getByRole('link').click()
    await page.waitForURL(/\/caisse\/recu\//)

    const downloadBtn = page.getByRole('button', { name: /télécharger|download|descarregar/i })
    await expect(downloadBtn).toBeEnabled({ timeout: 20_000 })

    const downloadPromise = page.waitForEvent('download')
    await downloadBtn.click()
    const download = await downloadPromise
    const path = await download.path()
    expect(path).toBeTruthy()

    if (path) {
      const fs = await import('node:fs')
      const buffer = fs.readFileSync(path)
      expect(buffer.subarray(0, 4).toString()).toBe('%PDF')
    }
  })
})
