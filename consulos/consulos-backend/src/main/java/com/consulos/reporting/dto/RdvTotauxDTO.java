package com.consulos.reporting.dto;

import java.util.List;

public record RdvTotauxDTO(
        long total,
        List<StatutCountDTO> parStatut
) {
}
