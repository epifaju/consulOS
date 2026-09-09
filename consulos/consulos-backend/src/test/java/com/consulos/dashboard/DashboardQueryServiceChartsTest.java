package com.consulos.dashboard;

import com.consulos.dashboard.dto.AgentChargeDTO;
import com.consulos.dashboard.dto.AlerteDTO;
import com.consulos.dashboard.dto.DashboardStatsDTO;
import com.consulos.dashboard.dto.DossierChartDTO;
import com.consulos.dashboard.dto.RecetteChartPointDTO;
import com.consulos.dashboard.dto.RecetteServiceDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Densification Mockito : agents, recettes, charts, dashboard agrégé, alertes restantes.
 */
@ExtendWith(MockitoExtension.class)
class DashboardQueryServiceChartsTest {

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
    void getAgentsCharge_shouldMapAgentRows() {
        UUID agentId = UUID.randomUUID();
        when(repository.agentsCharge(eq(organisationId), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{agentId, "Diallo", "Fatou", 12L, 3L}
        ));

        List<AgentChargeDTO> agents = dashboardQueryService.getAgentsCharge(organisationId);

        assertThat(agents).hasSize(1);
        assertThat(agents.getFirst().agentId()).isEqualTo(agentId);
        assertThat(agents.getFirst().agentNom()).isEqualTo("Diallo Fatou");
        assertThat(agents.getFirst().nbDossiersEnCours()).isEqualTo(12L);
        assertThat(agents.getFirst().nbRdvAujourdhui()).isEqualTo(3L);
    }

    @Test
    void getRecettesParService_shouldMapAmounts() {
        LocalDate debut = LocalDate.of(2026, 9, 1);
        LocalDate fin = LocalDate.of(2026, 9, 30);
        when(repository.recettesParService(organisationId, debut, fin)).thenReturn(List.<Object[]>of(
                new Object[]{"Passeport", new BigDecimal("250.00"), 5L}
        ));

        List<RecetteServiceDTO> result = dashboardQueryService.getRecettesParService(organisationId, debut, fin);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().typeService()).isEqualTo("Passeport");
        assertThat(result.getFirst().montantTotal()).isEqualByComparingTo("250.00");
        assertThat(result.getFirst().nbTransactions()).isEqualTo(5L);
    }

    @Test
    void getAlertesActives_shouldIncludeBloquesAndSurcharges() {
        UUID bloqueId = UUID.randomUUID();
        when(repository.dossiersBloquesEnRevue(organisationId)).thenReturn(List.of(bloqueId));
        when(repository.dossiersEcheanceDepassee(organisationId)).thenReturn(List.of());
        when(repository.agentsSurcharges(organisationId)).thenReturn(List.<Object[]>of(
                new Object[]{UUID.randomUUID(), "Agent", "Surchargé", 25L}
        ));

        List<AlerteDTO> alertes = dashboardQueryService.getAlertesActives(organisationId);

        assertThat(alertes).extracting(AlerteDTO::type)
                .containsExactly("DOSSIERS_BLOQUES", "AGENTS_SURCHARGES");
        assertThat(alertes.getFirst().severite()).isEqualTo("warning");
        assertThat(alertes.get(1).dossiersIds()).isEmpty();
    }

    @Test
    void getDossiersChart_semaine_shouldMapRows() {
        when(repository.dossierChartData(eq(organisationId), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{"Passeport", "SOUMIS", 4L}
        ));

        List<DossierChartDTO> chart = dashboardQueryService.getDossiersChart(organisationId, DashboardPeriode.SEMAINE);

        assertThat(chart).hasSize(1);
        assertThat(chart.getFirst().typeService()).isEqualTo("Passeport");
        assertThat(chart.getFirst().statut()).isEqualTo("SOUMIS");
        assertThat(chart.getFirst().count()).isEqualTo(4L);
    }

    @Test
    void getRecettesChart_shouldConvertSqlDate() {
        when(repository.recetteChartData(eq(organisationId), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{Date.valueOf("2026-09-08"), "ESPECES", new BigDecimal("80.50")}
        ));

        List<RecetteChartPointDTO> chart =
                dashboardQueryService.getRecettesChart(organisationId, DashboardPeriode.MOIS);

        assertThat(chart).hasSize(1);
        assertThat(chart.getFirst().date()).isEqualTo(LocalDate.of(2026, 9, 8));
        assertThat(chart.getFirst().modePaiement()).isEqualTo("ESPECES");
        assertThat(chart.getFirst().montant()).isEqualByComparingTo("80.50");
    }

    @Test
    void getDashboardStats_shouldAggregateKpisAndPeriodRecettes() {
        LocalDate debut = LocalDate.of(2026, 9, 1);
        LocalDate fin = LocalDate.of(2026, 9, 7);
        LocalDate today = LocalDate.now();

        when(repository.countDossiersByStatuts(
                organisationId, List.of("BROUILLON", "SOUMIS", "INCOMPLET", "EN_REVUE", "VALIDE")
        )).thenReturn(20L);
        when(repository.countDossiersByStatuts(
                organisationId, List.of("SOUMIS", "INCOMPLET", "EN_REVUE")
        )).thenReturn(8L);
        when(repository.countDossiersByStatuts(organisationId, List.of("VALIDE", "CLOTURE"))).thenReturn(10L);
        when(repository.countDossiersByStatuts(organisationId, List.of("REJETE"))).thenReturn(2L);
        when(repository.avgDelaiHeures(organisationId, debut, fin)).thenReturn(new BigDecimal("10.00"));
        when(repository.countDossiersEnRetard(organisationId)).thenReturn(1L);

        when(repository.rdvStatsForDay(eq(organisationId), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{"PRESENT", 2L},
                new Object[]{"ABSENT", 1L}
        ));

        when(repository.dossiersParJour(eq(organisationId), any(LocalDate.class), any(LocalDate.class)))
                .thenAnswer(inv -> {
                    LocalDate d0 = inv.getArgument(1);
                    LocalDate d1 = inv.getArgument(2);
                    if (d0.equals(debut) && d1.equals(fin)) {
                        return List.<Object[]>of(new Object[]{debut, 3L}, new Object[]{fin, 2L});
                    }
                    return List.<Object[]>of(new Object[]{d0, 1L});
                });

        when(repository.countDossiersByStatuts(organisationId, List.of("EN_REVUE"))).thenReturn(4L);
        when(repository.sumRecettesJour(organisationId, today)).thenReturn(new BigDecimal("150.00"));
        when(repository.sumRecettesJour(organisationId, today.minusDays(1))).thenReturn(new BigDecimal("100.00"));
        when(repository.recettesParService(organisationId, debut, fin)).thenReturn(List.<Object[]>of(
                new Object[]{"Visa", new BigDecimal("40.00"), 2L},
                new Object[]{"Passeport", new BigDecimal("60.00"), 1L}
        ));

        DashboardStatsDTO stats = dashboardQueryService.getDashboardStats(organisationId, debut, fin);

        assertThat(stats.dossiersOuverts().valeur()).isEqualTo(20L);
        assertThat(stats.enAttenteValidation().valeur()).isEqualTo(4L);
        assertThat(stats.rdvJour().valeur()).isEqualTo(3L);
        assertThat(stats.recettesJour().valeur()).isEqualTo(150L);
        assertThat(stats.recettesJour().tendancePourcent()).isEqualByComparingTo("50.0");
        assertThat(stats.recettesTotalPeriode()).isEqualByComparingTo("100.00");
        assertThat(stats.dossierStats().totalOuverts()).isEqualTo(20L);
        assertThat(stats.rdvStats().presents()).isEqualTo(2L);
    }
}
