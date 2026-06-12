package com.consulos.dossier.dto;

import com.consulos.dossier.DossierStatut;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DossierTransitionDTO(
        @NotNull DossierStatut statut,
        @Size(max = 2000) String motif
) {
}
