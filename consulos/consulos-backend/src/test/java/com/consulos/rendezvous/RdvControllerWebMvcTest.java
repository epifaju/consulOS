package com.consulos.rendezvous;

import com.consulos.common.PageResponse;
import com.consulos.rendezvous.dto.CreneauDTO;
import com.consulos.rendezvous.dto.RdvSummaryDTO;
import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = RdvController.class)
class RdvControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RdvService rdvService;

    @MockBean
    private DisponibiliteService disponibiliteService;

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void list_asAgent_returnsOkPage() throws Exception {
        PageResponse<RdvSummaryDTO> page = new PageResponse<>(
                List.of(), 0, 0, 0, 20, true, true);
        when(rdvService.list(
                any(UUID.class),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(page);

        mockMvc.perform(get("/rendezvous"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @WithConsulosUser(role = "ROLE_ACCUEIL")
    void disponibilites_asAccueil_returnsOk() throws Exception {
        LocalDate date = LocalDate.of(2024, 3, 15);
        when(disponibiliteService.getDisponibilites(any(UUID.class), isNull(), eq(date)))
                .thenReturn(List.<CreneauDTO>of());

        mockMvc.perform(get("/rendezvous/disponibilites").param("date", "2024-03-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithConsulosUser(role = "ROLE_CAISSIER")
    void list_asCaissier_returnsForbidden() throws Exception {
        mockMvc.perform(get("/rendezvous"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }
}
