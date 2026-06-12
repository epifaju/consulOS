package com.consulos.usager.dto;

import com.consulos.usager.TypeDocument;
import com.consulos.usager.TypeUsager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record UsagerDetailDTO(
        UUID id,
        String numero,
        String nom,
        String prenom,
        LocalDate dateNaissance,
        String lieuNaissance,
        String nationalite,
        TypeUsager typeUsager,
        String telephone,
        String email,
        String adresse,
        String numeroDocument,
        TypeDocument typeDocument,
        Map<String, Object> metadata,
        boolean actif,
        UUID organisationId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
