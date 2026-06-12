import { clearLoginRateLimit } from './helpers/rateLimit'

export default async function globalSetup(): Promise<void> {
  clearLoginRateLimit()
}
