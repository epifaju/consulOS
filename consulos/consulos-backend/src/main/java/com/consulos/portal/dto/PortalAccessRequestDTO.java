package com.consulos.portal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PortalAccessRequestDTO(
        @NotBlank @Size(max = 30) String reference,
        @NotBlank @Email @Size(max = 255) String email
) {
}
