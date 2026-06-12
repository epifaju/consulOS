package com.consulos.dossier.dto;

import com.consulos.dossier.DossierStatut;

import java.time.OffsetDateTime;
import java.util.UUID;

public record HistoriqueDTO(
        UUID id,
        DossierStatut statutDe,
        DossierStatut statutVers,
        String commentaire,
        UUID userId,
        String userNom,
        OffsetDateTime createdAt
) {
}
