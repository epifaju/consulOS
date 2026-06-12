package com.consulos.rendezvous.dto;

import com.consulos.rendezvous.RdvStatut;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RdvSummaryDTO(
        UUID id,
        UUID usagerId,
        String usagerNom,
        String usagerPrenom,
        UUID typeServiceId,
        String typeServiceLibelle,
        UUID agentId,
        String agentNom,
        OffsetDateTime dateHeure,
        Integer dureeMinutes,
        RdvStatut statut
) {
}
