package com.consulos.administration.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UserUpdateDTO(
        @Email String email,
        @Size(max = 100) String nom,
        @Size(max = 100) String prenom,
        UUID roleId,
        Boolean actif
) {
}
