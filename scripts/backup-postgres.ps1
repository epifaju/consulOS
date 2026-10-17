# pg_dump vers backups/postgres/<timestamp>.sql
param(
    [string]$ComposeFile = "docker-compose.yml",
    [string]$Service = "postgres",
    [string]$User = "consulos",
    [string]$Database = "consulos_dev"
)
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$destDir = Join-Path $Root "backups\postgres"
New-Item -ItemType Directory -Force -Path $destDir | Out-Null
$dest = Join-Path $destDir "$stamp.sql"

$composePath = Join-Path $Root $ComposeFile
$cid = (docker compose -f $composePath ps -q $Service 2>$null | Select-Object -First 1)
if (-not $cid) {
    Write-Host "Postgres n'est pas démarré ($ComposeFile)." -ForegroundColor Red
    exit 1
}

Write-Host "==> pg_dump $Database → $dest" -ForegroundColor Cyan
docker compose -f $composePath exec -T $Service pg_dump -U $User -d $Database -f /tmp/consulos-dump.sql
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERREUR : pg_dump a échoué." -ForegroundColor Red
    exit 1
}
docker cp "${cid}:/tmp/consulos-dump.sql" $dest
if ($LASTEXITCODE -ne 0 -or -not (Test-Path $dest) -or (Get-Item $dest).Length -lt 50) {
    Write-Host "ERREUR : copie du dump a échoué." -ForegroundColor Red
    exit 1
}
Write-Host "Sauvegarde Postgres terminée." -ForegroundColor Green
Write-Output $dest
exit 0
