# Modèles Tesseract (*.traineddata)

Télécharger avec :

```powershell
.\scripts\download-tessdata.ps1
```

Puis :

```env
CONSULOS_OCR_ENABLED=true
CONSULOS_OCR_PROVIDER=tesseract
CONSULOS_OCR_TESSERACT_DATAPATH=./tessdata
CONSULOS_OCR_TESSERACT_LANGUAGE=fra+eng
```

Les fichiers `*.traineddata` ne sont pas versionnés (taille).
