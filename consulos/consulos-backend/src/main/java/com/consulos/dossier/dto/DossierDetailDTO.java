package com.consulos.dossier.dto;

import com.consulos.dossier.DossierPriorite;
import com.consulos.dossier.DossierStatut;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record DossierDetailDTO(
        UUID id,
        String reference,
        UUID usagerId,
        String usagerNom,
        String usagerPrenom,
        UUID typeServiceId,
        String typeServiceCode,
        String typeServiceLibelle,
        DossierStatut statut,
        UUID agentId,
        String agentNom,
        DossierPriorite priorite,
        OffsetDateTime dateSoumission,
        OffsetDateTime dateEcheance,
        String motifRejet,
        String notesInternes,
        JsonNode metadata,
        JsonNode checklist,
        List<CommentaireDTO> commentaires,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
