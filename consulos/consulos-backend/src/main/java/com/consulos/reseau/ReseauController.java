package com.consulos.reseau;

import com.consulos.common.ApiResponse;
import com.consulos.reseau.dto.ReseauMeDTO;
import com.consulos.reseau.dto.ReseauPosteDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/reseau")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "consulos.reseau", name = "enabled", havingValue = "true")
@Tag(name = "15 — Réseau multi-postes", description = "Périmètre mutualisé (Vague A)")
public class ReseauController {

    private final ReseauService reseauService;
    private final ReseauProperties properties;

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN_RESEAU','ADMIN','SUPERVISEUR')")
    @Operation(summary = "Statut module réseau")
    public ApiResponse<Map<String, Object>> status() {
        return ApiResponse.ok(Map.of("enabled", properties.isEnabled()));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN_RESEAU','ADMIN','SUPERVISEUR')")
    @Operation(summary = "Réseaux et postes de l'utilisateur courant")
    public ApiResponse<ReseauMeDTO> me() {
        UUID userId = ReseauSecurityUtils.getUserId();
        return ApiResponse.ok(reseauService.getMyReseaux(userId));
    }

    @GetMapping("/{reseauId}/postes")
    @PreAuthorize("hasAnyRole('ADMIN_RESEAU','ADMIN','SUPERVISEUR')")
    @Operation(summary = "Lister les postes d'un réseau (membre requis)")
    public ApiResponse<List<ReseauPosteDTO>> listPostes(@PathVariable UUID reseauId) {
        UUID userId = ReseauSecurityUtils.getUserId();
        return ApiResponse.ok(reseauService.listPostes(reseauId, userId));
    }
}
