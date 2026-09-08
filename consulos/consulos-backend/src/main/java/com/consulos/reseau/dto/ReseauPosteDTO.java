package com.consulos.reseau.dto;

import java.util.UUID;

public record ReseauPosteDTO(
        UUID organisationId,
        String code,
        String nom,
        String paysCode,
        Boolean actif
) {
}
