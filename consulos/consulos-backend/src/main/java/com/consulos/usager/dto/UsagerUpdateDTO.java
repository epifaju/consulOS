package com.consulos.usager.dto;

import com.consulos.usager.TypeDocument;
import com.consulos.usager.TypeUsager;

import java.time.LocalDate;

public record UsagerUpdateDTO(
        String nom,
        String prenom,
        LocalDate dateNaissance,
        String lieuNaissance,
        String nationalite,
        TypeUsager typeUsager,
        String telephone,
        String email,
        String adresse,
        String numeroDocument,
        TypeDocument typeDocument
) {
}
