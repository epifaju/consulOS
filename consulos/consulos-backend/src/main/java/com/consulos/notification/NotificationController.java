package com.consulos.notification;

import com.consulos.common.ApiResponse;
import com.consulos.common.PageResponse;
import com.consulos.notification.dto.NotificationDTO;
import com.consulos.notification.dto.UnreadCountDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISEUR','AGENT','CAISSIER','ACCUEIL')")
    public ApiResponse<PageResponse<NotificationDTO>> list(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        UUID organisationId = NotificationSecurityUtils.getOrganisationId();
        UUID userId = NotificationSecurityUtils.getUserId();
        return ApiResponse.ok(notificationService.list(organisationId, userId, pageable));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISEUR','AGENT','CAISSIER','ACCUEIL')")
    public ApiResponse<UnreadCountDTO> unreadCount() {
        UUID organisationId = NotificationSecurityUtils.getOrganisationId();
        UUID userId = NotificationSecurityUtils.getUserId();
        return ApiResponse.ok(notificationService.unreadCount(organisationId, userId));
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISEUR','AGENT','CAISSIER','ACCUEIL')")
    public ApiResponse<NotificationDTO> markRead(@PathVariable UUID id) {
        UUID organisationId = NotificationSecurityUtils.getOrganisationId();
        UUID userId = NotificationSecurityUtils.getUserId();
        return ApiResponse.ok(notificationService.markRead(id, organisationId, userId));
    }

    @PostMapping("/read-all")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISEUR','AGENT','CAISSIER','ACCUEIL')")
    public ApiResponse<UnreadCountDTO> markAllRead() {
        UUID organisationId = NotificationSecurityUtils.getOrganisationId();
        UUID userId = NotificationSecurityUtils.getUserId();
        return ApiResponse.ok(notificationService.markAllRead(organisationId, userId));
    }
}
