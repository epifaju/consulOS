package com.consulos.ocr.dto;

import java.util.UUID;

public record OcrFieldSuggestionDTO(
        UUID documentId,
        String provider,
        String status,
        String nom,
        String prenom,
        String dateNaissance,
        String lieuNaissance,
        String nationalite,
        String numeroDocument,
        String typeDocument,
        double confidence
) {
}
