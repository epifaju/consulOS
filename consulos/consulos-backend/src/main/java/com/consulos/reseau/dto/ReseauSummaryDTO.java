package com.consulos.reseau.dto;

import java.util.List;
import java.util.UUID;

public record ReseauSummaryDTO(
        UUID id,
        String code,
        String nom,
        String description,
        String roleReseau,
        List<ReseauPosteDTO> postes
) {
}
