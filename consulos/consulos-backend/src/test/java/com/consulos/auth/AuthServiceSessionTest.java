package com.consulos.auth;

import com.consulos.auth.dto.RefreshResponseDTO;
import com.consulos.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.consulos.auth.repository.OrganisationRepository;
import com.consulos.auth.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Couvre refresh / logout (hors périmètre de AuthServiceTest login).
 * Fichier séparé pour respecter la règle « nouveaux fichiers uniquement ».
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceSessionTest {

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

    private UUID userId;
    private ConsulosUserDetails userDetails;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userDetails = new ConsulosUserDetails(
                userId,
                "agent@consulos.local",
                "hash",
                UUID.randomUUID(),
                "ROLE_AGENT",
                true
        );
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
    }

    @Test
    void refresh_shouldRotateTokenAndReturnNewAccessToken() {
        when(authCookieHelper.getRefreshToken(request)).thenReturn(Optional.of("old-refresh"));
        when(authCookieHelper.getUserId(request)).thenReturn(Optional.of(userId));
        when(refreshTokenService.validateRefreshToken(userId, "old-refresh")).thenReturn(true);
        when(refreshTokenService.rotateRefreshToken(userId)).thenReturn("new-refresh");
        when(userDetailsService.loadUserById(userId)).thenReturn(userDetails);
        when(jwtService.generateAccessToken(userDetails, userDetails.getOrganisationId()))
                .thenReturn("new-access");

        RefreshResponseDTO result = authService.refresh(request, response);

        assertThat(result.accessToken()).isEqualTo("new-access");
        verify(authCookieHelper).setRefreshCookies(response, userId, "new-refresh");
    }

    @Test
    void refresh_whenRefreshCookieMissing_shouldThrowUnauthorized() {
        when(authCookieHelper.getRefreshToken(request)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(request, response))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("INVALID_REFRESH_TOKEN");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });
    }

    @Test
    void refresh_whenUserIdCookieMissing_shouldThrowUnauthorized() {
        when(authCookieHelper.getRefreshToken(request)).thenReturn(Optional.of("old-refresh"));
        when(authCookieHelper.getUserId(request)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(request, response))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void refresh_whenStoredTokenInvalid_shouldThrowUnauthorized() {
        when(authCookieHelper.getRefreshToken(request)).thenReturn(Optional.of("old-refresh"));
        when(authCookieHelper.getUserId(request)).thenReturn(Optional.of(userId));
        when(refreshTokenService.validateRefreshToken(userId, "old-refresh")).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh(request, response))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo("INVALID_REFRESH_TOKEN"));

        verify(refreshTokenService, never()).rotateRefreshToken(userId);
    }

    @Test
    void logout_shouldRevokeRefreshTokenAndClearCookies() {
        when(authCookieHelper.getUserId(request)).thenReturn(Optional.of(userId));

        authService.logout(request, response);

        verify(refreshTokenService).revokeRefreshToken(userId);
        verify(authCookieHelper).clearRefreshCookies(response);
    }

    @Test
    void logout_whenNoUserCookie_shouldStillClearCookies() {
        when(authCookieHelper.getUserId(request)).thenReturn(Optional.empty());

        authService.logout(request, response);

        verify(refreshTokenService, never()).revokeRefreshToken(userId);
        verify(authCookieHelper).clearRefreshCookies(response);
    }
}
