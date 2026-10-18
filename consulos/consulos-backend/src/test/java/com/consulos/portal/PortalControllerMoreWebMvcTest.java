package com.consulos.portal;

import com.consulos.portal.dto.PortalDossierSummaryDTO;
import com.consulos.testsupport.ConsulosWebMvcTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = PortalController.class)
class PortalControllerMoreWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PortalAccessService portalAccessService;
    @MockBean
    private PortalDocumentService portalDocumentService;
    @MockBean
    private PortalRdvService portalRdvService;

    @AfterEach
    void tearDown() {
        PortalSessionHolder.clear();
    }

    @Test
    void dossier_withoutSession_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/portal/dossier"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("PORTAL_SESSION_REQUIRED"));
        verifyNoInteractions(portalAccessService);
    }

    @Test
    void dossier_withSession_returnsOk() throws Exception {
        PortalSession session = new PortalSession(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "DOS-2024-001");
        PortalSessionHolder.set(session);
        when(portalAccessService.getDossierSummary(any(PortalSession.class))).thenReturn(
                new PortalDossierSummaryDTO(
                        session.dossierId(),
                        "DOS-2024-001",
                        "EN_REVUE",
                        "Passeport",
                        OffsetDateTime.parse("2024-01-15T10:00:00Z"),
                        OffsetDateTime.parse("2024-02-15T10:00:00Z"),
                        "Mamadou",
                        "Camara"
                ));

        mockMvc.perform(get("/portal/dossier"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reference").value("DOS-2024-001"))
                .andExpect(jsonPath("$.data.usagerNom").value("Camara"));
    }
}
