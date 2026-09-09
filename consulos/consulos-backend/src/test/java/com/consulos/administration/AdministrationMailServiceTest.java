package com.consulos.administration;

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
class AdministrationMailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private AdministrationMailService administrationMailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(administrationMailService, "fromAddress", "noreply@consulos.local");
    }

    @Test
    void sendWelcomeEmail_shouldIncludeTemporaryPassword() {
        administrationMailService.sendWelcomeEmail("agent@consulos.local", "Tmp#12345");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly("agent@consulos.local");
        assertThat(message.getSubject()).contains("Compte agent");
        assertThat(message.getText()).contains("Tmp#12345");
    }

    @Test
    void sendWelcomeEmail_whenSmtpFails_shouldNotThrow() {
        doThrow(new MailSendException("down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> administrationMailService.sendWelcomeEmail("a@b.c", "pwd"))
                .doesNotThrowAnyException();
    }
}
