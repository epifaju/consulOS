package com.consulos.reseau;

import com.consulos.reseau.dto.ReseauAttachPosteRequestDTO;
import com.consulos.reseau.dto.ReseauPosteDTO;
import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = ReseauAdminController.class)
@TestPropertySource(properties = "consulos.reseau.enabled=true")
class ReseauAdminControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReseauAdminService reseauAdminService;

    @Test
    @WithConsulosUser(role = "ROLE_ADMIN_RESEAU")
    void listMembres_asAdminReseau_returnsOk() throws Exception {
        UUID reseauId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        when(reseauAdminService.listMembres(eq(reseauId), any(UUID.class))).thenReturn(List.of());

        mockMvc.perform(get("/reseau/{reseauId}/membres", reseauId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithConsulosUser(role = "ROLE_ADMIN_RESEAU")
    void attachPoste_asAdminReseau_returnsOk() throws Exception {
        UUID reseauId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID orgId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        ReseauPosteDTO poste = new ReseauPosteDTO(orgId, "GNB-FR", "Ambassade Paris", "FR", true);
        when(reseauAdminService.attachPoste(eq(reseauId), any(ReseauAttachPosteRequestDTO.class), any(UUID.class)))
                .thenReturn(poste);

        mockMvc.perform(post("/reseau/{reseauId}/postes", reseauId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"organisationCode":"GNB-FR"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("GNB-FR"));
    }

    @Test
    @WithConsulosUser(role = "ROLE_ADMIN_RESEAU")
    void detachPoste_asAdminReseau_returnsOk() throws Exception {
        UUID reseauId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID organisationId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

        mockMvc.perform(delete("/reseau/{reseauId}/postes/{organisationId}", reseauId, organisationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithConsulosUser(role = "ROLE_SUPERVISEUR")
    void attachPoste_asSuperviseur_returnsForbidden() throws Exception {
        UUID reseauId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        mockMvc.perform(post("/reseau/{reseauId}/postes", reseauId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"organisationCode":"GNB-FR"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void listMembres_asAgent_returnsForbidden() throws Exception {
        UUID reseauId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        mockMvc.perform(get("/reseau/{reseauId}/membres", reseauId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }
}
