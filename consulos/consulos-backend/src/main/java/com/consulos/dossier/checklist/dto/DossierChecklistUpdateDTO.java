package com.consulos.dossier.checklist.dto;

import jakarta.validation.constraints.NotNull;

public record DossierChecklistUpdateDTO(@NotNull Boolean valide) {
}
