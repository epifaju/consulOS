package com.consulos.reporting.regional;

import com.consulos.reporting.dto.EncaissementTotauxDTO;
import com.consulos.reporting.dto.RdvTotauxDTO;
import com.consulos.reporting.regional.dto.RegionalReportingMensuelDTO;
import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = RegionalReportingController.class)
@TestPropertySource(properties = "consulos.reseau.enabled=true")
class RegionalReportingControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RegionalReportingService regionalReportingService;

    @MockBean
    private RegionalReportingExcelExportService excelExportService;

    @Test
    @WithConsulosUser(role = "ROLE_ADMIN_RESEAU")
    void mensuel_asAdminReseau_returnsOk() throws Exception {
        RegionalReportingMensuelDTO dto = new RegionalReportingMensuelDTO(
                null, 0, 2024, 3,
                LocalDate.of(2024, 3, 1),
                LocalDate.of(2024, 3, 31),
                0L,
                List.of(),
                new EncaissementTotauxDTO(BigDecimal.ZERO, 0L, List.of(), List.of()),
                new RdvTotauxDTO(0L, List.of()),
                0L,
                BigDecimal.ZERO,
                List.of()
        );
        when(regionalReportingService.mensuel(anyInt(), anyInt(), isNull())).thenReturn(dto);

        mockMvc.perform(get("/regional/reporting/mensuel").param("annee", "2024").param("mois", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.annee").value(2024))
                .andExpect(jsonPath("$.data.mois").value(3));
    }

    @Test
    @WithConsulosUser(role = "ROLE_ADMIN")
    void mensuel_asAdmin_returnsOk() throws Exception {
        RegionalReportingMensuelDTO dto = new RegionalReportingMensuelDTO(
                null, 0, 2024, 3,
                LocalDate.of(2024, 3, 1),
                LocalDate.of(2024, 3, 31),
                0L,
                List.of(),
                new EncaissementTotauxDTO(BigDecimal.ZERO, 0L, List.of(), List.of()),
                new RdvTotauxDTO(0L, List.of()),
                0L,
                BigDecimal.ZERO,
                List.of()
        );
        when(regionalReportingService.mensuel(anyInt(), anyInt(), isNull())).thenReturn(dto);

        mockMvc.perform(get("/regional/reporting/mensuel").param("annee", "2024").param("mois", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void mensuel_asAgent_returnsForbidden() throws Exception {
        mockMvc.perform(get("/regional/reporting/mensuel"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }
}
