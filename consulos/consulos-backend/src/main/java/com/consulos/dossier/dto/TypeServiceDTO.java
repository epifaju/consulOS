package com.consulos.dossier.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.UUID;

public record TypeServiceDTO(
        UUID id,
        String code,
        String libelle,
        Integer delaiJours,
        BigDecimal tarifDefaut,
        JsonNode checklist
) {
}
