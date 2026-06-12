package com.consulos.dashboard.dto;

import java.math.BigDecimal;

public record DossierStatsDTO(
        long totalOuverts,
        long totalEnCours,
        long totalValides,
        long totalRejetes,
        BigDecimal delaiMoyenHeures,
        long dossiersEnRetard
) {
}
