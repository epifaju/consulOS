package com.consulos.ocr.structured;

import com.consulos.ocr.OcrExtractResult;
import com.consulos.ocr.OcrProvider;

/**
 * OCR stub riche : texte machine-lisible pour pré-remplissage formulaires.
 * Activer avec {@code consulos.ocr.provider=structured-log}.
 */
public class StructuredLoggingOcrProvider implements OcrProvider {

    @Override
    public String providerName() {
        return "structured-log";
    }

    @Override
    public OcrExtractResult extract(String fileName, String mimeType, byte[] content) {
        String seed = fileName != null ? fileName.toLowerCase() : "document";
        String nom = guessNom(seed);
        String prenom = seed.contains("mamadou") ? "Mamadou" : "Aissatou";
        String typeDoc = seed.contains("cni") ? "CNI" : seed.contains("titre") ? "TITRE_SEJOUR" : "PASSEPORT";
        String numero = typeDoc.equals("PASSEPORT") ? "P" + Math.abs(seed.hashCode() % 100000000)
                : "CNI" + Math.abs(seed.hashCode() % 1000000);

        String text = """
                [OCR structured-log]
                Fichier: %s
                MIME: %s
                Taille: %d octets
                ---
                NOM: %s
                PRENOM: %s
                DATE_NAISSANCE: 1990-05-12
                LIEU_NAISSANCE: Bissau
                NATIONALITE: GNB
                NUMERO_DOCUMENT: %s
                TYPE_DOCUMENT: %s
                """.formatted(
                fileName,
                mimeType != null ? mimeType : "unknown",
                content != null ? content.length : 0,
                nom,
                prenom,
                numero,
                typeDoc
        );
        return OcrExtractResult.ok(text);
    }

    private static String guessNom(String seed) {
        if (seed.contains("camara")) {
            return "Camara";
        }
        if (seed.contains("diallo")) {
            return "Diallo";
        }
        if (seed.contains("sow")) {
            return "Sow";
        }
        return "DEMO";
    }
}
