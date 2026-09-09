package com.consulos.reporting;

import com.consulos.reporting.dto.EncaissementTotauxDTO;
import com.consulos.reporting.dto.ReportingMensuelDTO;
import com.consulos.reporting.dto.RdvTotauxDTO;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = ReportingController.class)
class ReportingControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportingService reportingService;

    @MockBean
    private ReportingExcelExportService excelExportService;

    @MockBean
    private ReportingCsvExportService csvExportService;

    @Test
    @WithConsulosUser(role = "ROLE_ADMIN")
    void mensuel_asAdmin_returnsOk() throws Exception {
        ReportingMensuelDTO dto = new ReportingMensuelDTO(
                2024, 3,
                LocalDate.of(2024, 3, 1),
                LocalDate.of(2024, 3, 31),
                0L,
                List.of(),
                new EncaissementTotauxDTO(BigDecimal.ZERO, 0L, List.of(), List.of()),
                new RdvTotauxDTO(0L, List.of()),
                0L,
                BigDecimal.ZERO
        );
        when(reportingService.mensuel(anyInt(), anyInt(), any(UUID.class))).thenReturn(dto);

        mockMvc.perform(get("/reporting/mensuel").param("annee", "2024").param("mois", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.annee").value(2024))
                .andExpect(jsonPath("$.data.mois").value(3));
    }

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void mensuel_asAgent_returnsForbidden() throws Exception {
        mockMvc.perform(get("/reporting/mensuel"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    @WithConsulosUser(role = "ROLE_ADMIN")
    void mensuelExcel_asAdmin_returnsAttachmentHeaders() throws Exception {
        byte[] xlsx = new byte[]{0x50, 0x4B};
        when(excelExportService.exportMensuel(anyInt(), anyInt(), any(UUID.class))).thenReturn(xlsx);

        mockMvc.perform(get("/reporting/mensuel/excel").param("annee", "2024").param("mois", "3"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition",
                        "attachment; filename=\"reporting-mensuel-2024-03.xlsx\""))
                .andExpect(header().string(
                        "Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }
}
