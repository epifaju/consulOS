package com.consulos.dossier.assign.dto;

import java.util.UUID;

public record AgentOptionDTO(
        UUID id,
        String nom,
        String prenom,
        String email,
        String roleCode
) {
}
