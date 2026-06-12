package com.consulos.dashboard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardScheduler {

    private final DashboardQueryRepository dashboardQueryRepository;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void refreshDashboardStats() {
        try {
            dashboardQueryRepository.refreshMaterializedView();
            log.info("Vue matérialisée dashboard_daily_stats rafraîchie");
        } catch (Exception ex) {
            log.warn("Échec rafraîchissement dashboard_daily_stats: {}", ex.getMessage());
        }
    }
}
