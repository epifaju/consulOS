package com.consulos.dashboard.dto;

import java.math.BigDecimal;

public record RdvStatsDTO(
        long total,
        long confirmes,
        long presents,
        long absents,
        BigDecimal tauxPresence
) {
}
