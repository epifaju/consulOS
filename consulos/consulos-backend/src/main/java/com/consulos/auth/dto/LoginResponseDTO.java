package com.consulos.auth.dto;

public record LoginResponseDTO(
        String accessToken,
        UserProfileDTO user
) {
}
