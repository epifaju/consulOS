package com.consulos.encaissement.dto;

import com.consulos.encaissement.ModePaiement;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record EncaissementCreateDTO(
        @NotNull UUID usagerId,
        UUID dossierId,
        @NotNull @DecimalMin("0.01") BigDecimal montantTotal,
        @Size(max = 3) String devise,
        ModePaiement modePaiement,
        @Size(max = 2000) String notes
) {
}
