package com.consulos.dashboard.regional.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RegionalStatsDTO(
        UUID reseauId,
        int postesCount,
        long dossiersOuverts,
        long enRevue,
        long rdvJour,
        long dossiersEnRetard,
        BigDecimal recettesJour,
        BigDecimal recettesPeriode
) {
}
