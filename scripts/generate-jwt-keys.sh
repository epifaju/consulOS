#!/usr/bin/env bash
# Génère une paire de clés RSA-2048 pour JWT RS256 dans secrets/ (non versionné).
# Usage : ./scripts/generate-jwt-keys.sh
#         ./scripts/generate-jwt-keys.sh --force
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SECRETS_DIR="$ROOT/secrets"
PRIVATE_KEY="$SECRETS_DIR/jwt-private.pem"
PUBLIC_KEY="$SECRETS_DIR/jwt-public.pem"
FORCE=false

if [[ "${1:-}" == "--force" ]]; then
  FORCE=true
fi

if [[ -f "$PRIVATE_KEY" && -f "$PUBLIC_KEY" && "$FORCE" != "true" ]]; then
  echo "Clés JWT déjà présentes dans $SECRETS_DIR"
  echo "  (ajoutez --force pour regénérer — invalide les sessions en cours)"
  exit 0
fi

if [[ "$FORCE" == "true" && -f "$PRIVATE_KEY" ]]; then
  echo "Regénération des clés JWT..."
fi

mkdir -p "$SECRETS_DIR"

if command -v openssl >/dev/null 2>&1; then
  openssl genrsa -out "$PRIVATE_KEY" 2048
  openssl rsa -in "$PRIVATE_KEY" -pubout -out "$PUBLIC_KEY"
elif command -v node >/dev/null 2>&1; then
  node "$(dirname "$0")/generate-jwt-keys.node.mjs" "$SECRETS_DIR"
else
  echo "ERREUR : installez OpenSSL ou Node.js pour générer les clés JWT." >&2
  exit 1
fi

chmod 600 "$PRIVATE_KEY"
chmod 644 "$PUBLIC_KEY"

echo ""
echo "Clés RSA générées dans $SECRETS_DIR"
echo "  jwt-private.pem  (ne jamais committer — voir .gitignore)"
echo "  jwt-public.pem"
echo ""
echo "Variables d'environnement (optionnel, voir .env.example) :"
echo "  JWT_PRIVATE_KEY_PATH=$PRIVATE_KEY"
echo "  JWT_PUBLIC_KEY_PATH=$PUBLIC_KEY"
