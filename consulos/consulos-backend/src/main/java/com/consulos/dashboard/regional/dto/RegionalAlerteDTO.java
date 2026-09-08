package com.consulos.dashboard.regional.dto;

import java.util.List;
import java.util.UUID;

public record RegionalAlerteDTO(
        String type,
        String severite,
        String message,
        long count,
        List<String> posteCodes,
        List<UUID> dossiersIds
) {
}
