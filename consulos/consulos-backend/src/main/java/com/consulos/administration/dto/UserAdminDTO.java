package com.consulos.administration.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserAdminDTO(
        UUID id,
        String email,
        String nom,
        String prenom,
        String roleCode,
        String roleLibelle,
        Boolean actif,
        OffsetDateTime derniereConnexion,
        OffsetDateTime createdAt
) {
}
