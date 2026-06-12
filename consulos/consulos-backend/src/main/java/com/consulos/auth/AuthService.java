package com.consulos.auth;

import com.consulos.auth.dto.LoginRequestDTO;
import com.consulos.auth.dto.LoginResponseDTO;
import com.consulos.auth.dto.RefreshResponseDTO;
import com.consulos.auth.dto.ResetPasswordDTO;
import com.consulos.auth.dto.UserProfileDTO;
import com.consulos.auth.entity.OrganisationEntity;
import com.consulos.auth.entity.UserEntity;
import com.consulos.auth.repository.OrganisationRepository;
import com.consulos.auth.repository.UserRepository;
import com.consulos.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OrganisationRepository organisationRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final UserProfileMapper userProfileMapper;
    private final AuthCookieHelper authCookieHelper;
    private final PasswordResetService passwordResetService;
    private final MailService mailService;
    private final UserDetailsServiceImpl userDetailsService;

    @Transactional
    public LoginResponseDTO login(LoginRequestDTO dto, HttpServletResponse response) {
        UserEntity user = userRepository.findByEmail(dto.email())
                .orElseThrow(() -> new BusinessException("BAD_CREDENTIALS", "Identifiants invalides", HttpStatus.UNAUTHORIZED));

        if (!Boolean.TRUE.equals(user.getActif())) {
            throw new BusinessException("USER_INACTIVE", "Compte utilisateur inactif", HttpStatus.FORBIDDEN);
        }

        if (!passwordEncoder.matches(dto.password(), user.getPasswordHash())) {
            throw new BusinessException("BAD_CREDENTIALS", "Identifiants invalides", HttpStatus.UNAUTHORIZED);
        }

        ConsulosUserDetails userDetails = userDetailsService.loadUserById(user.getId());
        String accessToken = jwtService.generateAccessToken(userDetails, user.getOrganisationId());
        String refreshToken = jwtService.generateRefreshToken();
        refreshTokenService.saveRefreshToken(user.getId(), refreshToken);
        authCookieHelper.setRefreshCookies(response, user.getId(), refreshToken);

        OffsetDateTime now = OffsetDateTime.now();
        userRepository.updateDerniereConnexion(user.getId(), now);

        OrganisationEntity organisation = organisationRepository.findById(user.getOrganisationId()).orElse(null);
        UserProfileDTO profile = userProfileMapper.toDto(user, organisation);

        return new LoginResponseDTO(accessToken, profile);
    }

    @Transactional(readOnly = true)
    public RefreshResponseDTO refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = authCookieHelper.getRefreshToken(request)
                .orElseThrow(() -> new BusinessException("INVALID_REFRESH_TOKEN", "Refresh token manquant", HttpStatus.UNAUTHORIZED));
        UUID userId = authCookieHelper.getUserId(request)
                .orElseThrow(() -> new BusinessException("INVALID_REFRESH_TOKEN", "Identifiant utilisateur manquant", HttpStatus.UNAUTHORIZED));

        if (!refreshTokenService.validateRefreshToken(userId, refreshToken)) {
            throw new BusinessException("INVALID_REFRESH_TOKEN", "Refresh token invalide", HttpStatus.UNAUTHORIZED);
        }

        String newRefreshToken = refreshTokenService.rotateRefreshToken(userId);
        authCookieHelper.setRefreshCookies(response, userId, newRefreshToken);

        ConsulosUserDetails userDetails = userDetailsService.loadUserById(userId);
        String accessToken = jwtService.generateAccessToken(userDetails, userDetails.getOrganisationId());

        return new RefreshResponseDTO(accessToken);
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        authCookieHelper.getUserId(request).ifPresent(refreshTokenService::revokeRefreshToken);
        authCookieHelper.clearRefreshCookies(response);
    }

    public void forgotPassword(String email) {
        passwordResetService.createResetToken(email)
                .ifPresent(token -> mailService.sendPasswordResetEmail(email, token));
    }

    @Transactional
    public void resetPassword(ResetPasswordDTO dto) {
        UUID userId = passwordResetService.validateAndConsumeToken(dto.token())
                .orElseThrow(() -> new BusinessException("INVALID_RESET_TOKEN", "Token de réinitialisation invalide ou expiré", HttpStatus.BAD_REQUEST));

        String encoded = passwordEncoder.encode(dto.newPassword());
        userRepository.updatePassword(userId, encoded, OffsetDateTime.now());
    }

    @Transactional(readOnly = true)
    public UserProfileDTO getCurrentUser(ConsulosUserDetails principal) {
        UserEntity user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Utilisateur introuvable", HttpStatus.NOT_FOUND));
        OrganisationEntity organisation = organisationRepository.findById(user.getOrganisationId()).orElse(null);
        return userProfileMapper.toDto(user, organisation);
    }
}
