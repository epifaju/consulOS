package com.consulos.document.dto;

import com.consulos.document.StatutControle;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentDTO(
        UUID id,
        UUID dossierId,
        UUID usagerId,
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
