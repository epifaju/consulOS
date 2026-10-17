# Refuse les secrets de démo avant un compose prod / pilote.
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $Root ".env.prod"

if (-not (Test-Path $envFile)) {
    Write-Host "Fichier .env.prod introuvable. Copiez .env.prod.example vers .env.prod." -ForegroundColor Red
    exit 1
}

$forbidden = @(
    "Admin1234!",
    "consulos_dev_pass",
    "redis_dev_pass",
    "minioadmin",
    "minioadmin123",
    "password",
    "changeme"
)

$required = @(
    "POSTGRES_PASSWORD",
    "REDIS_PASSWORD",
    "MINIO_ACCESS_KEY",
    "MINIO_SECRET_KEY"
)

$values = @{}
Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq "" -or $line.StartsWith("#")) {
        return
    }
    $idx = $line.IndexOf("=")
    if ($idx -lt 1) {
        return
    }
    $key = $line.Substring(0, $idx).Trim()
    $val = $line.Substring($idx + 1).Trim()
    $values[$key] = $val
}

$failed = $false
foreach ($key in $required) {
    if (-not $values.ContainsKey($key) -or [string]::IsNullOrWhiteSpace($values[$key])) {
        Write-Host "Manquant : $key" -ForegroundColor Red
        $failed = $true
        continue
    }
    foreach ($bad in $forbidden) {
        if ($values[$key] -ieq $bad) {
            Write-Host "Secret démo interdit pour $key" -ForegroundColor Red
            $failed = $true
        }
    }
}

$jwtPriv = Join-Path $Root "secrets\jwt-private.pem"
$jwtPub = Join-Path $Root "secrets\jwt-public.pem"
if (-not (Test-Path $jwtPriv) -or -not (Test-Path $jwtPub)) {
    Write-Host "Clés JWT absentes (.\scripts\generate-jwt-keys.ps1)." -ForegroundColor Red
    $failed = $true
}

if ($failed) {
    exit 1
}
Write-Host "Secrets pilote : OK (aucun mot de passe démo)." -ForegroundColor Green
exit 0
