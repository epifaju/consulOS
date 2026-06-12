package com.consulos.administration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record OrganisationDTO(
        UUID id,
        @NotBlank @Size(max = 20) String code,
        @NotBlank @Size(max = 200) String nom,
        @NotBlank @Size(max = 3) String paysCode,
        @Size(max = 5) String langueDefaut,
        @Size(max = 3) String deviseDefaut,
        Boolean actif
) {
}
