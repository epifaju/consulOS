package com.consulos.usager.query.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UsagerActivityItemDTO(
        OffsetDateTime occurredAt,
        String category,
        String title,
        String detail,
        UUID relatedId
) {
}
