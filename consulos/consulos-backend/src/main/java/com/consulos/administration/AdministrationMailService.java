package com.consulos.administration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdministrationMailService {

    private final JavaMailSender mailSender;

    @Value("${consulos.mail.from:noreply@consulos.local}")
    private String fromAddress;

    public void sendWelcomeEmail(String email, String temporaryPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("ConsulOS — Compte agent créé");
        message.setText("""
                Bonjour,

                Votre compte agent ConsulOS a été créé.
                Mot de passe temporaire : %s

                Connectez-vous et changez votre mot de passe dès votre première connexion.

                — Équipe ConsulOS
                """.formatted(temporaryPassword));
        try {
            mailSender.send(message);
            log.info("Email de bienvenue envoyé à {}", email);
        } catch (Exception ex) {
            log.warn("Échec envoi email de bienvenue à {}: {}", email, ex.getMessage());
        }
    }
}
