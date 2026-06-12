package com.consulos.dossier.dto;

import com.consulos.dossier.DossierStatut;

import java.util.List;
import java.util.Map;

public record DossierStatsDTO(
        Map<DossierStatut, Long> parStatut,
        List<TypeServiceCountDTO> parTypeService
) {
}
