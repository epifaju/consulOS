package com.consulos.usager.dto;

import java.util.List;

public record DoublonGroupDTO(
        List<UsagerSummaryDTO> similaires,
        String motif
) {
}
