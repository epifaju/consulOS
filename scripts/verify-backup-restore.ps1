# Test de restore : dump Postgres actuel → conteneur jetable → SELECT flyway_schema_history.
param(
    [string]$ComposeFile = "docker-compose.yml",
    [string]$Database = "consulos_dev",
    [string]$User = "consulos"
)
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$dump = & (Join-Path $PSScriptRoot "backup-postgres.ps1") -ComposeFile $ComposeFile -Database $Database -User $User
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$dump = ($dump | Select-Object -Last 1).ToString().Trim()

$name = "consulos-restore-check"
docker rm -f $name 2>$null | Out-Null
docker run -d --name $name -e POSTGRES_PASSWORD=restore_check_pass -e POSTGRES_USER=$User -e POSTGRES_DB=$Database postgres:16-alpine | Out-Null
try {
    $ready = $false
    for ($i = 0; $i -lt 30; $i++) {
        docker exec $name pg_isready -U $User | Out-Null
        if ($LASTEXITCODE -eq 0) {
            $ready = $true
            break
        }
        Start-Sleep -Seconds 2
    }
    if (-not $ready) {
        throw "Postgres de restore non prêt"
    }
    Get-Content $dump -Raw | docker exec -i $name psql -U $User -d $Database | Out-Null
    $check = docker exec $name psql -U $User -d $Database -tAc "SELECT COUNT(*) FROM flyway_schema_history"
    if (-not $check -or [int]$check -lt 1) {
        throw "Restore invalide (flyway_schema_history vide)"
    }
    Write-Host ("Restore OK - {0} migrations Flyway visibles." -f $check.Trim()) -ForegroundColor Green
}
finally {
    docker rm -f $name 2>$null | Out-Null
}
exit 0
