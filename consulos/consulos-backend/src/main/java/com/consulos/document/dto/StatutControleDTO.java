package com.consulos.document.dto;

import com.consulos.document.StatutControle;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StatutControleDTO(
        @NotNull StatutControle statut,
        @Size(max = 2000) String notes
) {
}
