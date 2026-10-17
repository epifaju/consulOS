# Compare les chemins Postgres (documents.actif) et les objets MinIO consulos-docs.
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

Write-Host "==> Réconciliation stockage documents (Postgres vs MinIO)" -ForegroundColor Cyan

docker compose ps postgres --status running 2>$null | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERREUR : postgres n'est pas démarré. Lancez .\scripts\start-dev-infra.ps1" -ForegroundColor Red
    exit 1
}

$dbKeys = docker exec econsulos-postgres-1 psql -U consulos -d consulos_dev -t -A -c `
    "SELECT chemin_stockage FROM documents WHERE actif IS TRUE AND chemin_stockage IS NOT NULL AND chemin_stockage <> '';"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

docker exec econsulos-minio-1 mc alias set local http://127.0.0.1:9000 minioadmin minioadmin123 | Out-Null
$mcOut = docker exec econsulos-minio-1 mc ls --recursive local/consulos-docs 2>$null
$minioKeys = @()
foreach ($line in $mcOut) {
    if ($line -match '\s+(\S+)$') {
        $minioKeys += $Matches[1]
    }
}

$dbSet = [System.Collections.Generic.HashSet[string]]::new()
foreach ($k in ($dbKeys -split "`n")) {
    $trim = $k.Trim()
    if ($trim) { [void]$dbSet.Add($trim) }
}
$mcSet = [System.Collections.Generic.HashSet[string]]::new()
foreach ($k in $minioKeys) {
    if ($k) { [void]$mcSet.Add($k) }
}

$missing = @()
foreach ($k in $dbSet) {
    if (-not $mcSet.Contains($k)) { $missing += $k }
}
$orphans = @()
foreach ($k in $mcSet) {
    if (-not $dbSet.Contains($k)) { $orphans += $k }
}

Write-Host ""
Write-Host "  Documents actifs (DB) : $($dbSet.Count)"
Write-Host "  Objets MinIO          : $($mcSet.Count)"
Write-Host "  Absents de MinIO      : $($missing.Count)" -ForegroundColor $(if ($missing.Count -gt 0) { 'Yellow' } else { 'Green' })
Write-Host "  Orphelins MinIO       : $($orphans.Count)"

if ($missing.Count -gt 0) {
    Write-Host ""
    Write-Host "Fichiers à ré-uploader (max 20) :" -ForegroundColor Yellow
    $missing | Select-Object -First 20 | ForEach-Object { Write-Host "  $_" }
}

if ($orphans.Count -gt 0) {
    Write-Host ""
    Write-Host "Clés MinIO sans ligne DB (max 20) :"
    $orphans | Select-Object -First 20 | ForEach-Object { Write-Host "  $_" }
}

Write-Host ""
Write-Host "API admin (JWT) : GET /api/v1/admin/document-storage/reconciliation"
if ($missing.Count -gt 0) { exit 2 }
exit 0
