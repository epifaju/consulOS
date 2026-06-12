package com.consulos.encaissement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record TarifDTO(
        UUID id,
        UUID typeServiceId,
        @NotBlank @Size(max = 200) String libelle,
        @NotNull @DecimalMin("0") BigDecimal montant,
        @Size(max = 3) String devise,
        Boolean actif
) {
}
