package com.consulos.rendezvous;

import com.consulos.rendezvous.dto.RdvDetailDTO;
import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = RdvController.class)
class RdvControllerMoreWebMvcTest {

    private static final UUID RDV_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ORG_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RdvService rdvService;
    @MockBean
    private DisponibiliteService disponibiliteService;

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void getById_asAgent_returnsOk() throws Exception {
        when(rdvService.findById(eq(RDV_ID), eq(ORG_ID))).thenReturn(sampleDetail());

        mockMvc.perform(get("/rendezvous/{id}", RDV_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(RDV_ID.toString()))
                .andExpect(jsonPath("$.data.statut").value("CONFIRME"));
    }

    @Test
    @WithConsulosUser(role = "ROLE_SUPERVISEUR")
    void create_asSuperviseur_returnsForbidden() throws Exception {
        mockMvc.perform(post("/rendezvous")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usagerId":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                                  "dateHeure":"2026-03-15T10:00:00Z",
                                  "dureeMinutes":30
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
        verifyNoInteractions(rdvService);
    }

    private static RdvDetailDTO sampleDetail() {
        return new RdvDetailDTO(
                RDV_ID, null, "Camara", "Mamadou", null, null, "DOS-1",
                null, "Passeport", null, "Awa",
                OffsetDateTime.parse("2026-03-15T10:00:00Z"), 30,
                RdvStatut.CONFIRME, null, null, false, null, null);
    }
}
