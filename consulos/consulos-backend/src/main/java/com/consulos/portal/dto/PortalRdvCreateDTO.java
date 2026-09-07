package com.consulos.portal.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record PortalRdvCreateDTO(
        @NotNull OffsetDateTime dateHeure,
        Integer dureeMinutes,
        @Size(max = 2000) String notes
) {
}
