package com.consulos.rendezvous;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class RdvSchedulingConfig {

    private final RendezvousRepository rendezvousRepository;
    private final RdvNotificationService notificationService;

    @Scheduled(cron = "0 0 9 * * *")
    public void sendDailyReminders() {
        LocalDate tomorrow = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        OffsetDateTime debut = tomorrow.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime fin = tomorrow.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        List<RendezvousEntity> rdvs = rendezvousRepository.findPendingReminders(debut, fin);
        for (RendezvousEntity rdv : rdvs) {
            notificationService.sendRappel(rdv);
            rdv.setRappelEnvoye(true);
            rendezvousRepository.save(rdv);
        }
        if (!rdvs.isEmpty()) {
            log.info("{} rappels de rendez-vous envoyés", rdvs.size());
        }
    }
}
