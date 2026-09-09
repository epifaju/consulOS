package com.consulos.dashboard.regional;

import com.consulos.dashboard.regional.dto.RegionalPosteKpiDTO;
import com.consulos.dashboard.regional.dto.RegionalStatsDTO;
import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = RegionalDashboardController.class)
@TestPropertySource(properties = "consulos.reseau.enabled=true")
class RegionalDashboardControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RegionalDashboardService regionalDashboardService;

    @Test
    @WithConsulosUser(role = "ROLE_ADMIN_RESEAU")
    void stats_asAdminReseau_returnsOk() throws Exception {
        RegionalStatsDTO stats = new RegionalStatsDTO(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                2,
                10L,
                3L,
                5L,
                1L,
                new BigDecimal("100.00"),
                new BigDecimal("500.00")
        );
        when(regionalDashboardService.getStats(isNull(), isNull(), isNull())).thenReturn(stats);

        mockMvc.perform(get("/regional/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.postesCount").value(2))
                .andExpect(jsonPath("$.data.dossiersOuverts").value(10));
    }

    @Test
    @WithConsulosUser(role = "ROLE_ADMIN_RESEAU")
    void postes_asAdminReseau_returnsOk() throws Exception {
        RegionalPosteKpiDTO poste = new RegionalPosteKpiDTO(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "P1",
                "Poste 1",
                4L,
                1L,
                2L,
                new BigDecimal("50.00")
        );
        when(regionalDashboardService.listPostesKpis(isNull())).thenReturn(List.of(poste));

        mockMvc.perform(get("/regional/postes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("P1"))
                .andExpect(jsonPath("$.data[0].nom").value("Poste 1"));
    }

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void stats_asAgent_returnsForbidden() throws Exception {
        mockMvc.perform(get("/regional/stats"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }
}
