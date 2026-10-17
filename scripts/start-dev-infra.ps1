# Démarre l'infrastructure locale ConsulOS (PostgreSQL, Redis, MinIO)
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot

Write-Host "==> Clés JWT (secrets/)..." -ForegroundColor Cyan
& "$Root\scripts\generate-jwt-keys.ps1"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "==> Vérification Docker Desktop..." -ForegroundColor Cyan
$dockerReady = $false
for ($i = 1; $i -le 24; $i++) {
    docker info 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) {
        $dockerReady = $true
        break
    }
    if ($i -eq 1) {
        $dockerExe = "${env:ProgramFiles}\Docker\Docker\Docker Desktop.exe"
        if (Test-Path $dockerExe) {
            Write-Host "    Lancement de Docker Desktop..." -ForegroundColor Yellow
            Start-Process $dockerExe | Out-Null
        }
    }
    Write-Host "    Attente du démon Docker ($i/24)..."
    Start-Sleep -Seconds 5
}

if (-not $dockerReady) {
    Write-Host ""
    Write-Host "ERREUR : Docker Desktop n'est pas démarré." -ForegroundColor Red
    Write-Host "1. Ouvrez Docker Desktop manuellement et attendez qu'il soit prêt."
    Write-Host "2. Relancez : .\scripts\start-dev-infra.ps1"
    Write-Host ""
    Write-Host "Le backend se connecte à PostgreSQL sur le port 5433 (conteneur Docker)."
    exit 1
}

Write-Host "==> Démarrage postgres, redis, minio..." -ForegroundColor Cyan
Push-Location $Root
docker compose up -d postgres redis minio
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$redisPort = docker compose port redis 6379 2>$null
if (-not $redisPort) {
    Write-Host "    Redis sans port hôte 6379 — recreation du conteneur..." -ForegroundColor Yellow
    docker compose up -d --force-recreate redis
    if ($LASTEXITCODE -ne 0) { Pop-Location; exit $LASTEXITCODE }
}

Write-Host "==> Attente PostgreSQL (port 5433)..." -ForegroundColor Cyan
$pgReady = $false
for ($i = 1; $i -le 30; $i++) {
    $tcp = Test-NetConnection -ComputerName 127.0.0.1 -Port 5433 -WarningAction SilentlyContinue
    if ($tcp.TcpTestSucceeded) {
        $pgReady = $true
        break
    }
    Start-Sleep -Seconds 2
}

Write-Host "==> Attente Redis (port 6379)..." -ForegroundColor Cyan
$redisReady = $false
for ($i = 1; $i -le 30; $i++) {
    $tcp = Test-NetConnection -ComputerName 127.0.0.1 -Port 6379 -WarningAction SilentlyContinue
    if ($tcp.TcpTestSucceeded) {
        $redisReady = $true
        break
    }
    Start-Sleep -Seconds 2
}
Pop-Location

if (-not $pgReady) {
    Write-Host "ERREUR : PostgreSQL inaccessible sur 127.0.0.1:5433" -ForegroundColor Red
    docker compose -f "$Root\docker-compose.yml" ps postgres
    exit 1
}

if (-not $redisReady) {
    Write-Host "ERREUR : Redis inaccessible sur 127.0.0.1:6379 (le login ConsulOS en a besoin)." -ForegroundColor Red
    docker compose -f "$Root\docker-compose.yml" ps redis
    exit 1
}

Write-Host ""
Write-Host "==> Sauvegarde MinIO (backups/minio/)..." -ForegroundColor Cyan
& "$Root\scripts\backup-minio.ps1"
if ($LASTEXITCODE -ne 0) {
    Write-Host "    Sauvegarde MinIO non bloquante (échec ignoré)." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Infrastructure prête." -ForegroundColor Green
Write-Host "  PostgreSQL : 127.0.0.1:5433 / consulos / consulos_dev_pass / consulos_dev"
Write-Host "  Redis      : localhost:6379  (obligatoire pour le login)"
Write-Host "  MinIO      : http://localhost:9000"
Write-Host "  Frontend   : http://localhost:3010  (Grafana = 3000/3001)"
Write-Host ""
Write-Host "Lancez la démo complète :" -ForegroundColor Cyan
Write-Host "  .\scripts\start-demo.ps1"
Write-Host ""
Write-Host "Ou manuellement le backend :" -ForegroundColor Cyan
Write-Host '  $env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.10"'
Write-Host "  cd consulos\consulos-backend"
Write-Host "  mvn spring-boot:run -DskipTests"
Write-Host ""
Write-Host "Comptes démo (Admin1234!) : admin@, superviseur@, agent@, caissier@consulos.local"
Write-Host "Guide : DEMO.md"
