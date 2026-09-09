package com.consulos.portal;

import com.consulos.portal.dto.PortalDossierSummaryDTO;
import com.consulos.portal.dto.PortalSessionResponseDTO;
import com.consulos.testsupport.ConsulosWebMvcTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = PortalController.class)
class PortalControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PortalAccessService portalAccessService;

    @MockBean
    private PortalDocumentService portalDocumentService;

    @MockBean
    private PortalRdvService portalRdvService;

    @Test
    void requestAccess_withValidJson_returnsOk() throws Exception {
        doNothing().when(portalAccessService).requestAccess(anyString(), anyString());

        mockMvc.perform(post("/portal/access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reference":"DOS-2024-001","email":"usager@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("accepted"));
    }

    @Test
    void openSession_withValidJson_returnsOk() throws Exception {
        PortalDossierSummaryDTO dossier = new PortalDossierSummaryDTO(
                UUID.randomUUID(),
                "DOS-2024-001",
                "EN_COURS",
                "Passeport",
                OffsetDateTime.parse("2024-01-15T10:00:00Z"),
                OffsetDateTime.parse("2024-02-15T10:00:00Z"),
                "Jean",
                "Dupont"
        );
        when(portalAccessService.openSession(anyString()))
                .thenReturn(new PortalSessionResponseDTO("session-token-1", dossier));

        mockMvc.perform(post("/portal/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"access-token-abc"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sessionToken").value("session-token-1"))
                .andExpect(jsonPath("$.data.dossier.reference").value("DOS-2024-001"));
    }
}
