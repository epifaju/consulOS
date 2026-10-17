# Copie le bucket MinIO consulos-docs vers backups/minio/<timestamp>/
param(
    [string]$ComposeFile = "docker-compose.yml"
)
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$dest = Join-Path $Root "backups\minio\$stamp"

$cid = (docker compose -f (Join-Path $Root $ComposeFile) ps -q minio 2>$null | Select-Object -First 1)
if (-not $cid) {
    Write-Host "MinIO n'est pas démarré — sauvegarde ignorée." -ForegroundColor Yellow
    exit 0
}

New-Item -ItemType Directory -Force -Path $dest | Out-Null
Write-Host "==> Sauvegarde MinIO → $dest" -ForegroundColor Cyan
docker cp "${cid}:/data/consulos-docs" $dest
if ($LASTEXITCODE -ne 0) {
    Write-Host "Bucket consulos-docs introuvable, copie de /data..." -ForegroundColor Yellow
    docker cp "${cid}:/data" $dest
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERREUR : docker cp MinIO a échoué." -ForegroundColor Red
        exit 1
    }
}
Write-Host "Sauvegarde MinIO terminée." -ForegroundColor Green
exit 0
