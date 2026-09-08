package com.consulos.reporting.regional.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RegionalPosteMensuelDTO(
        UUID organisationId,
        String code,
        String nom,
        long dossiersCrees,
        BigDecimal montantPercu,
        long nbEncaissements,
        long rdvTotal
) {
}
