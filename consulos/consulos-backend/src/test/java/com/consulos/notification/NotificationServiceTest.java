package com.consulos.notification;

import com.consulos.auth.entity.UserEntity;
import com.consulos.common.BusinessException;
import com.consulos.notification.dto.NotificationDTO;
import com.consulos.notification.dto.UnreadCountDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationRecipientRepository recipientRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationService notificationService;

    private UUID organisationId;
    private UUID recipientId;
    private UUID actorId;
    private UUID notificationId;
    private NotificationEntity notification;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        recipientId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        notificationId = UUID.randomUUID();

        notification = new NotificationEntity();
        notification.setId(notificationId);
        notification.setOrganisationId(organisationId);
        notification.setRecipientUserId(recipientId);
        notification.setTypeNotification(NotificationService.TYPE_DOSSIER_ASSIGNED);
        notification.setTitre("Dossier assigné");
        notification.setMessage("Un dossier vous a été assigné");
        notification.setLien("/dossiers/" + UUID.randomUUID());
        notification.setLu(false);
        notification.setCreatedAt(LocalDateTime.parse("2026-09-09T10:00:00"));
    }

    @Test
    void list_shouldReturnPagedNotificationsForRecipient() {
        when(notificationRepository.findByOrganisationIdAndRecipientUserIdOrderByCreatedAtDesc(
                eq(organisationId), eq(recipientId), any()
        )).thenReturn(new PageImpl<>(List.of(notification), PageRequest.of(0, 20), 1));

        var page = notificationService.list(organisationId, recipientId, PageRequest.of(0, 20));

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().getFirst().titre()).isEqualTo("Dossier assigné");
        assertThat(page.content().getFirst().lu()).isFalse();
    }

    @Test
    void unreadCount_shouldDelegateToRepository() {
        when(notificationRepository.countByOrganisationIdAndRecipientUserIdAndLuFalse(organisationId, recipientId))
                .thenReturn(3L);

        UnreadCountDTO result = notificationService.unreadCount(organisationId, recipientId);

        assertThat(result.unreadCount()).isEqualTo(3L);
    }

    @Test
    void markRead_shouldSetLuAndLuAt() {
        when(notificationRepository.findByIdAndOrganisationIdAndRecipientUserId(
                notificationId, organisationId, recipientId
        )).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(NotificationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationDTO result = notificationService.markRead(notificationId, organisationId, recipientId);

        assertThat(notification.getLu()).isTrue();
        assertThat(notification.getLuAt()).isNotNull();
        assertThat(result.lu()).isTrue();
    }

    @Test
    void markRead_whenAlreadyRead_shouldNotSaveAgain() {
        notification.setLu(true);
        notification.setLuAt(LocalDateTime.parse("2026-09-01T08:00:00"));
        when(notificationRepository.findByIdAndOrganisationIdAndRecipientUserId(
                notificationId, organisationId, recipientId
        )).thenReturn(Optional.of(notification));

        NotificationDTO result = notificationService.markRead(notificationId, organisationId, recipientId);

        assertThat(result.lu()).isTrue();
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markRead_whenOtherRecipient_shouldThrowNotFound() {
        when(notificationRepository.findByIdAndOrganisationIdAndRecipientUserId(
                notificationId, organisationId, recipientId
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markRead(notificationId, organisationId, recipientId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("NOTIFICATION_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    void markAllRead_shouldReturnUpdatedUnreadCount() {
        when(notificationRepository.countByOrganisationIdAndRecipientUserIdAndLuFalse(organisationId, recipientId))
                .thenReturn(0L);

        UnreadCountDTO result = notificationService.markAllRead(organisationId, recipientId);

        verify(notificationRepository).markAllRead(eq(organisationId), eq(recipientId), any());
        assertThat(result.unreadCount()).isZero();
    }

    @Test
    void notifyUsers_shouldPersistAndPushForEligibleRecipientsOnly() {
        UUID otherRecipient = UUID.randomUUID();
        UserEntity recipient = user(recipientId, "agent@consulos.local");
        UserEntity other = user(otherRecipient, "autre@consulos.local");

        when(recipientRepository.findByIdAndOrganisationIdAndActifTrue(recipientId, organisationId))
                .thenReturn(Optional.of(recipient));
        when(recipientRepository.findByIdAndOrganisationIdAndActifTrue(otherRecipient, organisationId))
                .thenReturn(Optional.of(other));
        when(recipientRepository.findByIdAndOrganisationIdAndActifTrue(UUID.fromString("00000000-0000-0000-0000-000000000099"), organisationId))
                .thenReturn(Optional.empty());
        when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(invocation -> {
            NotificationEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            entity.setCreatedAt(LocalDateTime.now());
            return entity;
        });

        notificationService.notifyUsers(
                organisationId,
                Set.of(recipientId, otherRecipient, actorId, UUID.fromString("00000000-0000-0000-0000-000000000099")),
                NotificationService.TYPE_DOSSIER_TRANSITION,
                "Transition",
                "Statut mis à jour",
                "/dossiers/1",
                actorId
        );

        ArgumentCaptor<NotificationEntity> captor = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(NotificationEntity::getRecipientUserId)
                .containsExactlyInAnyOrder(recipientId, otherRecipient);
        verify(messagingTemplate).convertAndSendToUser(
                eq("agent@consulos.local"), eq("/queue/notifications"), any(NotificationDTO.class));
        verify(messagingTemplate).convertAndSendToUser(
                eq("autre@consulos.local"), eq("/queue/notifications"), any(NotificationDTO.class));
    }

    @Test
    void notifyUsers_whenEmpty_shouldNoOp() {
        notificationService.notifyUsers(organisationId, Set.of(), "T", "t", "m", null, actorId);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void notifySupervisorsAndAgent_shouldIncludeAgentAndSupervisors() {
        UUID supervisorId = UUID.randomUUID();
        UserEntity agent = user(recipientId, "agent@consulos.local");
        UserEntity supervisor = user(supervisorId, "sup@consulos.local");

        when(recipientRepository.findActiveByOrgAndRoles(
                organisationId, List.of("ROLE_SUPERVISEUR", "ROLE_ADMIN")
        )).thenReturn(List.of(supervisor));
        when(recipientRepository.findByIdAndOrganisationIdAndActifTrue(recipientId, organisationId))
                .thenReturn(Optional.of(agent));
        when(recipientRepository.findByIdAndOrganisationIdAndActifTrue(supervisorId, organisationId))
                .thenReturn(Optional.of(supervisor));
        when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(invocation -> {
            NotificationEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            entity.setCreatedAt(LocalDateTime.now());
            return entity;
        });

        notificationService.notifySupervisorsAndAgent(
                organisationId,
                recipientId,
                NotificationService.TYPE_DOSSIER_ASSIGNED,
                "Assigné",
                "Nouveau dossier",
                "/dossiers/x",
                actorId
        );

        verify(notificationRepository, times(2)).save(any(NotificationEntity.class));
    }

    private UserEntity user(UUID id, String email) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setOrganisationId(organisationId);
        user.setEmail(email);
        user.setActif(true);
        return user;
    }
}
