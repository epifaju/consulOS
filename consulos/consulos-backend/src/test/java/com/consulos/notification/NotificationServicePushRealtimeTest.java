package com.consulos.notification;

import com.consulos.auth.entity.UserEntity;
import com.consulos.notification.dto.NotificationDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServicePushRealtimeTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationRecipientRepository recipientRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void notifyUsers_whenPushThrows_shouldStillPersist() {
        UUID organisationId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        UserEntity recipient = new UserEntity();
        recipient.setId(recipientId);
        recipient.setOrganisationId(organisationId);
        recipient.setEmail("agent@consulos.local");
        recipient.setActif(true);

        when(recipientRepository.findByIdAndOrganisationIdAndActifTrue(recipientId, organisationId))
                .thenReturn(Optional.of(recipient));
        when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(invocation -> {
            NotificationEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            entity.setCreatedAt(LocalDateTime.now());
            return entity;
        });
        doThrow(new RuntimeException("ws down"))
                .when(messagingTemplate)
                .convertAndSendToUser(eq("agent@consulos.local"), eq("/queue/notifications"), any(NotificationDTO.class));

        notificationService.notifyUsers(
                organisationId,
                Set.of(recipientId),
                NotificationService.TYPE_DOSSIER_TRANSITION,
                "Transition",
                "Statut mis à jour",
                "/dossiers/1",
                actorId
        );

        verify(notificationRepository).save(any(NotificationEntity.class));
        verify(messagingTemplate).convertAndSendToUser(
                eq("agent@consulos.local"), eq("/queue/notifications"), any(NotificationDTO.class));
    }
}
