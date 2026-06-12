# Genere une paire de cles RSA-2048 pour JWT RS256 dans secrets/ (non versionne).
# Usage : .\scripts\generate-jwt-keys.ps1
#         .\scripts\generate-jwt-keys.ps1 -Force
param(
    [switch]$Force
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$secretsDir = Join-Path $Root "secrets"
$privateKey = Join-Path $secretsDir "jwt-private.pem"
$publicKey = Join-Path $secretsDir "jwt-public.pem"

$keysPresent = (Test-Path $privateKey) -and (Test-Path $publicKey)

if ($keysPresent -and -not $Force) {
    Write-Host "Cles JWT deja presentes dans $secretsDir" -ForegroundColor Yellow
    Write-Host "  Utilisez -Force pour regenerer (invalide les sessions en cours)."
    exit 0
}

if ($Force -and $keysPresent) {
    Write-Host "Regeneration des cles JWT..." -ForegroundColor Yellow
}

New-Item -ItemType Directory -Force -Path $secretsDir | Out-Null

$generated = $false
$openssl = Get-Command openssl -ErrorAction SilentlyContinue
if ($openssl) {
    & $openssl genrsa -out $privateKey 2048
    if ($LASTEXITCODE -eq 0) {
        & $openssl rsa -in $privateKey -pubout -out $publicKey
        if ($LASTEXITCODE -eq 0) {
            $generated = $true
            Write-Host "Generation via OpenSSL" -ForegroundColor DarkGray
        }
    }
}

if (-not $generated) {
    $node = Get-Command node -ErrorAction SilentlyContinue
    if ($node) {
        $nodeScript = Join-Path $PSScriptRoot "generate-jwt-keys.node.mjs"
        & $node $nodeScript $secretsDir
        if ($LASTEXITCODE -eq 0) {
            $generated = $true
            Write-Host "Generation via Node.js crypto" -ForegroundColor DarkGray
        }
    }
}

if (-not $generated) {
    Write-Host ""
    Write-Host "ERREUR : impossible de generer les cles JWT." -ForegroundColor Red
    Write-Host "Installez OpenSSL (Git for Windows) ou Node.js, puis relancez ce script."
    exit 1
}

try {
    icacls $privateKey /inheritance:r /grant:r "${env:USERNAME}:(R)" | Out-Null
}
catch {
    Write-Host "Note : permissions restrictives non appliquees sur la cle privee." -ForegroundColor DarkYellow
}

Write-Host ""
Write-Host "Cles RSA generees dans $secretsDir" -ForegroundColor Green
Write-Host "  jwt-private.pem  (ne jamais committer, voir .gitignore)"
Write-Host "  jwt-public.pem"
Write-Host ""
Write-Host "Variables d'environnement optionnelles (voir .env.example) :"
Write-Host "  JWT_PRIVATE_KEY_PATH=$privateKey"
Write-Host "  JWT_PUBLIC_KEY_PATH=$publicKey"
