import { test, expect } from '@playwright/test'
import { loginAs } from './helpers/auth'

test.describe('Dashboard', () => {
  test('superviseur sees dashboard with KPIs', async ({ page }) => {
    await loginAs(page, 'superviseur')
    await expect(page.getByRole('heading', { name: /tableau de bord|dashboard|painel/i })).toBeVisible({
      timeout: 15_000,
    })
    await expect(page.getByText(/dossiers ouverts|open cases|processos abertos/i)).toBeVisible()
    await expect(page.getByText(/rdv du jour|today's appointments|marcações hoje/i)).toBeVisible()
  })

  test('agent sees dashboard page with limited widgets', async ({ page }) => {
    await loginAs(page, 'agent')
    await expect(page.getByRole('heading', { name: /tableau de bord|dashboard|painel/i })).toBeVisible({
      timeout: 15_000,
    })
    await expect(page.getByText(/alertes actives|active alerts|alertas ativos/i)).toBeVisible()
  })
})
