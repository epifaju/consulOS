package com.consulos.reseau;

import com.consulos.common.ApiResponse;
import com.consulos.reseau.dto.ReseauAttachPosteRequestDTO;
import com.consulos.reseau.dto.ReseauMembreDTO;
import com.consulos.reseau.dto.ReseauPosteDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/reseau")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "consulos.reseau", name = "enabled", havingValue = "true")
@Tag(name = "15 — Réseau multi-postes", description = "Admin réseau (Vague D)")
public class ReseauAdminController {

    private final ReseauAdminService reseauAdminService;

    @GetMapping("/{reseauId}/membres")
    @PreAuthorize("hasAnyRole('ADMIN_RESEAU','ADMIN','SUPERVISEUR')")
    @Operation(summary = "Lister les membres d'un réseau")
    public ApiResponse<List<ReseauMembreDTO>> listMembres(@PathVariable UUID reseauId) {
        UUID userId = ReseauSecurityUtils.getUserId();
        return ApiResponse.ok(reseauAdminService.listMembres(reseauId, userId));
    }

    @PostMapping("/{reseauId}/postes")
    @PreAuthorize("hasAnyRole('ADMIN_RESEAU','ADMIN')")
    @Operation(summary = "Rattacher un poste (code organisation)")
    public ApiResponse<ReseauPosteDTO> attachPoste(
            @PathVariable UUID reseauId,
            @Valid @RequestBody ReseauAttachPosteRequestDTO request
    ) {
        UUID userId = ReseauSecurityUtils.getUserId();
        return ApiResponse.ok(reseauAdminService.attachPoste(reseauId, request, userId));
    }

    @DeleteMapping("/{reseauId}/postes/{organisationId}")
    @PreAuthorize("hasAnyRole('ADMIN_RESEAU','ADMIN')")
    @Operation(summary = "Détacher un poste du réseau")
    public ApiResponse<Void> detachPoste(@PathVariable UUID reseauId, @PathVariable UUID organisationId) {
        UUID userId = ReseauSecurityUtils.getUserId();
        reseauAdminService.detachPoste(reseauId, organisationId, userId);
        return ApiResponse.ok(null, "Poste détaché");
    }
}
