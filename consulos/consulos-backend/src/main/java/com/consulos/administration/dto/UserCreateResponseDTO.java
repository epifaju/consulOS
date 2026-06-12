package com.consulos.administration.dto;

public record UserCreateResponseDTO(
        UserAdminDTO user,
        String temporaryPassword
) {
}
