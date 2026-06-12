package com.consulos.encaissement.dto;

import java.math.BigDecimal;

public record ModeTotalDTO(
        String modePaiement,
        long nombreTransactions,
        BigDecimal total
) {
}
