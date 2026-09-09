package com.consulos.encaissement.export;

import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = EncaissementExportController.class)
class EncaissementExportControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EncaissementExcelExportService excelExportService;

    @Test
    @WithConsulosUser(role = "ROLE_CAISSIER")
    void exportExcel_asCaissier_returnsOkWithDisposition() throws Exception {
        LocalDate debut = LocalDate.of(2026, 9, 1);
        byte[] xlsx = new byte[]{0x50, 0x4B};
        when(excelExportService.exportJournal(eq(debut), eq(debut), any(UUID.class))).thenReturn(xlsx);

        mockMvc.perform(get("/encaissements/export/excel").param("dateDebut", "2026-09-01"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"journal-caisse-2026-09-01_2026-09-01.xlsx\""))
                .andExpect(content().bytes(xlsx));
    }

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void exportExcel_asAgent_returnsForbidden() throws Exception {
        mockMvc.perform(get("/encaissements/export/excel").param("dateDebut", "2026-09-01"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }
}
