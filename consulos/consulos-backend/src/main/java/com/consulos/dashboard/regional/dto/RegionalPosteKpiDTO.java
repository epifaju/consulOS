package com.consulos.dashboard.regional.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RegionalPosteKpiDTO(
        UUID organisationId,
        String code,
        String nom,
        long dossiersOuverts,
        long enRevue,
        long rdvJour,
        BigDecimal recettesJour
) {
}
