package com.consulos.reporting.dto;

import java.math.BigDecimal;
import java.util.List;

public record EncaissementTotauxDTO(
        BigDecimal montantPercuTotal,
        long nbTransactions,
        List<ModePaiementTotalDTO> parMode,
        List<ServiceTotalDTO> parService
) {
}
