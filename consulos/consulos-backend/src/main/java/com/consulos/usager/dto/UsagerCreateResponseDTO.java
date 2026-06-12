package com.consulos.usager.dto;

import java.util.List;

public record UsagerCreateResponseDTO(
        UsagerDetailDTO usager,
        List<DoublonGroupDTO> doublonsPotentiels
) {
}
