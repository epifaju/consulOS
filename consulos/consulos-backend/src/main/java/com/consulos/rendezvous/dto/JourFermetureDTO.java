package com.consulos.rendezvous.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record JourFermetureDTO(
        UUID id,
        @NotNull LocalDate dateFermeture,
        @Size(max = 200) String motif
) {
}
