package com.consulos.sms;

import com.consulos.dossier.TypeServiceEntity;
import com.consulos.dossier.TypeServiceRepository;
import com.consulos.rendezvous.RendezvousEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Envoie un SMS en complément des emails RDV, sans modifier RdvNotificationService.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "consulos.sms", name = "enabled", havingValue = "true")
public class RdvSmsNotificationAspect {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");

    private final SmsService smsService;
    private final TypeServiceRepository typeServiceRepository;

    @After("execution(* com.consulos.rendezvous.RdvNotificationService.sendConfirmation(..)) && args(rdv)")
    public void afterConfirmation(RendezvousEntity rdv) {
        if (rdv == null) {
            return;
        }
        String type = resolveType(rdv);
        String body = "ConsulOS: RDV confirme le %s a %s (%s). Presentez-vous avec vos pieces."
                .formatted(rdv.getDateHeure().format(DATE_FORMAT), rdv.getDateHeure().format(TIME_FORMAT), type);
        safeSend(rdv, "RDV_CONFIRMATION", body);
    }

    @After("execution(* com.consulos.rendezvous.RdvNotificationService.sendAnnulation(..)) && args(rdv, motif)")
    public void afterAnnulation(RendezvousEntity rdv, String motif) {
        if (rdv == null) {
            return;
        }
        String reason = motif != null && !motif.isBlank() ? motif : "non precise";
        String body = "ConsulOS: votre RDV du %s a %s est annule. Motif: %s"
                .formatted(rdv.getDateHeure().format(DATE_FORMAT), rdv.getDateHeure().format(TIME_FORMAT), reason);
        safeSend(rdv, "RDV_ANNULATION", body);
    }

    @After("execution(* com.consulos.rendezvous.RdvNotificationService.sendRappel(..)) && args(rdv)")
    public void afterRappel(RendezvousEntity rdv) {
        if (rdv == null) {
            return;
        }
        String type = resolveType(rdv);
        String body = "ConsulOS: rappel RDV demain %s a %s (%s)."
                .formatted(rdv.getDateHeure().format(DATE_FORMAT), rdv.getDateHeure().format(TIME_FORMAT), type);
        safeSend(rdv, "RDV_RAPPEL", body);
    }

    private void safeSend(RendezvousEntity rdv, String purpose, String body) {
        try {
            smsService.sendRdvSms(
                    rdv.getOrganisationId(),
                    rdv.getUsagerId(),
                    rdv.getId(),
                    purpose,
                    truncate(body, 320)
            );
        } catch (Exception ex) {
            log.warn("SMS {} echoue pour RDV {}: {}", purpose, rdv.getId(), ex.getMessage());
        }
    }

    private String resolveType(RendezvousEntity rdv) {
        if (rdv.getTypeServiceId() == null) {
            return "service consulaire";
        }
        return typeServiceRepository.findById(rdv.getTypeServiceId())
                .map(TypeServiceEntity::getLibelle)
                .orElse("service consulaire");
    }

    private static String truncate(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 1) + "…";
    }
}
