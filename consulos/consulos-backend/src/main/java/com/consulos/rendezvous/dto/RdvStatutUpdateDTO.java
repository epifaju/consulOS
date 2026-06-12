package com.consulos.rendezvous.dto;

import com.consulos.rendezvous.RdvStatut;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RdvStatutUpdateDTO(
        @NotNull RdvStatut statut,
        @Size(max = 2000) String motif
) {
}
