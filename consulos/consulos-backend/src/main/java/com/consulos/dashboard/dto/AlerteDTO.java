package com.consulos.dashboard.dto;

import java.util.List;
import java.util.UUID;

public record AlerteDTO(
        String type,
        String severite,
        String message,
        long count,
        List<UUID> dossiersIds
) {
}
