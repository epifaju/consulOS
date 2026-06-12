import { defineConfig, devices } from '@playwright/test'

/** Port dédié E2E — évite le conflit avec PostgREST / autres apps sur 3000 */
const E2E_PORT = process.env.PLAYWRIGHT_PORT ?? '3010'
const baseURL = `http://127.0.0.1:${E2E_PORT}`

export default defineConfig({
  globalSetup: './e2e/global-setup.ts',
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  reporter: 'list',
  timeout: 60_000,
  use: {
    baseURL,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: process.env.CI
    ? undefined
    : {
        command: `npm run dev:e2e`,
        url: baseURL,
        reuseExistingServer: false,
        timeout: 120_000,
      },
})
