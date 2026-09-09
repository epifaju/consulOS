package com.consulos.sms;

import com.consulos.dossier.TypeServiceRepository;
import com.consulos.rendezvous.RendezvousEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RdvSmsNotificationAspectTest {

    @Mock
    private SmsService smsService;
    @Mock
    private TypeServiceRepository typeServiceRepository;

    @InjectMocks
    private RdvSmsNotificationAspect aspect;

    private RendezvousEntity rdv;

    @BeforeEach
    void setUp() {
        rdv = new RendezvousEntity();
        rdv.setId(UUID.randomUUID());
        rdv.setOrganisationId(UUID.randomUUID());
        rdv.setUsagerId(UUID.randomUUID());
        rdv.setDateHeure(OffsetDateTime.of(2026, 9, 15, 10, 30, 0, 0, ZoneOffset.UTC));
    }

    @Test
    void afterConfirmation_shouldSendRdvSmsWithDate() {
        aspect.afterConfirmation(rdv);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(smsService).sendRdvSms(
                eq(rdv.getOrganisationId()),
                eq(rdv.getUsagerId()),
                eq(rdv.getId()),
                eq("RDV_CONFIRMATION"),
                bodyCaptor.capture()
        );
        assertThat(bodyCaptor.getValue()).contains("15/09/2026");
    }

    @Test
    void afterAnnulation_whenMotifNull_shouldUseNonPrecise() {
        aspect.afterAnnulation(rdv, null);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(smsService).sendRdvSms(
                eq(rdv.getOrganisationId()),
                eq(rdv.getUsagerId()),
                eq(rdv.getId()),
                eq("RDV_ANNULATION"),
                bodyCaptor.capture()
        );
        assertThat(bodyCaptor.getValue()).contains("non precise");
    }

    @Test
    void afterRappel_shouldSendRdvSms() {
        aspect.afterRappel(rdv);

        verify(smsService).sendRdvSms(
                eq(rdv.getOrganisationId()),
                eq(rdv.getUsagerId()),
                eq(rdv.getId()),
                eq("RDV_RAPPEL"),
                any(String.class)
        );
    }

    @Test
    void afterConfirmation_whenRdvNull_shouldNeverSend() {
        aspect.afterConfirmation(null);

        verify(smsService, never()).sendRdvSms(any(), any(), any(), any(), any());
    }

    @Test
    void afterAnnulation_whenRdvNull_shouldNeverSend() {
        aspect.afterAnnulation(null, "motif");

        verify(smsService, never()).sendRdvSms(any(), any(), any(), any(), any());
    }

    @Test
    void afterRappel_whenRdvNull_shouldNeverSend() {
        aspect.afterRappel(null);

        verify(smsService, never()).sendRdvSms(any(), any(), any(), any(), any());
    }
}
