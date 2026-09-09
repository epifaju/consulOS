package com.consulos.encaissement;

import com.consulos.common.PageResponse;
import com.consulos.encaissement.dto.EncaissementSummaryDTO;
import com.consulos.encaissement.dto.JournalCaisseDTO;
import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = EncaissementController.class)
class EncaissementControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EncaissementService encaissementService;

    @Test
    @WithConsulosUser(role = "ROLE_CAISSIER")
    void list_asCaissier_returnsOkPage() throws Exception {
        PageResponse<EncaissementSummaryDTO> page = new PageResponse<>(
                List.of(), 0, 0, 0, 20, true, true);
        when(encaissementService.list(
                any(UUID.class),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(page);

        mockMvc.perform(get("/encaissements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @WithConsulosUser(role = "ROLE_CAISSIER")
    void journalCaisse_asCaissier_returnsOk() throws Exception {
        LocalDate date = LocalDate.of(2024, 3, 15);
        when(encaissementService.getJournalCaisse(eq(date), any(UUID.class)))
                .thenReturn(new JournalCaisseDTO(date, List.of(), 0L, BigDecimal.ZERO));

        mockMvc.perform(get("/encaissements/journal-caisse").param("date", "2024-03-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nombreTransactions").value(0));
    }

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void list_asAgent_returnsForbidden() throws Exception {
        mockMvc.perform(get("/encaissements"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void paiement_asAgent_returnsForbidden() throws Exception {
        UUID id = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

        mockMvc.perform(post("/encaissements/{id}/paiement", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montant":10.00,"modePaiement":"ESPECES","notes":null}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }
}
