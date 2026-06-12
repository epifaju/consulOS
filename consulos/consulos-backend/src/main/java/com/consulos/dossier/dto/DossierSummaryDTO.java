package com.consulos.dossier.dto;

import com.consulos.dossier.DossierPriorite;
import com.consulos.dossier.DossierStatut;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DossierSummaryDTO(
        UUID id,
        String reference,
        UUID usagerId,
        String usagerNom,
        String usagerPrenom,
        String typeServiceLibelle,
        DossierStatut statut,
        UUID agentId,
        String agentNom,
        DossierPriorite priorite,
        OffsetDateTime dateEcheance
) {
}
