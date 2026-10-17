package com.consulos.dossier.checklist;

import com.consulos.dossier.checklist.dto.DossierChecklistItemDTO;
import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = DossierChecklistController.class)
class DossierChecklistControllerWebMvcTest {

    private static final UUID DOSSIER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ORG_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DossierChecklistService checklistService;

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void list_asAgent_returnsOk() throws Exception {
        when(checklistService.list(DOSSIER_ID, ORG_ID)).thenReturn(
                List.of(new DossierChecklistItemDTO("PASSEPORT", "Passeport", false)));

        mockMvc.perform(get("/dossiers/{id}/checklist", DOSSIER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("PASSEPORT"))
                .andExpect(jsonPath("$.data[0].valide").value(false));
    }

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void update_asAgent_returnsOk() throws Exception {
        when(checklistService.update(eq(DOSSIER_ID), eq("PASSEPORT"), eq(true), eq(ORG_ID)))
                .thenReturn(List.of(new DossierChecklistItemDTO("PASSEPORT", "Passeport", true)));

        mockMvc.perform(put("/dossiers/{id}/checklist/{code}", DOSSIER_ID, "PASSEPORT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valide\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].valide").value(true));

        verify(checklistService).update(DOSSIER_ID, "PASSEPORT", true, ORG_ID);
    }

    @Test
    @WithConsulosUser(role = "ROLE_CAISSIER")
    void update_asCaissier_returnsForbidden() throws Exception {
        mockMvc.perform(put("/dossiers/{id}/checklist/{code}", DOSSIER_ID, "PASSEPORT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valide\":true}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));

        verifyNoInteractions(checklistService);
    }
}
