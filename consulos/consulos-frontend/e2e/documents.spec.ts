import { test, expect } from '@playwright/test'
import { loginAs, navigateViaSidebar } from './helpers/auth'

/** Minimal valid-enough PDF for MinIO upload (mime application/pdf). */
function tinyPdfBuffer(label: string): Buffer {
  const body = `%PDF-1.4
1 0 obj<< /Type /Catalog /Pages 2 0 R >>endobj
2 0 obj<< /Type /Pages /Kids [3 0 R] /Count 1 >>endobj
3 0 obj<< /Type /Page /Parent 2 0 R /MediaBox [0 0 200 200] /Contents 4 0 R >>endobj
4 0 obj<< /Length 44 >>stream
BT /F1 12 Tf 40 100 Td (${label}) Tj ET
endstream
endobj
xref
0 5
trailer<< /Size 5 /Root 1 0 R >>
startxref
0
%%EOF
`
  return Buffer.from(body, 'utf8')
}

test.describe('Documents', () => {
  test('upload on dossier then appears in global Documents list', async ({ page }) => {
    const fileName = `e2e-doc-${Date.now()}.pdf`

    await loginAs(page, 'agent')
    await navigateViaSidebar(page, /^dossiers$/i)
    await page.locator('tr').filter({ hasText: 'DOS-2026-00001' }).getByRole('link').click()
    await page.waitForURL(/\/dossiers\/[a-f0-9-]+/)

    await page.getByRole('tab', { name: /^documents$/i }).click()
    await page.getByRole('button', { name: /ajouter un document|add document|adicionar documento/i }).click()

    await page.locator('input[type="file"]').setInputFiles({
      name: fileName,
      mimeType: 'application/pdf',
      buffer: tinyPdfBuffer(fileName),
    })

    const uploadResponse = page.waitForResponse(
      (res) =>
        res.url().includes('/documents/upload') &&
        res.request().method() === 'POST' &&
        res.status() < 500,
    )
    await page.getByRole('button', { name: /^envoyer$|^send$|^enviar$/i }).click()
    const response = await uploadResponse
    expect(response.ok()).toBeTruthy()

    await expect(page.getByText(fileName)).toBeVisible({ timeout: 15_000 })

    await navigateViaSidebar(page, /^documents$/i)
    await expect(page.getByRole('heading', { name: /^documents$/i })).toBeVisible()

    const search = page.getByPlaceholder(/rechercher|search|pesquisar/i)
    await search.fill(fileName)
    await expect(page.getByText(fileName)).toBeVisible({ timeout: 15_000 })
  })
})
