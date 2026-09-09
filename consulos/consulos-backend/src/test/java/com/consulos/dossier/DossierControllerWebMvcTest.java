package com.consulos.dossier;

import com.consulos.common.PageResponse;
import com.consulos.dossier.dto.DossierDetailDTO;
import com.consulos.dossier.dto.DossierSummaryDTO;
import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = DossierController.class)
class DossierControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DossierService dossierService;

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void list_asAgent_returnsOkPage() throws Exception {
        PageResponse<DossierSummaryDTO> page = new PageResponse<>(
                List.of(), 0, 0, 0, 20, true, true);
        when(dossierService.list(
                any(UUID.class),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(page);

        mockMvc.perform(get("/dossiers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void findById_asAgent_returnsOk() throws Exception {
        UUID id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        DossierDetailDTO detail = new DossierDetailDTO(
                id, "DOS-1", null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, List.of(), null, null
        );
        when(dossierService.findById(eq(id), any(UUID.class))).thenReturn(detail);

        mockMvc.perform(get("/dossiers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.reference").value("DOS-1"));
    }

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void stats_asAgent_returnsForbidden() throws Exception {
        mockMvc.perform(get("/dossiers/stats"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }
}
