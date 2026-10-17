# Sauvegarde Postgres + MinIO (répertoire backups/, gitignoré).
param(
    [string]$ComposeFile = "docker-compose.yml",
    [string]$Database = "consulos_dev"
)
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot

& (Join-Path $PSScriptRoot "backup-postgres.ps1") -ComposeFile $ComposeFile -Database $Database
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

& (Join-Path $PSScriptRoot "backup-minio.ps1") -ComposeFile $ComposeFile
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Sauvegarde complète OK." -ForegroundColor Green
exit 0
