package com.consulos.portal.dto;

import java.util.UUID;

public record PortalDocumentDTO(
        UUID id,
        String nomOriginal,
        String typeDocument,
        String statutControle,
        long tailleOctets,
        String createdAt
) {
}
