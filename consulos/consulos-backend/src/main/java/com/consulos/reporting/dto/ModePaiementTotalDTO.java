package com.consulos.reporting.dto;

import java.math.BigDecimal;

public record ModePaiementTotalDTO(String mode, long count, BigDecimal montant) {
}
