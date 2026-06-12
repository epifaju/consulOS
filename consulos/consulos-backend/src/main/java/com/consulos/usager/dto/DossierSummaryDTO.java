package com.consulos.usager.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DossierSummaryDTO(
        UUID id,
        String numero,
        String typeService,
        String statut,
        LocalDateTime createdAt
) {
}
