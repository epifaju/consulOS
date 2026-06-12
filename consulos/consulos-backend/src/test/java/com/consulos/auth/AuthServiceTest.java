package com.consulos.auth;

import com.consulos.auth.dto.LoginRequestDTO;
import com.consulos.auth.dto.LoginResponseDTO;
import com.consulos.auth.dto.UserProfileDTO;
import com.consulos.auth.entity.OrganisationEntity;
import com.consulos.auth.entity.RoleEntity;
import com.consulos.auth.entity.UserEntity;
import com.consulos.auth.repository.OrganisationRepository;
import com.consulos.auth.repository.UserRepository;
import com.consulos.common.BusinessException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private OrganisationRepository organisationRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserProfileMapper userProfileMapper;
    @Mock
    private AuthCookieHelper authCookieHelper;
    @Mock
    private PasswordResetService passwordResetService;
    @Mock
    private MailService mailService;
    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @InjectMocks
    private AuthService authService;

    private UserEntity activeUser;
    private ConsulosUserDetails userDetails;

    @BeforeEach
    void setUp() {
        RoleEntity role = new RoleEntity();
        role.setCode("ROLE_ADMIN");

        activeUser = new UserEntity();
        activeUser.setId(UUID.randomUUID());
        activeUser.setEmail("admin@consulos.local");
        activeUser.setPasswordHash("encoded");
        activeUser.setNom("Admin");
        activeUser.setPrenom("Système");
        activeUser.setOrganisationId(UUID.randomUUID());
        activeUser.setRole(role);
        activeUser.setActif(true);

        userDetails = new ConsulosUserDetails(
                activeUser.getId(),
                activeUser.getEmail(),
                activeUser.getPasswordHash(),
                activeUser.getOrganisationId(),
                "ROLE_ADMIN",
                true
        );
    }

    @Test
    void login_shouldReturnLoginResponseWhenCredentialsValid() {
        LoginRequestDTO request = new LoginRequestDTO("admin@consulos.local", "Admin1234!");
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(request.password(), activeUser.getPasswordHash())).thenReturn(true);
        when(userDetailsService.loadUserById(activeUser.getId())).thenReturn(userDetails);
        when(jwtService.generateAccessToken(userDetails, activeUser.getOrganisationId())).thenReturn("access-token");
        when(jwtService.generateRefreshToken()).thenReturn("refresh-token");
        when(organisationRepository.findById(activeUser.getOrganisationId())).thenReturn(Optional.of(new OrganisationEntity()));
        when(userProfileMapper.toDto(any(UserEntity.class), any(OrganisationEntity.class)))
                .thenReturn(new UserProfileDTO(
                        activeUser.getId(),
                        activeUser.getEmail(),
                        activeUser.getNom(),
                        activeUser.getPrenom(),
                        "ROLE_ADMIN",
                        activeUser.getOrganisationId(),
                        "Organisation",
                        "GNB-FR"
                ));

        LoginResponseDTO result = authService.login(request, response);

        assertThat(result.accessToken()).isEqualTo("access-token");
        verify(refreshTokenService).saveRefreshToken(eq(activeUser.getId()), eq("refresh-token"));
        verify(authCookieHelper).setRefreshCookies(eq(response), eq(activeUser.getId()), eq("refresh-token"));
    }

    @Test
    void login_shouldThrowWhenUserInactive() {
        activeUser.setActif(false);
        when(userRepository.findByEmail("admin@consulos.local")).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> authService.login(
                new LoginRequestDTO("admin@consulos.local", "Admin1234!"),
                mock(HttpServletResponse.class)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("USER_INACTIVE");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }

    @Test
    void login_shouldThrowWhenPasswordInvalid() {
        when(userRepository.findByEmail("admin@consulos.local")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(
                new LoginRequestDTO("admin@consulos.local", "wrong"),
                mock(HttpServletResponse.class)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("BAD_CREDENTIALS");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });
    }
}
