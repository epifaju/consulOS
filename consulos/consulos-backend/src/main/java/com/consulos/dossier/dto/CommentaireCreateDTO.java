package com.consulos.dossier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentaireCreateDTO(
        @NotBlank @Size(max = 5000) String contenu
) {
}
