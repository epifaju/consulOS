package com.consulos.portal.dto;

import jakarta.validation.constraints.NotBlank;

public record PortalSessionRequestDTO(@NotBlank String token) {
}
