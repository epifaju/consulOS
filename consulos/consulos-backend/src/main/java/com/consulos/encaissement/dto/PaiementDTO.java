package com.consulos.encaissement.dto;

import com.consulos.encaissement.ModePaiement;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PaiementDTO(
        @NotNull @DecimalMin("0.01") BigDecimal montant,
        @NotNull ModePaiement modePaiement,
        @Size(max = 2000) String notes
) {
}
