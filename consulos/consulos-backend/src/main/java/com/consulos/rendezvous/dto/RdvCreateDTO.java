package com.consulos.rendezvous.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RdvCreateDTO(
        @NotNull UUID usagerId,
        @NotNull OffsetDateTime dateHeure,
        UUID typeServiceId,
        UUID dossierId,
        UUID agentId,
        Integer dureeMinutes,
        @Size(max = 2000) String notes
) {
}
