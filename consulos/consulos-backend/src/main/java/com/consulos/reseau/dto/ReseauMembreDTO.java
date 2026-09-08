package com.consulos.reseau.dto;

import java.util.UUID;

public record ReseauMembreDTO(
        UUID userId,
        String email,
        String nom,
        String prenom,
        String roleReseau,
        UUID organisationId
) {
}
