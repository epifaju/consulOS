package com.consulos.notification.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationDTO(
        UUID id,
        String typeNotification,
        String titre,
        String message,
        String lien,
        boolean lu,
        LocalDateTime createdAt
) {
}
