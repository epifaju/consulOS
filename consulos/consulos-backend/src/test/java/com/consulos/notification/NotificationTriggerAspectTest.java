package com.consulos.notification;

import com.consulos.auth.ConsulosUserDetails;
import com.consulos.dossier.DossierStatut;
import com.consulos.dossier.dto.DossierDetailDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NotificationTriggerAspectTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationTriggerAspect aspect;

    private UUID userId;
    private UUID organisationId;
    private UUID agentId;
    private UUID dossierId;
    private DossierDetailDTO detail;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        organisationId = UUID.randomUUID();
        agentId = UUID.randomUUID();
        dossierId = UUID.randomUUID();

        ConsulosUserDetails details = new ConsulosUserDetails(
                userId, "agent@consulos.local", "hash", organisationId, "ROLE_AGENT", true
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities())
        );

        detail = new DossierDetailDTO(
                dossierId,
                "DOS-2026-00042",
                null,
                null,
                null,
                null,
                null,
                null,
                DossierStatut.EN_REVUE,
                agentId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                null,
                null
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void afterDossierTransition_shouldNotifySupervisorsAndAgent() {
        aspect.afterDossierTransition(detail);

        verify(notificationService).notifySupervisorsAndAgent(
                eq(organisationId),
                eq(agentId),
                eq(NotificationService.TYPE_DOSSIER_TRANSITION),
                eq("Dossier DOS-2026-00042"),
                eq("Nouveau statut : EN_REVUE"),
                eq("/dossiers/" + dossierId),
                eq(userId)
        );
    }

    @Test
    void afterDossierAssign_shouldNotifyUsers() {
        aspect.afterDossierAssign(detail);

        verify(notificationService).notifyUsers(
                eq(organisationId),
                eq(Set.of(agentId)),
                eq(NotificationService.TYPE_DOSSIER_ASSIGNED),
                eq("Dossier assigné — DOS-2026-00042"),
                eq("Un dossier vous a été assigné."),
                eq("/dossiers/" + dossierId),
                eq(userId)
        );
    }

    @Test
    void afterDossierTransition_whenDetailNull_shouldDoNothing() {
        aspect.afterDossierTransition(null);

        verifyNoInteractions(notificationService);
    }

    @Test
    void afterDossierAssign_whenDetailNull_shouldDoNothing() {
        aspect.afterDossierAssign(null);

        verifyNoInteractions(notificationService);
    }

    @Test
    void afterDossierTransition_whenNoAuth_shouldDoNothing() {
        SecurityContextHolder.clearContext();

        aspect.afterDossierTransition(detail);

        verify(notificationService, never()).notifySupervisorsAndAgent(
                any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void afterDossierAssign_whenNoAuth_shouldDoNothing() {
        SecurityContextHolder.clearContext();

        aspect.afterDossierAssign(detail);

        verify(notificationService, never()).notifyUsers(
                any(), any(), any(), any(), any(), any(), any()
        );
    }
}
