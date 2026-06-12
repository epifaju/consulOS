package com.consulos.usager.dto;

import com.consulos.usager.TypeDocument;
import com.consulos.usager.TypeUsager;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record UsagerCreateDTO(
        @NotBlank String nom,
        @NotBlank String prenom,
        LocalDate dateNaissance,
        String lieuNaissance,
        String nationalite,
        TypeUsager typeUsager,
        String telephone,
        @Email String email,
        String adresse,
        String numeroDocument,
        TypeDocument typeDocument
) {
}
