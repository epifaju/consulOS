package com.consulos.usager;

import com.consulos.common.PageResponse;
import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import com.consulos.usager.dto.UsagerSummaryDTO;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = UsagerController.class)
class UsagerControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsagerService usagerService;

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void list_asAgent_returnsOkPage() throws Exception {
        PageResponse<UsagerSummaryDTO> page = new PageResponse<>(
                List.of(), 0, 0, 0, 20, true, true);
        when(usagerService.search(isNull(), any(UUID.class), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/usagers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void search_asAgent_returnsOk() throws Exception {
        when(usagerService.searchQuick(eq("dup"), any(UUID.class))).thenReturn(List.of());

        mockMvc.perform(get("/usagers/search").param("q", "dup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void delete_asAgent_returnsForbidden() throws Exception {
        UUID id = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

        mockMvc.perform(delete("/usagers/{id}", id))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }
}
