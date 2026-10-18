package com.consulos.dashboard;

import com.consulos.dashboard.dto.AgentChargeDTO;
import com.consulos.dashboard.dto.DashboardStatsDTO;
import com.consulos.dashboard.dto.DossierChartDTO;
import com.consulos.dashboard.dto.DossierStatsDTO;
import com.consulos.dashboard.dto.KpiDTO;
import com.consulos.dashboard.dto.RdvStatsDTO;
import com.consulos.dashboard.dto.RecetteChartPointDTO;
import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = DashboardController.class)
class DashboardControllerMoreWebMvcTest {

    private static final UUID ORG_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID AGENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardQueryService dashboardQueryService;

    @Test
    @WithConsulosUser(role = "ROLE_SUPERVISEUR")
    void stats_asSuperviseur_returnsKpis() throws Exception {
        KpiDTO kpi = new KpiDTO("ouverts", 10L, 8L, new BigDecimal("25"));
        DashboardStatsDTO stats = new DashboardStatsDTO(
                kpi, kpi, kpi, kpi,
                new DossierStatsDTO(10, 4, 3, 1, BigDecimal.ONE, 0),
                new RdvStatsDTO(5, 4, 3, 1, new BigDecimal("75")),
                new BigDecimal("100.00")
        );
        when(dashboardQueryService.getDashboardStats(eq(ORG_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(stats);

        mockMvc.perform(get("/dashboard/stats")
                        .param("dateDebut", "2026-01-01")
                        .param("dateFin", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dossiersOuverts.valeur").value(10))
                .andExpect(jsonPath("$.data.recettesTotalPeriode").value(100.00));
    }

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void stats_asAgent_returnsForbidden() throws Exception {
        mockMvc.perform(get("/dashboard/stats"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
        verifyNoInteractions(dashboardQueryService);
    }

    @Test
    @WithConsulosUser(role = "ROLE_ADMIN")
    void chartsAndAgentsCharge_asAdmin_returnOk() throws Exception {
        when(dashboardQueryService.getDossiersChart(ORG_ID, DashboardPeriode.MOIS))
                .thenReturn(List.of(new DossierChartDTO("Passeport", "SOUMIS", 2L)));
        when(dashboardQueryService.getRecettesChart(ORG_ID, DashboardPeriode.SEMAINE))
                .thenReturn(List.of(new RecetteChartPointDTO(LocalDate.of(2026, 1, 5), "ESPECES", BigDecimal.TEN)));
        when(dashboardQueryService.getAgentsCharge(ORG_ID))
                .thenReturn(List.of(new AgentChargeDTO(AGENT_ID, "Awa Diallo", 3L, 1L)));

        mockMvc.perform(get("/dashboard/dossiers-chart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].typeService").value("Passeport"));

        mockMvc.perform(get("/dashboard/recettes-chart").param("periode", "SEMAINE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].modePaiement").value("ESPECES"));

        mockMvc.perform(get("/dashboard/agents-charge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].agentNom").value("Awa Diallo"));
    }

    @Test
    @WithConsulosUser(role = "ROLE_CAISSIER")
    void recettesChart_asCaissier_returnsOk() throws Exception {
        when(dashboardQueryService.getRecettesChart(ORG_ID, DashboardPeriode.MOIS)).thenReturn(List.of());

        mockMvc.perform(get("/dashboard/recettes-chart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithConsulosUser(role = "ROLE_CAISSIER")
    void alerts_asCaissier_returnsForbidden() throws Exception {
        mockMvc.perform(get("/dashboard/alerts"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }
}
