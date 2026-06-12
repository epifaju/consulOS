package com.consulos.auth.dto;

import java.util.UUID;

public record UserProfileDTO(
        UUID id,
        String email,
        String nom,
        String prenom,
        String role,
        UUID organisationId,
        String organisationNom,
        String organisationCode
) {
}
