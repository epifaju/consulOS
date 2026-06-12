import { expect, type Page } from '@playwright/test'

/** PRD : temps de chargement cible < 2 s sur les vues courantes (marge CI). */
export const PRD_VIEW_LOAD_MS = 3_000
export const PRD_API_RESPONSE_MS = 2_500

export async function measureApiResponse(
  page: Page,
  urlPart: string,
  action: () => Promise<void>,
): Promise<number> {
  const started = Date.now()
  const responsePromise = page.waitForResponse(
    (res) => res.url().includes(urlPart) && res.status() === 200,
  )
  await action()
  const response = await responsePromise
  expect(response.ok()).toBeTruthy()
  return Date.now() - started
}

export async function measureViewReady(
  page: Page,
  action: () => Promise<void>,
  readyLocator: ReturnType<Page['getByRole']>,
): Promise<number> {
  const started = Date.now()
  await action()
  await readyLocator.waitFor({ state: 'visible', timeout: PRD_VIEW_LOAD_MS })
  return Date.now() - started
}
