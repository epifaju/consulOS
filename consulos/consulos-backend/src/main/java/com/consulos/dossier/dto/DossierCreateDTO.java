package com.consulos.dossier.dto;

import com.consulos.dossier.DossierPriorite;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record DossierCreateDTO(
        @NotNull UUID usagerId,
        @NotNull UUID typeServiceId,
        DossierPriorite priorite,
        @Size(max = 5000) String notesInternes
) {
}
