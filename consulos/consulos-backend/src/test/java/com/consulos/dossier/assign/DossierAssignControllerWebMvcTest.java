package com.consulos.dossier.assign;

import com.consulos.dossier.DossierService;
import com.consulos.dossier.assign.dto.AgentOptionDTO;
import com.consulos.dossier.dto.DossierDetailDTO;
import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = DossierAssignController.class)
class DossierAssignControllerWebMvcTest {

    private static final UUID DOSSIER_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID AGENT_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DossierService dossierService;

    @MockBean
    private DossierAgentLookupService dossierAgentLookupService;

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void listAgents_asAgent_returnsOk() throws Exception {
        when(dossierAgentLookupService.listAssignableAgents(any(UUID.class)))
                .thenReturn(List.of(new AgentOptionDTO(
                        AGENT_ID, "Dupont", "Alice", "alice@consulos.local", "AGENT")));

        mockMvc.perform(get("/dossier-assign/agents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(AGENT_ID.toString()))
                .andExpect(jsonPath("$.data[0].email").value("alice@consulos.local"));
    }

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void assignAgent_asAgent_returnsOk() throws Exception {
        DossierDetailDTO detail = new DossierDetailDTO(
                DOSSIER_ID, "REF-1", null, null, null, null, null, null,
                null, AGENT_ID, "Dupont", null, null, null, null, null,
                null, null, List.of(), null, null
        );
        when(dossierService.assignerAgent(eq(DOSSIER_ID), eq(AGENT_ID), any(UUID.class)))
                .thenReturn(detail);

        mockMvc.perform(post("/dossiers/{id}/assign-agent", DOSSIER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"agentId":"%s"}
                                """.formatted(AGENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(DOSSIER_ID.toString()))
                .andExpect(jsonPath("$.data.agentId").value(AGENT_ID.toString()));
    }

    @Test
    @WithConsulosUser(role = "ROLE_CAISSIER")
    void listAgents_asCaissier_returnsForbidden() throws Exception {
        mockMvc.perform(get("/dossier-assign/agents"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }
}
