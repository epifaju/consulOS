package com.consulos.auth;

import com.consulos.auth.dto.LoginRequestDTO;
import com.consulos.auth.dto.LoginResponseDTO;
import com.consulos.auth.dto.ResetPasswordDTO;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceAccountTest {

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

    private UserEntity user;
    private ConsulosUserDetails userDetails;

    @BeforeEach
    void setUp() {
        RoleEntity role = new RoleEntity();
        role.setCode("ROLE_AGENT");
        user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("agent@consulos.local");
        user.setPasswordHash("hash");
        user.setNom("Diallo");
        user.setPrenom("Awa");
        user.setOrganisationId(UUID.randomUUID());
        user.setRole(role);
        user.setActif(true);
        userDetails = new ConsulosUserDetails(
                user.getId(), user.getEmail(), user.getPasswordHash(),
                user.getOrganisationId(), "ROLE_AGENT", true);
    }

    @Test
    void login_whenEmailUnknown_shouldThrowBadCredentials() {
        when(userRepository.findByEmail("ghost@consulos.local")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(
                new LoginRequestDTO("ghost@consulos.local", "x"),
                mock(HttpServletResponse.class)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("BAD_CREDENTIALS");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void login_whenOrganisationMissing_shouldStillReturnToken() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Admin1234!", user.getPasswordHash())).thenReturn(true);
        when(userDetailsService.loadUserById(user.getId())).thenReturn(userDetails);
        when(jwtService.generateAccessToken(userDetails, user.getOrganisationId())).thenReturn("access");
        when(jwtService.generateRefreshToken()).thenReturn("refresh");
        when(organisationRepository.findById(user.getOrganisationId())).thenReturn(Optional.empty());
        when(userProfileMapper.toDto(eq(user), isNull())).thenReturn(new UserProfileDTO(
                user.getId(), user.getEmail(), user.getNom(), user.getPrenom(),
                "ROLE_AGENT", user.getOrganisationId(), null, null));

        LoginResponseDTO result = authService.login(
                new LoginRequestDTO(user.getEmail(), "Admin1234!"), response);

        assertThat(result.accessToken()).isEqualTo("access");
        assertThat(result.user().organisationId()).isEqualTo(user.getOrganisationId());
        verify(authCookieHelper).setRefreshCookies(response, user.getId(), "refresh");
    }

    @Test
    void getCurrentUser_whenMissing_shouldThrowNotFound() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser(userDetails))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("USER_NOT_FOUND"));
    }

    @Test
    void getCurrentUser_shouldMapProfile() {
        OrganisationEntity org = new OrganisationEntity();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(organisationRepository.findById(user.getOrganisationId())).thenReturn(Optional.of(org));
        when(userProfileMapper.toDto(user, org)).thenReturn(new UserProfileDTO(
                user.getId(), user.getEmail(), user.getNom(), user.getPrenom(),
                "ROLE_AGENT", user.getOrganisationId(), "Poste", "GNB-FR"));

        UserProfileDTO profile = authService.getCurrentUser(userDetails);

        assertThat(profile.email()).isEqualTo(user.getEmail());
        assertThat(profile.organisationCode()).isEqualTo("GNB-FR");
    }

    @Test
    void forgotPassword_whenTokenCreated_shouldSendMail() {
        when(passwordResetService.createResetToken(user.getEmail())).thenReturn(Optional.of("tok"));

        authService.forgotPassword(user.getEmail());

        verify(mailService).sendPasswordResetEmail(user.getEmail(), "tok");
    }

    @Test
    void forgotPassword_whenUnknown_shouldNotSendMail() {
        when(passwordResetService.createResetToken("x@y.z")).thenReturn(Optional.empty());

        authService.forgotPassword("x@y.z");

        verify(mailService, never()).sendPasswordResetEmail(any(), any());
    }

    @Test
    void resetPassword_whenTokenInvalid_shouldThrow() {
        ResetPasswordDTO dto = new ResetPasswordDTO("bad", "Admin1234!", "Admin1234!");
        when(passwordResetService.validateAndConsumeToken("bad")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword(dto))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo("INVALID_RESET_TOKEN"));
        verify(userRepository, never()).updatePassword(any(), any(), any());
    }

    @Test
    void resetPassword_whenTokenValid_shouldUpdateHash() {
        ResetPasswordDTO dto = new ResetPasswordDTO("ok", "Admin1234!", "Admin1234!");
        when(passwordResetService.validateAndConsumeToken("ok")).thenReturn(Optional.of(user.getId()));
        when(passwordEncoder.encode("Admin1234!")).thenReturn("new-hash");

        authService.resetPassword(dto);

        verify(userRepository).updatePassword(eq(user.getId()), eq("new-hash"), any());
    }
}
