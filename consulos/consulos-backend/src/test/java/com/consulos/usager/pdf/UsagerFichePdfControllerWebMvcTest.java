package com.consulos.usager.pdf;

import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = UsagerFichePdfController.class)
class UsagerFichePdfControllerWebMvcTest {

    private static final UUID USAGER_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsagerFichePdfService fichePdfService;

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void exportFichePdf_asAgent_returnsPdf() throws Exception {
        byte[] pdf = new byte[]{0x25, 0x50, 0x44, 0x46};
        when(fichePdfService.generateFiche(eq(USAGER_ID), any(UUID.class))).thenReturn(pdf);
        when(fichePdfService.suggestedFilename(eq(USAGER_ID), any(UUID.class)))
                .thenReturn("fiche-usager.pdf");

        mockMvc.perform(get("/usagers/{id}/fiche/pdf", USAGER_ID))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"fiche-usager.pdf\""))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(content().bytes(pdf));
    }

    @Test
    @WithConsulosUser(role = "ROLE_CAISSIER")
    void exportFichePdf_asCaissier_returnsForbidden() throws Exception {
        mockMvc.perform(get("/usagers/{id}/fiche/pdf", USAGER_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }
}
