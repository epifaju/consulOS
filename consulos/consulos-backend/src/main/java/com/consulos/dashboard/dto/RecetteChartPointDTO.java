package com.consulos.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecetteChartPointDTO(
        LocalDate date,
        String modePaiement,
        BigDecimal montant
) {
}
