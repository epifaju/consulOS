package com.consulos.dashboard.dto;

public record DossierChartDTO(
        String typeService,
        String statut,
        long count
) {
}
