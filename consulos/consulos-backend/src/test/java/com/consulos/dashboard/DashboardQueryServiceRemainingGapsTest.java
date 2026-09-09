package com.consulos.dashboard;

import com.consulos.dashboard.dto.AlerteDTO;
import com.consulos.dashboard.dto.DashboardStatsDTO;
import com.consulos.dashboard.dto.DossierChartDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Residuals P2 : alertes vides, fenêtre TRIMESTRE, tendance quand période précédente = 0.
 */
@ExtendWith(MockitoExtension.class)
class DashboardQueryServiceRemainingGapsTest {

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
    void getAlertesActives_whenEmpty_shouldReturnEmpty() {
        when(repository.dossiersBloquesEnRevue(organisationId)).thenReturn(List.of());
        when(repository.dossiersEcheanceDepassee(organisationId)).thenReturn(List.of());
        when(repository.agentsSurcharges(organisationId)).thenReturn(List.of());

        List<AlerteDTO> alertes = dashboardQueryService.getAlertesActives(organisationId);

        assertThat(alertes).isEmpty();
    }

    @Test
    void getDossiersChart_trimestre_shouldQueryLast90Days() {
        LocalDate fin = LocalDate.now();
        LocalDate expectedDebut = fin.minusDays(90);
        when(repository.dossierChartData(eq(organisationId), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{"Visa", "EN_REVUE", 2L}
        ));

        List<DossierChartDTO> chart =
                dashboardQueryService.getDossiersChart(organisationId, DashboardPeriode.TRIMESTRE);

        ArgumentCaptor<LocalDate> debutCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> finCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(repository).dossierChartData(eq(organisationId), debutCaptor.capture(), finCaptor.capture());
        assertThat(debutCaptor.getValue()).isEqualTo(expectedDebut);
        assertThat(finCaptor.getValue()).isEqualTo(fin);
        assertThat(chart).hasSize(1);
        assertThat(chart.getFirst().count()).isEqualTo(2L);
    }

    @Test
    void getDashboardStats_whenPrevRecettesZeroAndTodayPositive_shouldTendance100() {
        LocalDate debut = LocalDate.of(2026, 9, 1);
        LocalDate fin = LocalDate.of(2026, 9, 7);
        LocalDate today = LocalDate.now();

        when(repository.countDossiersByStatuts(
                organisationId, List.of("BROUILLON", "SOUMIS", "INCOMPLET", "EN_REVUE", "VALIDE")
        )).thenReturn(5L);
        when(repository.countDossiersByStatuts(
                organisationId, List.of("SOUMIS", "INCOMPLET", "EN_REVUE")
        )).thenReturn(2L);
        when(repository.countDossiersByStatuts(organisationId, List.of("VALIDE", "CLOTURE"))).thenReturn(1L);
        when(repository.countDossiersByStatuts(organisationId, List.of("REJETE"))).thenReturn(0L);
        when(repository.avgDelaiHeures(organisationId, debut, fin)).thenReturn(BigDecimal.ZERO);
        when(repository.countDossiersEnRetard(organisationId)).thenReturn(0L);

        when(repository.rdvStatsForDay(eq(organisationId), any(), any())).thenReturn(List.of());
        when(repository.dossiersParJour(eq(organisationId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(repository.countDossiersByStatuts(organisationId, List.of("EN_REVUE"))).thenReturn(0L);
        when(repository.sumRecettesJour(organisationId, today)).thenReturn(new BigDecimal("75.00"));
        when(repository.sumRecettesJour(organisationId, today.minusDays(1))).thenReturn(BigDecimal.ZERO);
        when(repository.recettesParService(organisationId, debut, fin)).thenReturn(List.of());

        DashboardStatsDTO stats = dashboardQueryService.getDashboardStats(organisationId, debut, fin);

        assertThat(stats.recettesJour().valeur()).isEqualTo(75L);
        assertThat(stats.recettesJour().tendancePourcent()).isEqualByComparingTo("100");
        assertThat(stats.recettesTotalPeriode()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
