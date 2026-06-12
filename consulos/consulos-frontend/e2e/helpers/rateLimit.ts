import { execSync } from 'node:child_process'

const REDIS_PASSWORD = process.env.REDIS_PASSWORD ?? 'redis_dev_pass'

/** Supprime les compteurs rate_limit:login:* (5 tentatives / 15 min). */
export function clearLoginRateLimit(): void {
  const listCommands = [
    `docker exec econsulos-redis-1 redis-cli -a ${REDIS_PASSWORD} --no-auth-warning KEYS "rate_limit:login:*"`,
    `redis-cli -p 6379 -a ${REDIS_PASSWORD} --no-auth-warning KEYS "rate_limit:login:*"`,
  ]

  for (const listCmd of listCommands) {
    try {
      const keys = execSync(listCmd, { encoding: 'utf8', timeout: 10_000 })
        .split(/\r?\n/)
        .map((k) => k.trim())
        .filter((k) => k && !k.startsWith('Warning'))
      if (keys.length === 0) return

      const delPrefix = listCmd.includes('docker exec')
        ? `docker exec econsulos-redis-1 redis-cli -a ${REDIS_PASSWORD} --no-auth-warning DEL`
        : `redis-cli -p 6379 -a ${REDIS_PASSWORD} --no-auth-warning DEL`
      execSync(`${delPrefix} ${keys.join(' ')}`, { timeout: 10_000, stdio: 'ignore' })
      return
    } catch {
      // essayer la commande suivante
    }
  }
}
