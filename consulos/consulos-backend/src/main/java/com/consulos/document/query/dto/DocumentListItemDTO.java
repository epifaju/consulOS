package com.consulos.document.query.dto;

import com.consulos.document.StatutControle;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentListItemDTO(
        UUID id,
        UUID dossierId,
        String dossierReference,
        UUID usagerId,
        String usagerNom,
        String usagerPrenom,
        String nomFichier,
        String nomOriginal,
        String typeDocument,
        String mimeType,
        Long tailleOctets,
        StatutControle statutControle,
        Integer version,
        String notes,
        OffsetDateTime createdAt
) {
}
