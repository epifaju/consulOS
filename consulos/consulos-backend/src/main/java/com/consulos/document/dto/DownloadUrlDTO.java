package com.consulos.document.dto;

import java.time.OffsetDateTime;

public record DownloadUrlDTO(
        String url,
        OffsetDateTime expiresAt
) {
}
