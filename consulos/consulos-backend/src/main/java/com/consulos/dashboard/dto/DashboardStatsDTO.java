package com.consulos.dashboard.dto;

import java.math.BigDecimal;

public record DashboardStatsDTO(
        KpiDTO dossiersOuverts,
        KpiDTO enAttenteValidation,
        KpiDTO rdvJour,
        KpiDTO recettesJour,
        DossierStatsDTO dossierStats,
        RdvStatsDTO rdvStats,
        BigDecimal recettesTotalPeriode
) {
}
