package com.consulos.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${consulos.mail.from:noreply@consulos.local}")
    private String fromAddress;

    @Value("${consulos.mail.reset-url:http://localhost:3000/reset-password?token=}")
    private String resetUrlBase;

    public void sendPasswordResetEmail(String email, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("ConsulOS — Réinitialisation de mot de passe");
        message.setText("""
                Bonjour,

                Vous avez demandé la réinitialisation de votre mot de passe ConsulOS.
                Cliquez sur le lien suivant (valide 1 heure) :

                %s%s

                Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.

                — Équipe ConsulOS
                """.formatted(resetUrlBase, token));
        try {
            mailSender.send(message);
            log.info("Email de réinitialisation envoyé à {}", email);
        } catch (Exception ex) {
            log.warn("Échec envoi email à {}: {}", email, ex.getMessage());
        }
    }
}
