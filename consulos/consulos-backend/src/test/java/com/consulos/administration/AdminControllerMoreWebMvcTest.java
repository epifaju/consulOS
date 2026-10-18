package com.consulos.administration;

import com.consulos.administration.dto.OrganisationDTO;
import com.consulos.administration.dto.RoleDTO;
import com.consulos.administration.dto.UserAdminDTO;
import com.consulos.administration.dto.UserCreateResponseDTO;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = AdminController.class)
class AdminControllerMoreWebMvcTest {

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ROLE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID ORG_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CURRENT_USER = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminUserService adminUserService;
    @MockBean
    private AdminRoleService adminRoleService;
    @MockBean
    private OrganisationService organisationService;

    @Test
    @WithConsulosUser(role = "ROLE_ADMIN")
    void getUser_asAdmin_returnsOk() throws Exception {
        when(adminUserService.findById(eq(USER_ID), eq(ORG_ID))).thenReturn(sampleUser());

        mockMvc.perform(get("/admin/users/{id}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("agent@consulos.local"))
                .andExpect(jsonPath("$.data.roleCode").value("ROLE_AGENT"));
    }

    @Test
    @WithConsulosUser(role = "ROLE_ADMIN")
    void createUser_asAdmin_returnsCreated() throws Exception {
        when(adminUserService.create(any(), eq(ORG_ID), eq(CURRENT_USER)))
                .thenReturn(new UserCreateResponseDTO(sampleUser(), "Temp1234!"));

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"agent@consulos.local",
                                  "nom":"Diallo",
                                  "prenom":"Awa",
                                  "roleId":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                                  "actif":true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.temporaryPassword").value("Temp1234!"))
                .andExpect(jsonPath("$.data.user.id").value(USER_ID.toString()));
    }

    @Test
    @WithConsulosUser(role = "ROLE_SUPERVISEUR")
    void createUser_asSuperviseur_returnsForbidden() throws Exception {
        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"agent@consulos.local",
                                  "nom":"Diallo",
                                  "prenom":"Awa",
                                  "roleId":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
        verifyNoInteractions(adminUserService);
    }

    @Test
    @WithConsulosUser(role = "ROLE_ADMIN")
    void updateUser_toggle_reset_delete_asAdmin() throws Exception {
        when(adminUserService.update(eq(USER_ID), any(), eq(ORG_ID))).thenReturn(sampleUser());
        when(adminUserService.toggleActif(eq(USER_ID), eq(ORG_ID))).thenReturn(sampleUser());
        when(adminUserService.resetPassword(eq(USER_ID), eq(ORG_ID))).thenReturn("NewTemp1!");
        doNothing().when(adminUserService).delete(eq(USER_ID), eq(ORG_ID), eq(CURRENT_USER));

        mockMvc.perform(put("/admin/users/{id}", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prenom":"Awa","nom":"Diallo"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nom").value("Diallo"));

        mockMvc.perform(post("/admin/users/{id}/toggle-actif", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actif").value(true));

        mockMvc.perform(post("/admin/users/{id}/reset-password", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.temporaryPassword").value("NewTemp1!"));

        mockMvc.perform(delete("/admin/users/{id}", USER_ID))
                .andExpect(status().isNoContent());
        verify(adminUserService).delete(USER_ID, ORG_ID, CURRENT_USER);
    }

    @Test
    @WithConsulosUser(role = "ROLE_ADMIN")
    void rolesAndOrganisation_asAdmin_returnsOk() throws Exception {
        when(adminRoleService.list()).thenReturn(
                List.of(new RoleDTO(ROLE_ID, "ROLE_AGENT", "Agent", List.of("dossiers:write"))));
        OrganisationDTO org = new OrganisationDTO(ORG_ID, "GNB-FR", "Consulat", "GNB", "fr", "EUR", true);
        when(organisationService.get(ORG_ID)).thenReturn(org);
        when(organisationService.update(eq(ORG_ID), any())).thenReturn(org);

        mockMvc.perform(get("/admin/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("ROLE_AGENT"));

        mockMvc.perform(get("/admin/organisation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("GNB-FR"));

        mockMvc.perform(put("/admin/organisation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id":"22222222-2222-2222-2222-222222222222",
                                  "code":"GNB-FR",
                                  "nom":"Consulat",
                                  "paysCode":"GNB"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nom").value("Consulat"));
    }

    private static UserAdminDTO sampleUser() {
        return new UserAdminDTO(
                USER_ID, "agent@consulos.local", "Diallo", "Awa",
                "ROLE_AGENT", "Agent", true, null, null);
    }
}
