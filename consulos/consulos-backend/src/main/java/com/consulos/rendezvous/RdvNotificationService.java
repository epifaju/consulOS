package com.consulos.rendezvous;

import com.consulos.auth.repository.OrganisationRepository;
import com.consulos.dossier.TypeServiceEntity;
import com.consulos.dossier.TypeServiceRepository;
import com.consulos.usager.UsagerEntity;
import com.consulos.usager.UsagerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class RdvNotificationService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");

    private final JavaMailSender mailSender;
    private final UsagerRepository usagerRepository;
    private final TypeServiceRepository typeServiceRepository;
    private final OrganisationRepository organisationRepository;

    @Value("${consulos.mail.from:noreply@consulos.local}")
    private String fromAddress;

    @Async
    public void sendConfirmation(RendezvousEntity rdv) {
        UsagerEntity usager = usagerRepository.findById(rdv.getUsagerId()).orElse(null);
        if (usager == null || !StringUtils.hasText(usager.getEmail())) {
            log.info("Pas d'email usager pour confirmation RDV {}", rdv.getId());
            return;
        }

        String typeService = resolveTypeServiceLibelle(rdv.getTypeServiceId());
        String consulatAdresse = resolveConsulatAdresse(rdv.getOrganisationId());
        String html = renderTemplate(
                "templates/email/rdv-confirmation.html",
                usager.getPrenom() + " " + usager.getNom(),
                rdv.getDateHeure().format(DATE_FORMAT),
                rdv.getDateHeure().format(TIME_FORMAT),
                typeService,
                consulatAdresse
        );

        sendHtmlEmail(usager.getEmail(), "ConsulOS — Confirmation de rendez-vous", html);
    }

    @Async
    public void sendAnnulation(RendezvousEntity rdv, String motif) {
        UsagerEntity usager = usagerRepository.findById(rdv.getUsagerId()).orElse(null);
        if (usager == null || !StringUtils.hasText(usager.getEmail())) {
            return;
        }

        String body = """
                Bonjour %s,

                Votre rendez-vous du %s à %s a été annulé.
                Motif : %s

                — Équipe ConsulOS
                """.formatted(
                usager.getPrenom(),
                rdv.getDateHeure().format(DATE_FORMAT),
                rdv.getDateHeure().format(TIME_FORMAT),
                motif != null ? motif : "Non précisé"
        );
        sendPlainEmail(usager.getEmail(), "ConsulOS — Annulation de rendez-vous", body);
    }

    @Async
    public void sendRappel(RendezvousEntity rdv) {
        UsagerEntity usager = usagerRepository.findById(rdv.getUsagerId()).orElse(null);
        if (usager == null || !StringUtils.hasText(usager.getEmail())) {
            return;
        }

        String typeService = resolveTypeServiceLibelle(rdv.getTypeServiceId());
        String body = """
                Bonjour %s,

                Rappel : vous avez un rendez-vous demain (%s à %s) pour %s.

                — Équipe ConsulOS
                """.formatted(
                usager.getPrenom(),
                rdv.getDateHeure().format(DATE_FORMAT),
                rdv.getDateHeure().format(TIME_FORMAT),
                typeService
        );
        sendPlainEmail(usager.getEmail(), "ConsulOS — Rappel de rendez-vous", body);
    }

    private String resolveTypeServiceLibelle(java.util.UUID typeServiceId) {
        if (typeServiceId == null) {
            return "Service consulaire";
        }
        return typeServiceRepository.findById(typeServiceId)
                .map(TypeServiceEntity::getLibelle)
                .orElse("Service consulaire");
    }

    private String resolveConsulatAdresse(java.util.UUID organisationId) {
        return organisationRepository.findById(organisationId)
                .map(org -> org.getNom() + " — " + org.getPaysCode())
                .orElse("Consulat");
    }

    private String renderTemplate(
            String path,
            String usagerNom,
            String date,
            String heure,
            String typeService,
            String consulatAdresse
    ) {
        try {
            String template = StreamUtils.copyToString(
                    new ClassPathResource(path).getInputStream(),
                    StandardCharsets.UTF_8
            );
            return template
                    .replace("{{usagerNom}}", usagerNom)
                    .replace("{{date}}", date)
                    .replace("{{heure}}", heure)
                    .replace("{{typeService}}", typeService)
                    .replace("{{consulatAdresse}}", consulatAdresse);
        } catch (Exception ex) {
            log.warn("Template email introuvable, fallback texte: {}", ex.getMessage());
            return """
                    Bonjour %s, votre rendez-vous est confirmé le %s à %s (%s) — %s
                    """.formatted(usagerNom, date, heure, typeService, consulatAdresse);
        }
    }

    private void sendHtmlEmail(String to, String subject, String html) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email HTML envoyé à {}", to);
        } catch (Exception ex) {
            log.warn("Échec envoi email à {}: {}", to, ex.getMessage());
        }
    }

    private void sendPlainEmail(String to, String subject, String body) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
            log.info("Email envoyé à {}", to);
        } catch (Exception ex) {
            log.warn("Échec envoi email à {}: {}", to, ex.getMessage());
        }
    }
}
