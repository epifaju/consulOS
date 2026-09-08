# Télécharge les modèles Tesseract (fra + eng) dans tessdata/ à la racine du repo.
# Prérequis : connexion Internet. Sous Windows, Visual C++ Redistributable v14 recommandé.

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$tessdata = Join-Path $Root "tessdata"
New-Item -ItemType Directory -Force -Path $tessdata | Out-Null

$base = "https://github.com/tesseract-ocr/tessdata/raw/main"
$files = @("fra.traineddata", "eng.traineddata")

foreach ($file in $files) {
    $target = Join-Path $tessdata $file
    if (Test-Path $target) {
        Write-Host "Déjà présent : $file" -ForegroundColor Yellow
        continue
    }
    $url = "$base/$file"
    Write-Host "Téléchargement $file ..." -ForegroundColor Cyan
    Invoke-WebRequest -Uri $url -OutFile $target -UseBasicParsing
    Write-Host "OK $target" -ForegroundColor Green
}

Write-Host ""
Write-Host "Tessdata prêt : $tessdata" -ForegroundColor Green
Write-Host "Activer OCR Tesseract :"
Write-Host '  CONSULOS_OCR_ENABLED=true'
Write-Host '  CONSULOS_OCR_PROVIDER=tesseract'
Write-Host ("  CONSULOS_OCR_TESSERACT_DATAPATH=" + $tessdata)
