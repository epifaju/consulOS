package com.consulos.dossier.dto;

import com.consulos.dossier.DossierPriorite;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record DossierUpdateDTO(
        @Size(max = 5000) String notesInternes,
        DossierPriorite priorite,
        UUID agentId
) {
}
