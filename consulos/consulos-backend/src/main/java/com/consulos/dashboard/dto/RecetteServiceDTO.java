package com.consulos.dashboard.dto;

import java.math.BigDecimal;

public record RecetteServiceDTO(
        String typeService,
        BigDecimal montantTotal,
        long nbTransactions
) {
}
