package com.consulos.portal.dto;

public record PortalSessionResponseDTO(
        String sessionToken,
        PortalDossierSummaryDTO dossier
) {
}
