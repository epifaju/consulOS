package com.consulos.encaissement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record JournalCaisseDTO(
        LocalDate date,
        List<ModeTotalDTO> parMode,
        long nombreTransactions,
        BigDecimal montantTotalPercu
) {
}
