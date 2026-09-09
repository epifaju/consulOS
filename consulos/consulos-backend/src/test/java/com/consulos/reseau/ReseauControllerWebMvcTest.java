package com.consulos.reseau;

import com.consulos.reseau.dto.ReseauMeDTO;
import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = ReseauController.class)
@TestPropertySource(properties = "consulos.reseau.enabled=true")
class ReseauControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReseauService reseauService;

    @MockBean
    private ReseauProperties properties;

    @Test
    @WithConsulosUser(role = "ROLE_ADMIN_RESEAU")
    void status_asAdminReseau_returnsOk() throws Exception {
        when(properties.isEnabled()).thenReturn(true);

        mockMvc.perform(get("/reseau/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    @WithConsulosUser(role = "ROLE_ADMIN_RESEAU")
    void me_asAdminReseau_returnsOk() throws Exception {
        when(reseauService.getMyReseaux(any(UUID.class))).thenReturn(new ReseauMeDTO(List.of()));

        mockMvc.perform(get("/reseau/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reseaux").isArray());
    }

    @Test
    @WithConsulosUser(role = "ROLE_ADMIN_RESEAU")
    void listPostes_asAdminReseau_returnsOk() throws Exception {
        UUID reseauId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        when(reseauService.listPostes(eq(reseauId), any(UUID.class))).thenReturn(List.of());

        mockMvc.perform(get("/reseau/{id}/postes", reseauId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void status_asAgent_returnsForbidden() throws Exception {
        mockMvc.perform(get("/reseau/status"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }
}
