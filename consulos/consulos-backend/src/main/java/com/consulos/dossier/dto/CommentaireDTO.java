package com.consulos.dossier.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CommentaireDTO(
        UUID id,
        UUID auteurId,
        String auteurNom,
        String contenu,
        OffsetDateTime createdAt
) {
}
