package com.consulos.usager.dto;

import com.consulos.usager.TypeUsager;

import java.util.UUID;

public record UsagerSummaryDTO(
        UUID id,
        String numero,
        String nom,
        String prenom,
        String nationalite,
        TypeUsager typeUsager,
        String telephone,
        String email,
        boolean actif
) {
}
