package com.consulos.document;

import com.consulos.document.dto.DocumentDTO;
import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = DossierDocumentController.class)
class DossierDocumentControllerWebMvcTest {

    private static final UUID DOSSIER_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService documentService;

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void listByDossier_asAgent_returnsOk() throws Exception {
        DocumentDTO dto = new DocumentDTO(
                UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
                DOSSIER_ID,
                null,
                "file.pdf",
                "file.pdf",
                "PASSEPORT",
                "application/pdf",
                100L,
                null,
                1,
                null,
                null
        );
        when(documentService.listByDossier(eq(DOSSIER_ID), any(UUID.class))).thenReturn(List.of(dto));

        mockMvc.perform(get("/dossiers/{id}/documents", DOSSIER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].nomFichier").value("file.pdf"));
    }

    @Test
    @WithConsulosUser(role = "ROLE_ACCUEIL")
    void listByDossier_asAccueil_returnsForbidden() throws Exception {
        mockMvc.perform(get("/dossiers/{id}/documents", DOSSIER_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }
}
