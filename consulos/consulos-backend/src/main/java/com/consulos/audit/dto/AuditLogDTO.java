package com.consulos.audit.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditLogDTO(
        UUID id,
        UUID organisationId,
        UUID userId,
        String userNom,
        String userPrenom,
        String action,
        String entiteType,
        UUID entiteId,
        String ancienEtat,
        String nouvelEtat,
        String ipAddress,
        String userAgent,
        OffsetDateTime createdAt
) {
}
