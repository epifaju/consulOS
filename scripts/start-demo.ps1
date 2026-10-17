# Démarre l'environnement de démonstration ConsulOS (infra + instructions backend/frontend)
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot

& "$Root\scripts\start-dev-infra.ps1"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$backendDir = Join-Path $Root "consulos\consulos-backend"
$frontendDir = Join-Path $Root "consulos\consulos-frontend"
$javaHome = "C:\Program Files\Java\jdk-21.0.10"

Write-Host ""
Write-Host "==> Lancement du backend (nouvelle fenêtre)..." -ForegroundColor Cyan
$backendCmd = @"
`$env:JAVA_HOME = '$javaHome'
Set-Location '$backendDir'
mvn spring-boot:run -DskipTests
"@
Start-Process powershell -ArgumentList "-NoExit", "-Command", $backendCmd | Out-Null

Write-Host "==> Lancement du frontend (nouvelle fenêtre)..." -ForegroundColor Cyan
$frontendCmd = @"
Set-Location '$frontendDir'
npm run dev
"@
Start-Process powershell -ArgumentList "-NoExit", "-Command", $frontendCmd | Out-Null

Write-Host ""
Write-Host "Démo ConsulOS démarrée." -ForegroundColor Green
Write-Host "  Frontend : http://localhost:3010  (pas 3000/3001 : Grafana)"
Write-Host "  API      : http://localhost:8082/api/v1"
Write-Host ""
Write-Host "Comptes (mot de passe Admin1234!) :" -ForegroundColor Yellow
Write-Host "  admin@consulos.local       → Administration"
Write-Host "  superviseur@consulos.local → Dashboard"
Write-Host "  agent@consulos.local       → Opérations"
Write-Host "  caissier@consulos.local    → Caisse"
Write-Host ""
Write-Host "Guide complet : $Root\DEMO.md"
