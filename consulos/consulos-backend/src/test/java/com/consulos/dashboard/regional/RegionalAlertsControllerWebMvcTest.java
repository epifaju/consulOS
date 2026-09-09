package com.consulos.dashboard.regional;

import com.consulos.dashboard.regional.dto.RegionalAlerteDTO;
import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = RegionalAlertsController.class)
@TestPropertySource(properties = "consulos.reseau.enabled=true")
class RegionalAlertsControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RegionalAlertsService regionalAlertsService;

    @Test
    @WithConsulosUser(role = "ROLE_ADMIN_RESEAU")
    void alerts_asAdminReseau_returnsOk() throws Exception {
        RegionalAlerteDTO alerte = new RegionalAlerteDTO(
                "RETARD",
                "WARNING",
                "Dossiers en retard",
                2L,
                List.of("P1"),
                List.of(UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"))
        );
        when(regionalAlertsService.getAlertes(isNull())).thenReturn(List.of(alerte));

        mockMvc.perform(get("/regional/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].type").value("RETARD"))
                .andExpect(jsonPath("$.data[0].count").value(2));
    }

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void alerts_asAgent_returnsForbidden() throws Exception {
        mockMvc.perform(get("/regional/alerts"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }
}
