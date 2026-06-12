import { generateKeyPairSync } from 'node:crypto'
import { writeFileSync } from 'node:fs'
import { join } from 'node:path'

const dir = process.argv[2]
if (!dir) {
  console.error('Usage: node generate-jwt-keys.node.mjs <secrets-dir>')
  process.exit(1)
}

const { publicKey, privateKey } = generateKeyPairSync('rsa', { modulusLength: 2048 })
writeFileSync(join(dir, 'jwt-private.pem'), privateKey.export({ type: 'pkcs8', format: 'pem' }), {
  mode: 0o600,
})
writeFileSync(join(dir, 'jwt-public.pem'), publicKey.export({ type: 'spki', format: 'pem' }), {
  mode: 0o644,
})
