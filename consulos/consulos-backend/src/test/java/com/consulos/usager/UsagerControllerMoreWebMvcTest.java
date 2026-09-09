package com.consulos.usager;

import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import com.consulos.usager.dto.DossierSummaryDTO;
import com.consulos.usager.dto.DoublonGroupDTO;
import com.consulos.usager.dto.UsagerCreateResponseDTO;
import com.consulos.usager.dto.UsagerDetailDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = UsagerController.class)
class UsagerControllerMoreWebMvcTest {

    private static final UUID USAGER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ORG_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsagerService usagerService;

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void create_asAgent_returnsCreated() throws Exception {
        UsagerDetailDTO detail = sampleDetail();
        when(usagerService.create(any(), any(UUID.class)))
                .thenReturn(new UsagerCreateResponseDTO(detail, List.of()));

        mockMvc.perform(post("/usagers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nom":"Camara",
                                  "prenom":"Mamadou",
                                  "nationalite":"GNB",
                                  "typeUsager":"DIASPORA"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.usager.id").value(USAGER_ID.toString()))
                .andExpect(jsonPath("$.data.usager.nom").value("Camara"))
                .andExpect(jsonPath("$.data.doublonsPotentiels").isArray());
    }

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void getById_asAgent_returnsOk() throws Exception {
        when(usagerService.findById(eq(USAGER_ID), any(UUID.class))).thenReturn(sampleDetail());

        mockMvc.perform(get("/usagers/{id}", USAGER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(USAGER_ID.toString()))
                .andExpect(jsonPath("$.data.prenom").value("Mamadou"));
    }

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void update_asAgent_returnsOk() throws Exception {
        when(usagerService.update(eq(USAGER_ID), any(), any(UUID.class))).thenReturn(sampleDetail());

        mockMvc.perform(put("/usagers/{id}", USAGER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"nouveau@test.local"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(USAGER_ID.toString()));
    }

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void doublons_asAgent_returnsOk() throws Exception {
        when(usagerService.findDoublons(
                eq("Camara"),
                eq(LocalDate.of(1990, 1, 1)),
                eq("GNB"),
                any(UUID.class),
                isNull()
        )).thenReturn(List.<DoublonGroupDTO>of());

        mockMvc.perform(get("/usagers/doublons")
                        .param("nom", "Camara")
                        .param("dateNaissance", "1990-01-01")
                        .param("nationalite", "GNB"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void dossiers_asAgent_returnsOk() throws Exception {
        when(usagerService.getHistorique(eq(USAGER_ID), any(UUID.class)))
                .thenReturn(List.<DossierSummaryDTO>of());

        mockMvc.perform(get("/usagers/{id}/dossiers", USAGER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithConsulosUser(role = "ROLE_CAISSIER")
    void create_asCaissier_returnsForbidden() throws Exception {
        mockMvc.perform(post("/usagers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nom":"Camara",
                                  "prenom":"Mamadou"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    @WithConsulosUser(role = "ROLE_ADMIN")
    void delete_asAdmin_returnsNoContent() throws Exception {
        doNothing().when(usagerService).softDelete(eq(USAGER_ID), any(UUID.class));

        mockMvc.perform(delete("/usagers/{id}", USAGER_ID))
                .andExpect(status().isNoContent());

        verify(usagerService).softDelete(eq(USAGER_ID), any(UUID.class));
    }

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void create_withBlankNom_returnsValidationError() throws Exception {
        mockMvc.perform(post("/usagers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nom":"",
                                  "prenom":"Mamadou"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    private static UsagerDetailDTO sampleDetail() {
        return new UsagerDetailDTO(
                USAGER_ID,
                "GNB-2026-00001",
                "Camara",
                "Mamadou",
                LocalDate.of(1990, 1, 1),
                null,
                "GNB",
                TypeUsager.DIASPORA,
                null,
                "m@test.local",
                null,
                null,
                null,
                Map.of(),
                true,
                ORG_ID,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
