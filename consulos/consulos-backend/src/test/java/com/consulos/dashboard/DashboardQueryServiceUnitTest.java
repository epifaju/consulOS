package com.consulos.dashboard;

import com.consulos.dashboard.dto.AlerteDTO;
import com.consulos.dashboard.dto.ChartPointDTO;
import com.consulos.dashboard.dto.DossierStatsDTO;
import com.consulos.dashboard.dto.RdvStatsDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires Mockito (complément de DashboardQueryServiceTest Testcontainers).
 */
@ExtendWith(MockitoExtension.class)
class DashboardQueryServiceUnitTest {

    @Mock
    private DashboardQueryRepository repository;

    @InjectMocks
    private DashboardQueryService dashboardQueryService;

    private UUID organisationId;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
    }

    @Test
    void getDossierStats_shouldAggregateCountsAndDelai() {
        when(repository.countDossiersByStatuts(
                organisationId, List.of("BROUILLON", "SOUMIS", "INCOMPLET", "EN_REVUE", "VALIDE")
        )).thenReturn(10L);
        when(repository.countDossiersByStatuts(
                organisationId, List.of("SOUMIS", "INCOMPLET", "EN_REVUE")
        )).thenReturn(4L);
        when(repository.countDossiersByStatuts(organisationId, List.of("VALIDE", "CLOTURE"))).thenReturn(5L);
        when(repository.countDossiersByStatuts(organisationId, List.of("REJETE"))).thenReturn(1L);
        when(repository.avgDelaiHeures(organisationId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(new BigDecimal("12.345"));
        when(repository.countDossiersEnRetard(organisationId)).thenReturn(2L);

        DossierStatsDTO stats = dashboardQueryService.getDossierStats(
                organisationId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)
        );

        assertThat(stats.totalOuverts()).isEqualTo(10L);
        assertThat(stats.totalEnCours()).isEqualTo(4L);
        assertThat(stats.totalValides()).isEqualTo(5L);
        assertThat(stats.totalRejetes()).isEqualTo(1L);
        assertThat(stats.delaiMoyenHeures()).isEqualByComparingTo("12.35");
        assertThat(stats.dossiersEnRetard()).isEqualTo(2L);
    }

    @Test
    void getRdvStats_shouldComputePresenceRate() {
        LocalDate date = LocalDate.of(2026, 9, 9);
        when(repository.rdvStatsForDay(eq(organisationId), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{"CONFIRME", 2L},
                new Object[]{"EN_ATTENTE", 1L},
                new Object[]{"PRESENT", 4L},
                new Object[]{"ABSENT", 1L},
                new Object[]{"ANNULE", 2L}
        ));

        RdvStatsDTO stats = dashboardQueryService.getRdvStats(organisationId, date);

        assertThat(stats.total()).isEqualTo(10L);
        assertThat(stats.confirmes()).isEqualTo(3L);
        assertThat(stats.presents()).isEqualTo(4L);
        assertThat(stats.absents()).isEqualTo(1L);
        assertThat(stats.tauxPresence()).isEqualByComparingTo("80.0");
    }

    @Test
    void getRdvStats_whenNoEvaluations_shouldReturnZeroRate() {
        when(repository.rdvStatsForDay(eq(organisationId), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{"CONFIRME", 3L}
        ));

        RdvStatsDTO stats = dashboardQueryService.getRdvStats(organisationId, LocalDate.of(2026, 9, 9));

        assertThat(stats.tauxPresence()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getAlertesActives_shouldIncludeCriticalEcheances() {
        UUID dossierId = UUID.randomUUID();
        when(repository.dossiersBloquesEnRevue(organisationId)).thenReturn(List.of());
        when(repository.dossiersEcheanceDepassee(organisationId)).thenReturn(List.of(dossierId));
        when(repository.agentsSurcharges(organisationId)).thenReturn(List.of());

        List<AlerteDTO> alertes = dashboardQueryService.getAlertesActives(organisationId);

        assertThat(alertes).hasSize(1);
        assertThat(alertes.getFirst().type()).isEqualTo("ECHEANCES_DEPASSEES");
        assertThat(alertes.getFirst().severite()).isEqualTo("critical");
        assertThat(alertes.getFirst().count()).isEqualTo(1);
        assertThat(alertes.getFirst().dossiersIds()).containsExactly(dossierId);
    }

    @Test
    void getDossiersParJour_shouldMapLocalDates() {
        when(repository.dossiersParJour(organisationId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3)))
                .thenReturn(List.<Object[]>of(
                        new Object[]{LocalDate.of(2026, 9, 1), 2L},
                        new Object[]{LocalDate.of(2026, 9, 2), 5L}
                ));

        List<ChartPointDTO> points = dashboardQueryService.getDossiersParJour(
                organisationId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3)
        );

        assertThat(points).containsExactly(
                new ChartPointDTO(LocalDate.of(2026, 9, 1), 2L),
                new ChartPointDTO(LocalDate.of(2026, 9, 2), 5L)
        );
    }
}
