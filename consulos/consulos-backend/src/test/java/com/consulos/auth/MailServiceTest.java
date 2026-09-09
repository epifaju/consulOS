package com.consulos.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private MailService mailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mailService, "fromAddress", "noreply@consulos.local");
        ReflectionTestUtils.setField(mailService, "resetUrlBase", "http://localhost:3010/reset-password?token=");
    }

    @Test
    void sendPasswordResetEmail_shouldBuildMessageWithTokenLink() {
        mailService.sendPasswordResetEmail("user@example.com", "tok-123");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getFrom()).isEqualTo("noreply@consulos.local");
        assertThat(message.getTo()).containsExactly("user@example.com");
        assertThat(message.getSubject()).contains("Réinitialisation");
        assertThat(message.getText()).contains("http://localhost:3010/reset-password?token=tok-123");
    }

    @Test
    void sendPasswordResetEmail_whenSmtpFails_shouldNotThrow() {
        doThrow(new MailSendException("down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> mailService.sendPasswordResetEmail("user@example.com", "tok"))
                .doesNotThrowAnyException();
    }
}
