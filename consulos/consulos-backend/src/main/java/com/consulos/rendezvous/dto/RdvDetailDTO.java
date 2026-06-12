package com.consulos.rendezvous.dto;

import com.consulos.rendezvous.RdvStatut;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RdvDetailDTO(
        UUID id,
        UUID usagerId,
        String usagerNom,
        String usagerPrenom,
        String usagerEmail,
        UUID dossierId,
        String dossierReference,
        UUID typeServiceId,
        String typeServiceLibelle,
        UUID agentId,
        String agentNom,
        OffsetDateTime dateHeure,
        Integer dureeMinutes,
        RdvStatut statut,
        String motif,
        String notes,
        Boolean rappelEnvoye,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
