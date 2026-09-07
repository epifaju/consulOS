package com.consulos.portal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortalMailService {

    private final JavaMailSender mailSender;

    @Value("${consulos.mail.from:noreply@consulos.local}")
    private String fromAddress;

    @Value("${consulos.mail.portal-url:http://localhost:3000/portal/ouvrir?token=}")
    private String portalUrlBase;

    public void sendMagicLink(String email, String token, String dossierReference) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("ConsulOS — Accès à votre dossier " + dossierReference);
        message.setText("""
                Bonjour,

                Voici votre lien d'accès sécurisé au dossier %s (valide 1 heure) :

                %s%s

                Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.

                — Équipe ConsulOS
                """.formatted(dossierReference, portalUrlBase, token));
        try {
            mailSender.send(message);
            log.info("Email portail envoyé à {} pour {}", email, dossierReference);
        } catch (Exception ex) {
            log.warn("Échec envoi email portail à {}: {}", email, ex.getMessage());
        }
    }
}
