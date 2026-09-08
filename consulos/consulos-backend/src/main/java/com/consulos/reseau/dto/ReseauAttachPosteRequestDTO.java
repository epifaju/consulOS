package com.consulos.reseau.dto;

import jakarta.validation.constraints.NotBlank;

public record ReseauAttachPosteRequestDTO(
        @NotBlank String organisationCode
) {
}
