package com.consulos.rendezvous;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RdvSchedulingConfigTest {

    @Mock
    private RendezvousRepository rendezvousRepository;
    @Mock
    private RdvNotificationService notificationService;

    @InjectMocks
    private RdvSchedulingConfig schedulingConfig;

    @Test
    void sendDailyReminders_whenEmpty_shouldNotNotifyOrSave() {
        when(rendezvousRepository.findPendingReminders(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(List.of());

        schedulingConfig.sendDailyReminders();

        verify(notificationService, never()).sendRappel(any());
        verify(rendezvousRepository, never()).save(any());
    }

    @Test
    void sendDailyReminders_whenPending_shouldSendRappelMarkAndSave() {
        RendezvousEntity rdv = new RendezvousEntity();
        rdv.setId(UUID.randomUUID());
        rdv.setRappelEnvoye(false);
        when(rendezvousRepository.findPendingReminders(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(List.of(rdv));
        when(rendezvousRepository.save(any(RendezvousEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        schedulingConfig.sendDailyReminders();

        verify(notificationService).sendRappel(rdv);
        assertThat(rdv.getRappelEnvoye()).isTrue();
        ArgumentCaptor<RendezvousEntity> captor = ArgumentCaptor.forClass(RendezvousEntity.class);
        verify(rendezvousRepository).save(captor.capture());
        assertThat(captor.getValue().getRappelEnvoye()).isTrue();
    }
}
