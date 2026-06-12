package com.consulos.administration.dto;

import java.util.List;
import java.util.UUID;

public record RoleDTO(
        UUID id,
        String code,
        String libelle,
        List<String> permissions
) {
}
