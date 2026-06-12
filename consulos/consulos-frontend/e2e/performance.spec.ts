import { test, expect } from '@playwright/test'
import { loginAs, navigateViaSidebar } from './helpers/auth'
import { measureApiResponse, measureViewReady, PRD_API_RESPONSE_MS, PRD_VIEW_LOAD_MS } from './helpers/perf'

test.describe('Performance MVP (PRD < 2s)', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'superviseur')
  })

  test('dashboard API responds within target', async ({ page }) => {
    const elapsed = await measureApiResponse(page, '/dashboard/stats', async () => {
      await navigateViaSidebar(page, /^tableau de bord|dashboard|painel$/i)
    })
    expect(elapsed).toBeLessThan(PRD_API_RESPONSE_MS)
  })

  test('usagers list view ready within target', async ({ page }) => {
    const elapsed = await measureViewReady(
      page,
      () => navigateViaSidebar(page, /^usagers$/i),
      page.getByRole('heading', { name: /usagers|citizens|utentes/i }),
    )
    expect(elapsed).toBeLessThan(PRD_VIEW_LOAD_MS)
  })

  test('dossiers list view ready within target', async ({ page }) => {
    const elapsed = await measureViewReady(
      page,
      () => navigateViaSidebar(page, /^dossiers$/i),
      page.getByRole('heading', { name: /dossiers|cases|processos/i }),
    )
    expect(elapsed).toBeLessThan(PRD_VIEW_LOAD_MS)
  })

  test('caisse journal API responds within target', async ({ page }) => {
    await loginAs(page, 'caissier')
    const elapsed = await measureApiResponse(page, '/journal-caisse', async () => {
      await navigateViaSidebar(page, /^caisse$/i)
    })
    expect(elapsed).toBeLessThan(PRD_API_RESPONSE_MS)
  })
})
