package com.consulos.reporting;

import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = ReportingController.class)
class ReportingControllerMoreWebMvcTest {

    private static final UUID ORG_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportingService reportingService;
    @MockBean
    private ReportingExcelExportService excelExportService;
    @MockBean
    private ReportingCsvExportService csvExportService;

    @Test
    @WithConsulosUser(role = "ROLE_SUPERVISEUR")
    void dossiersCsv_asSuperviseur_returnsAttachment() throws Exception {
        byte[] csv = "reference,statut\nDOS-1,SOUMIS\n".getBytes();
        when(csvExportService.exportDossiersCsv(eq(2024), eq(3), eq(ORG_ID))).thenReturn(csv);

        mockMvc.perform(get("/reporting/dossiers/csv").param("annee", "2024").param("mois", "3"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"dossiers-2024-03.csv\""))
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(content().bytes(csv));
    }

    @Test
    @WithConsulosUser(role = "ROLE_ADMIN")
    void rdvCsv_asAdmin_returnsAttachment() throws Exception {
        byte[] csv = "id,statut\n".getBytes();
        when(csvExportService.exportRdvCsv(anyInt(), anyInt(), any(UUID.class))).thenReturn(csv);

        mockMvc.perform(get("/reporting/rdv/csv").param("annee", "2024").param("mois", "3"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"rdv-2024-03.csv\""))
                .andExpect(content().bytes(csv));
    }

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void mensuelExcel_asAgent_returnsForbidden() throws Exception {
        mockMvc.perform(get("/reporting/mensuel/excel"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
        verifyNoInteractions(excelExportService);
    }

    @Test
    @WithConsulosUser(role = "ROLE_CAISSIER")
    void dossiersCsv_asCaissier_returnsForbidden() throws Exception {
        mockMvc.perform(get("/reporting/dossiers/csv"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
        verifyNoInteractions(csvExportService);
    }
}
