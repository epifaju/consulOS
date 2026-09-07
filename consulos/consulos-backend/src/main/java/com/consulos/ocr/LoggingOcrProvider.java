package com.consulos.ocr;

/**
 * Stub OCR : produit un texte synthétique pour démo / pré-remplissage futur.
 */
public class LoggingOcrProvider implements OcrProvider {

    @Override
    public String providerName() {
        return "log";
    }

    @Override
    public OcrExtractResult extract(String fileName, String mimeType, byte[] content) {
        int size = content != null ? content.length : 0;
        String text = """
                [OCR stub]
                Fichier: %s
                MIME: %s
                Taille: %d octets
                ---
                Texte extrait simulé — brancher Tesseract (provider=tesseract) pour l'OCR réel.
                Champs candidats: NOM, PRENOM, DATE_NAISSANCE, NUMERO_DOCUMENT.
                """.formatted(fileName, mimeType != null ? mimeType : "unknown", size);
        return OcrExtractResult.ok(text);
    }
}
