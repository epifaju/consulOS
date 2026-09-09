package com.consulos.encaissement;

import com.consulos.encaissement.dto.TarifDTO;
import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = TarifController.class)
class TarifControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TarifService tarifService;

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void list_asAgent_returnsOkEmpty() throws Exception {
        when(tarifService.list(any(UUID.class))).thenReturn(List.of());

        mockMvc.perform(get("/tarifs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @WithConsulosUser(role = "ROLE_ADMIN")
    void create_asAdmin_returnsCreated() throws Exception {
        UUID typeServiceId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        UUID createdId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        TarifDTO created = new TarifDTO(
                createdId, typeServiceId, "Passeport", new BigDecimal("50.00"), "EUR", true);
        when(tarifService.create(any(TarifDTO.class), any(UUID.class))).thenReturn(created);

        mockMvc.perform(post("/tarifs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id":null,
                                  "typeServiceId":"cccccccc-cccc-cccc-cccc-cccccccccccc",
                                  "libelle":"Passeport",
                                  "montant":50.00,
                                  "devise":"EUR",
                                  "actif":true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.libelle").value("Passeport"));
    }

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void create_asAgent_returnsForbidden() throws Exception {
        mockMvc.perform(post("/tarifs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id":null,
                                  "typeServiceId":"cccccccc-cccc-cccc-cccc-cccccccccccc",
                                  "libelle":"Passeport",
                                  "montant":50.00,
                                  "devise":"EUR",
                                  "actif":true
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    @WithConsulosUser(role = "ROLE_ADMIN")
    void update_asAdmin_returnsOk() throws Exception {
        UUID id = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        UUID typeServiceId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        TarifDTO updated = new TarifDTO(
                id, typeServiceId, "Passeport maj", new BigDecimal("55.00"), "EUR", true);
        when(tarifService.update(eq(id), any(TarifDTO.class), any(UUID.class))).thenReturn(updated);

        mockMvc.perform(put("/tarifs/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id":"dddddddd-dddd-dddd-dddd-dddddddddddd",
                                  "typeServiceId":"cccccccc-cccc-cccc-cccc-cccccccccccc",
                                  "libelle":"Passeport maj",
                                  "montant":55.00,
                                  "devise":"EUR",
                                  "actif":true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.libelle").value("Passeport maj"));
    }
}
