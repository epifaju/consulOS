# Certificat auto-signé pour le pilote (consulos.local). Remplacer par Let's Encrypt / PKI métier en prod réelle.
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$certDir = Join-Path $Root "nginx\certs"
New-Item -ItemType Directory -Force -Path $certDir | Out-Null
$key = Join-Path $certDir "privkey.pem"
$crt = Join-Path $certDir "fullchain.pem"

$openssl = Get-Command openssl -ErrorAction SilentlyContinue
if (-not $openssl) {
    Write-Host "openssl introuvable. Installez OpenSSL ou mkcert, puis générez nginx/certs/{fullchain,privkey}.pem" -ForegroundColor Red
    exit 1
}

& openssl req -x509 -nodes -days 825 -newkey rsa:2048 `
    -keyout $key -out $crt `
    -subj "/CN=consulos.local" `
    -addext "subjectAltName=DNS:consulos.local,DNS:localhost,IP:127.0.0.1"
if ($LASTEXITCODE -ne 0) {
    Write-Host "Échec openssl." -ForegroundColor Red
    exit 1
}
Write-Host "TLS pilote : $crt + $key" -ForegroundColor Green
Write-Host "Ajoutez consulos.local dans le fichier hosts, puis https://consulos.local" -ForegroundColor Cyan
exit 0
