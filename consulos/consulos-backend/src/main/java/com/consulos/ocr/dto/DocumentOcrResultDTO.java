package com.consulos.ocr.dto;

import java.util.UUID;

public record DocumentOcrResultDTO(
        UUID documentId,
        String provider,
        String status,
        String extractedText,
        String errorMessage,
        String updatedAt
) {
}
