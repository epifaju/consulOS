package com.consulos.usager.query.dto;

import com.consulos.document.StatutControle;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UsagerDocumentSummaryDTO(
        UUID id,
        UUID dossierId,
        String dossierReference,
        String nomOriginal,
        String typeDocument,
        String mimeType,
        Long tailleOctets,
        StatutControle statutControle,
        OffsetDateTime createdAt
) {
}
