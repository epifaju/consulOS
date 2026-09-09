package com.consulos.sms;

import com.consulos.usager.UsagerEntity;
import com.consulos.usager.UsagerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsServiceTest {

    @Mock
    private SmsGateway smsGateway;
    @Mock
    private SmsLogRepository smsLogRepository;
    @Mock
    private UsagerRepository usagerRepository;
    @Mock
    private SmsProperties properties;

    @InjectMocks
    private SmsService smsService;

    private UUID organisationId;
    private UUID usagerId;
    private UUID rdvId;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        usagerId = UUID.randomUUID();
        rdvId = UUID.randomUUID();
    }

    @ParameterizedTest
    @CsvSource({
            "+221771234567, +221771234567",
            "00221771234567, +221771234567",
            "221771234567, +221771234567",
            "'+221 77-123-4567', +221771234567"
    })
    void normalizePhone_shouldNormalizeValidNumbers(String raw, String expected) {
        assertThat(SmsService.normalizePhone(raw)).isEqualTo(expected);
    }

    @Test
    void normalizePhone_whenInvalid_shouldReturnNull() {
        assertThat(SmsService.normalizePhone("not-a-phone")).isNull();
    }

    @Test
    void sendRdvSms_whenNoPhone_shouldSkip() {
        UsagerEntity usager = new UsagerEntity();
        usager.setId(usagerId);
        usager.setTelephone(null);
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        smsService.sendRdvSms(organisationId, usagerId, rdvId, "CONFIRM", "Bonjour");

        verify(smsGateway, never()).send(any(), any());
        verify(smsLogRepository, never()).save(any());
    }

    @Test
    void sendRdvSms_whenGatewaySucceeds_shouldPersistSentLog() {
        UsagerEntity usager = new UsagerEntity();
        usager.setId(usagerId);
        usager.setTelephone("+221 77 123 45 67");
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));
        when(smsGateway.send("+221771234567", "RDV confirmé")).thenReturn(
                new SmsSendResult(true, "SM123", null));
        when(smsGateway.providerName()).thenReturn("twilio");
        when(smsLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        smsService.sendRdvSms(organisationId, usagerId, rdvId, "CONFIRM", "RDV confirmé");

        ArgumentCaptor<SmsLogEntity> captor = ArgumentCaptor.forClass(SmsLogEntity.class);
        verify(smsLogRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("SENT");
        assertThat(captor.getValue().getRecipient()).isEqualTo("+221771234567");
        assertThat(captor.getValue().getProviderRef()).isEqualTo("SM123");
        assertThat(captor.getValue().getRelatedRdvId()).isEqualTo(rdvId);
    }

    @Test
    void sendRdvSms_whenGatewayFails_shouldPersistFailedLog() {
        UsagerEntity usager = new UsagerEntity();
        usager.setId(usagerId);
        usager.setTelephone("221771234567");
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));
        when(smsGateway.send("+221771234567", "body")).thenReturn(
                new SmsSendResult(false, null, "timeout"));
        when(smsGateway.providerName()).thenReturn("twilio");
        when(smsLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        smsService.sendRdvSms(organisationId, usagerId, rdvId, "CANCEL", "body");

        ArgumentCaptor<SmsLogEntity> captor = ArgumentCaptor.forClass(SmsLogEntity.class);
        verify(smsLogRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(captor.getValue().getErrorMessage()).isEqualTo("timeout");
    }
}
