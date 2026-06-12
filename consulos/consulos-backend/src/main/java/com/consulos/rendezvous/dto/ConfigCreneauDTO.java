package com.consulos.rendezvous.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.UUID;

public record ConfigCreneauDTO(
        UUID id,
        UUID typeServiceId,
        @NotNull @Min(1) @Max(7) Integer jourSemaine,
        @NotNull LocalTime heureDebut,
        @NotNull LocalTime heureFin,
        Integer dureeMinutes,
        Integer quotaMax,
        Boolean actif
) {
}
