package com.consulos.auth;

import com.consulos.auth.dto.LoginRequestDTO;
import com.consulos.auth.dto.LoginResponseDTO;
import com.consulos.auth.dto.UserProfileDTO;
import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = AuthController.class)
class AuthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    void login_public_returnsOk() throws Exception {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID orgId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UserProfileDTO profile = new UserProfileDTO(
                userId, "admin@consulos.local", "Admin", "User",
                "ROLE_ADMIN", orgId, "Org", "ORG"
        );
        when(authService.login(any(LoginRequestDTO.class), any(HttpServletResponse.class)))
                .thenReturn(new LoginResponseDTO("access-token", profile));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@consulos.local","password":"Secret123!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.user.email").value("admin@consulos.local"));
    }

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void me_authenticated_returnsOk() throws Exception {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID orgId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UserProfileDTO profile = new UserProfileDTO(
                userId, "admin@consulos.local", "Agent", "User",
                "ROLE_AGENT", orgId, "Org", "ORG"
        );
        when(authService.getCurrentUser(any(ConsulosUserDetails.class))).thenReturn(profile);

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("ROLE_AGENT"))
                .andExpect(jsonPath("$.data.email").value("admin@consulos.local"));
    }
}
