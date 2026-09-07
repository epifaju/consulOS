package com.consulos.portal.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PortalDossierSummaryDTO(
        UUID id,
        String reference,
        String statut,
        String typeServiceLibelle,
        OffsetDateTime dateSoumission,
        OffsetDateTime dateEcheance,
        String usagerPrenom,
        String usagerNom
) {
}
