package com.consulos.auth;

import com.consulos.auth.dto.ForgotPasswordDTO;
import com.consulos.auth.dto.LoginRequestDTO;
import com.consulos.auth.dto.LoginResponseDTO;
import com.consulos.auth.dto.RefreshResponseDTO;
import com.consulos.auth.dto.ResetPasswordDTO;
import com.consulos.auth.dto.UserProfileDTO;
import com.consulos.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto,
                                               HttpServletResponse response) {
        return ApiResponse.ok(authService.login(dto, response));
    }

    @PostMapping("/refresh")
    public ApiResponse<RefreshResponseDTO> refresh(HttpServletRequest request,
                                                   HttpServletResponse response) {
        return ApiResponse.ok(authService.refresh(request, response));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ApiResponse.ok(null, "Déconnexion réussie");
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordDTO dto) {
        authService.forgotPassword(dto.email());
        return ApiResponse.ok(null, "Si l'email existe, un lien de réinitialisation a été envoyé");
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        authService.resetPassword(dto);
        return ApiResponse.ok(null, "Mot de passe réinitialisé avec succès");
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserProfileDTO> me(@AuthenticationPrincipal ConsulosUserDetails principal) {
        return ApiResponse.ok(authService.getCurrentUser(principal));
    }
}
