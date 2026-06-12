import type { Page } from '@playwright/test'
import { clearLoginRateLimit } from './rateLimit'

export const CREDENTIALS = {
  admin: { email: 'admin@consulos.local', password: 'Admin1234!' },
  agent: { email: 'agent@consulos.local', password: 'Admin1234!' },
  superviseur: { email: 'superviseur@consulos.local', password: 'Admin1234!' },
  caissier: { email: 'caissier@consulos.local', password: 'Admin1234!' },
} as const

export async function loginAs(
  page: Page,
  user: keyof typeof CREDENTIALS = 'admin',
): Promise<void> {
  const { email, password } = CREDENTIALS[user]
  clearLoginRateLimit()
  await page.goto('/login')
  await expectLoginPage(page)
  await page.getByRole('textbox', { name: /adresse e-mail|email address|endereço de e-mail/i }).fill(email)
  await page.getByLabel(/mot de passe|password|palavra-passe/i).fill(password)
  await page.getByRole('button', { name: /se connecter|connecter|login|sign in|entrar/i }).click()
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 20_000 })
}

export async function expectLoginPage(page: Page): Promise<void> {
  await page.getByText('ConsulOS', { exact: true }).waitFor({ timeout: 10_000 })
  await page.getByRole('button', { name: /se connecter|login|sign in|entrar/i }).waitFor()
}

/** Navigation SPA (évite un rechargement complet qui efface le token Zustand). */
export async function navigateViaSidebar(page: Page, label: RegExp): Promise<void> {
  await page.getByRole('link', { name: label }).click()
}

export async function logout(page: Page): Promise<void> {
  await page.locator('header button').last().click()
  await page.getByRole('menuitem', { name: /déconnexion|logout|sair/i }).click()
  await page.waitForURL(/\/login/, { timeout: 10_000 })
}
