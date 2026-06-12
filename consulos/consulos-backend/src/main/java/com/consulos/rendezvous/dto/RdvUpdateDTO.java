package com.consulos.rendezvous.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RdvUpdateDTO(
        UUID agentId,
        @Size(max = 2000) String notes
) {
}
