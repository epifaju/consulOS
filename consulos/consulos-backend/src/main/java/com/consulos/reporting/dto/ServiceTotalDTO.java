package com.consulos.reporting.dto;

import java.math.BigDecimal;

public record ServiceTotalDTO(String typeService, BigDecimal montantTotal, long nbTransactions) {
}
