package com.consulos.dashboard.dto;

import java.math.BigDecimal;

public record KpiDTO(
        String label,
        long valeur,
        long valeurPrecedente,
        BigDecimal tendancePourcent
) {
}
