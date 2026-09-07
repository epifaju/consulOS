package com.consulos.notification;

import com.consulos.auth.ConsulosUserDetails;
import com.consulos.dossier.dto.DossierDetailDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class NotificationTriggerAspect {

    private final NotificationService notificationService;

    @AfterReturning(
            pointcut = "execution(* com.consulos.dossier.DossierService.transition(..))",
            returning = "detail"
    )
    public void afterDossierTransition(DossierDetailDTO detail) {
        if (detail == null) {
            return;
        }
        UUID actorId = currentUserId();
        UUID organisationId = currentOrganisationId();
        if (organisationId == null) {
            return;
        }
        String titre = "Dossier " + detail.reference();
        String message = "Nouveau statut : " + detail.statut();
        String lien = "/dossiers/" + detail.id();
        try {
            notificationService.notifySupervisorsAndAgent(
                    organisationId,
                    detail.agentId(),
                    NotificationService.TYPE_DOSSIER_TRANSITION,
                    titre,
                    message,
                    lien,
                    actorId
            );
        } catch (Exception ex) {
            log.warn("Échec notification transition dossier {}: {}", detail.reference(), ex.getMessage());
        }
    }

    @AfterReturning(
            pointcut = "execution(* com.consulos.dossier.DossierService.assignerAgent(..))",
            returning = "detail"
    )
    public void afterDossierAssign(DossierDetailDTO detail) {
        if (detail == null || detail.agentId() == null) {
            return;
        }
        UUID actorId = currentUserId();
        UUID organisationId = currentOrganisationId();
        if (organisationId == null) {
            return;
        }
        try {
            notificationService.notifyUsers(
                    organisationId,
                    Set.of(detail.agentId()),
                    NotificationService.TYPE_DOSSIER_ASSIGNED,
                    "Dossier assigné — " + detail.reference(),
                    "Un dossier vous a été assigné.",
                    "/dossiers/" + detail.id(),
                    actorId
            );
        } catch (Exception ex) {
            log.warn("Échec notification assignation dossier {}: {}", detail.reference(), ex.getMessage());
        }
    }

    private UUID currentUserId() {
        ConsulosUserDetails user = currentUser();
        return user != null ? user.getUserId() : null;
    }

    private UUID currentOrganisationId() {
        ConsulosUserDetails user = currentUser();
        return user != null ? user.getOrganisationId() : null;
    }

    private ConsulosUserDetails currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof ConsulosUserDetails userDetails) {
            return userDetails;
        }
        return null;
    }
}
