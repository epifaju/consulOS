package com.consulos.portal;

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
class PortalMailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private PortalMailService portalMailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(portalMailService, "fromAddress", "noreply@consulos.local");
        ReflectionTestUtils.setField(portalMailService, "portalUrlBase", "http://localhost:3010/portal/ouvrir?token=");
    }

    @Test
    void sendMagicLink_shouldIncludeDossierReferenceAndToken() {
        portalMailService.sendMagicLink("diaspora@example.com", "magic-tok", "DOS-2026-0001");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly("diaspora@example.com");
        assertThat(message.getSubject()).contains("DOS-2026-0001");
        assertThat(message.getText()).contains("http://localhost:3010/portal/ouvrir?token=magic-tok");
        assertThat(message.getText()).contains("DOS-2026-0001");
    }

    @Test
    void sendMagicLink_whenSmtpFails_shouldNotThrow() {
        doThrow(new MailSendException("down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> portalMailService.sendMagicLink("a@b.c", "t", "DOS-1"))
                .doesNotThrowAnyException();
    }
}
