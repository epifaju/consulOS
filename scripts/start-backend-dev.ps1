# Démarre l'infra puis le backend Spring Boot
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot

& "$Root\scripts\start-dev-infra.ps1"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.10"
if (-not (Test-Path $env:JAVA_HOME)) {
    Write-Host "JAVA_HOME introuvable : $env:JAVA_HOME" -ForegroundColor Red
    exit 1
}

if (-not $env:SERVER_PORT) {
    $env:SERVER_PORT = "8082"
}

Push-Location "$Root\consulos\consulos-backend"
mvn spring-boot:run -DskipTests
Pop-Location
