package com.consulos.notification;

import com.consulos.auth.entity.UserEntity;
import com.consulos.common.BusinessException;
import com.consulos.common.PageResponse;
import com.consulos.notification.dto.NotificationDTO;
import com.consulos.notification.dto.UnreadCountDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    public static final String TYPE_DOSSIER_TRANSITION = "DOSSIER_TRANSITION";
    public static final String TYPE_DOSSIER_ASSIGNED = "DOSSIER_ASSIGNED";

    private final NotificationRepository notificationRepository;
    private final NotificationRecipientRepository recipientRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public PageResponse<NotificationDTO> list(UUID organisationId, UUID recipientUserId, Pageable pageable) {
        Page<NotificationDTO> page = notificationRepository
                .findByOrganisationIdAndRecipientUserIdOrderByCreatedAtDesc(organisationId, recipientUserId, pageable)
                .map(this::toDto);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public UnreadCountDTO unreadCount(UUID organisationId, UUID recipientUserId) {
        long count = notificationRepository.countByOrganisationIdAndRecipientUserIdAndLuFalse(
                organisationId, recipientUserId);
        return new UnreadCountDTO(count);
    }

    @Transactional
    public NotificationDTO markRead(UUID notificationId, UUID organisationId, UUID recipientUserId) {
        NotificationEntity entity = notificationRepository
                .findByIdAndOrganisationIdAndRecipientUserId(notificationId, organisationId, recipientUserId)
                .orElseThrow(() -> new BusinessException(
                        "NOTIFICATION_NOT_FOUND", "Notification introuvable", HttpStatus.NOT_FOUND));
        if (!Boolean.TRUE.equals(entity.getLu())) {
            entity.setLu(true);
            entity.setLuAt(LocalDateTime.now());
            entity.setUpdatedBy(recipientUserId);
            entity = notificationRepository.save(entity);
        }
        return toDto(entity);
    }

    @Transactional
    public UnreadCountDTO markAllRead(UUID organisationId, UUID recipientUserId) {
        notificationRepository.markAllRead(organisationId, recipientUserId, LocalDateTime.now());
        return unreadCount(organisationId, recipientUserId);
    }

    @Transactional
    public void notifyUsers(
            UUID organisationId,
            Set<UUID> recipientUserIds,
            String type,
            String titre,
            String message,
            String lien,
            UUID actorUserId
    ) {
        if (recipientUserIds == null || recipientUserIds.isEmpty()) {
            return;
        }
        for (UUID recipientId : recipientUserIds) {
            if (recipientId == null || recipientId.equals(actorUserId)) {
                continue;
            }
            UserEntity recipient = recipientRepository
                    .findByIdAndOrganisationIdAndActifTrue(recipientId, organisationId)
                    .orElse(null);
            if (recipient == null) {
                continue;
            }
            NotificationEntity entity = new NotificationEntity();
            entity.setOrganisationId(organisationId);
            entity.setRecipientUserId(recipientId);
            entity.setTypeNotification(type);
            entity.setTitre(titre);
            entity.setMessage(message);
            entity.setLien(lien);
            entity.setLu(false);
            entity.setCreatedBy(actorUserId);
            entity.setUpdatedBy(actorUserId);
            NotificationEntity saved = notificationRepository.save(entity);
            pushRealtime(recipient.getEmail(), toDto(saved));
        }
    }

    @Transactional
    public void notifySupervisorsAndAgent(
            UUID organisationId,
            UUID agentId,
            String type,
            String titre,
            String message,
            String lien,
            UUID actorUserId
    ) {
        Set<UUID> recipients = new HashSet<>();
        if (agentId != null) {
            recipients.add(agentId);
        }
        List<UserEntity> supervisors = recipientRepository.findActiveByOrgAndRoles(
                organisationId,
                List.of("ROLE_SUPERVISEUR", "ROLE_ADMIN")
        );
        supervisors.forEach(u -> recipients.add(u.getId()));
        notifyUsers(organisationId, recipients, type, titre, message, lien, actorUserId);
    }

    private void pushRealtime(String recipientEmail, NotificationDTO dto) {
        try {
            messagingTemplate.convertAndSendToUser(recipientEmail, "/queue/notifications", dto);
        } catch (Exception ex) {
            log.debug("Push WS notification ignoré pour {}: {}", recipientEmail, ex.getMessage());
        }
    }

    private NotificationDTO toDto(NotificationEntity entity) {
        return new NotificationDTO(
                entity.getId(),
                entity.getTypeNotification(),
                entity.getTitre(),
                entity.getMessage(),
                entity.getLien(),
                Boolean.TRUE.equals(entity.getLu()),
                entity.getCreatedAt()
        );
    }
}
