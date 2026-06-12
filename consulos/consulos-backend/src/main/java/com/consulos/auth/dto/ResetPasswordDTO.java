package com.consulos.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordDTO(
        @NotBlank String token,
        @NotBlank
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "Le mot de passe doit contenir au moins 8 caractères, une majuscule et un chiffre"
        )
        String newPassword,
        @NotBlank String confirmPassword
) {
    @AssertTrue(message = "Les mots de passe ne correspondent pas")
    public boolean isPasswordMatching() {
        if (newPassword == null || confirmPassword == null) {
            return false;
        }
        return newPassword.equals(confirmPassword);
    }
}
