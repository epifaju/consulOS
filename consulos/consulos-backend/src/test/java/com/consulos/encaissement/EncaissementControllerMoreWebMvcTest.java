package com.consulos.encaissement;

import com.consulos.encaissement.dto.EncaissementDetailDTO;
import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = EncaissementController.class)
class EncaissementControllerMoreWebMvcTest {

    private static final UUID ENC_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID ORG_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EncaissementService encaissementService;

    @Test
    @WithConsulosUser(role = "ROLE_CAISSIER")
    void getById_asCaissier_returnsOk() throws Exception {
        when(encaissementService.findById(eq(ENC_ID), eq(ORG_ID))).thenReturn(sampleDetail());

        mockMvc.perform(get("/encaissements/{id}", ENC_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.numeroRecu").value("REC-2024-00001"))
                .andExpect(jsonPath("$.data.statut").value("DU"));
    }

    @Test
    @WithConsulosUser(role = "ROLE_CAISSIER")
    void recuPdf_asCaissier_returnsPdf() throws Exception {
        byte[] pdf = new byte[]{0x25, 0x50, 0x44, 0x46};
        when(encaissementService.getRecu(eq(ENC_ID), eq(ORG_ID))).thenReturn(pdf);

        mockMvc.perform(get("/encaissements/{id}/recu/pdf", ENC_ID))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        "inline; filename=\"recu-" + ENC_ID + ".pdf\""));
    }

    @Test
    @WithConsulosUser(role = "ROLE_ACCUEIL")
    void getById_asAccueil_returnsForbidden() throws Exception {
        mockMvc.perform(get("/encaissements/{id}", ENC_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
        verifyNoInteractions(encaissementService);
    }

    private static EncaissementDetailDTO sampleDetail() {
        return new EncaissementDetailDTO(
                ENC_ID, "REC-2024-00001",
                null, "Camara", "Mamadou", "GNB-1",
                null, "DOS-1", "Passeport",
                new BigDecimal("50.00"), BigDecimal.ZERO, "EUR",
                ModePaiement.ESPECES, StatutEncaissement.DU,
                null, null, null, null, null, null);
    }
}
