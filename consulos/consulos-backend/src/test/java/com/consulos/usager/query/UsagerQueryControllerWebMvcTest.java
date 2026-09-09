package com.consulos.usager.query;

import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import com.consulos.usager.dto.DossierSummaryDTO;
import com.consulos.usager.query.dto.UsagerActivityItemDTO;
import com.consulos.usager.query.dto.UsagerDocumentSummaryDTO;
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

@ConsulosWebMvcTest(controllers = {UsagerQueryController.class, UsagerDocumentQueryController.class})
class UsagerQueryControllerWebMvcTest {

    private static final UUID USAGER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsagerDossierListQueryService dossierListQueryService;

    @MockBean
    private UsagerActivityQueryService activityQueryService;

    @MockBean
    private UsagerDocumentListQueryService usagerDocumentListQueryService;

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void dossiers_asAgent_returnsOk() throws Exception {
        when(dossierListQueryService.listDossiers(eq(USAGER_ID), any(UUID.class)))
                .thenReturn(List.<DossierSummaryDTO>of());

        mockMvc.perform(get("/usager-queries/{id}/dossiers", USAGER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void activity_asAgent_returnsOk() throws Exception {
        when(activityQueryService.listActivity(eq(USAGER_ID), any(UUID.class)))
                .thenReturn(List.<UsagerActivityItemDTO>of());

        mockMvc.perform(get("/usager-queries/{id}/activity", USAGER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void documents_asAgent_returnsOk() throws Exception {
        when(usagerDocumentListQueryService.listDocuments(eq(USAGER_ID), any(UUID.class)))
                .thenReturn(List.<UsagerDocumentSummaryDTO>of());

        mockMvc.perform(get("/usager-queries/{id}/documents", USAGER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithConsulosUser(role = "ROLE_CAISSIER")
    void dossiers_asCaissier_returnsForbidden() throws Exception {
        mockMvc.perform(get("/usager-queries/{id}/dossiers", USAGER_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    @WithConsulosUser(role = "ROLE_ACCUEIL")
    void activity_asAccueil_returnsForbidden() throws Exception {
        mockMvc.perform(get("/usager-queries/{id}/activity", USAGER_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }
}
