package com.consulos.dashboard.regional;

import com.consulos.common.ApiResponse;
import com.consulos.dashboard.regional.dto.RegionalAlerteDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/regional")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "consulos.reseau", name = "enabled", havingValue = "true")
@Tag(name = "15 — Réseau multi-postes", description = "Alertes réseau (Vague D)")
public class RegionalAlertsController {

    private final RegionalAlertsService regionalAlertsService;

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('ADMIN_RESEAU','ADMIN','SUPERVISEUR')")
    @Operation(summary = "Alertes agrégées multi-postes")
    public ApiResponse<List<RegionalAlerteDTO>> alerts(@RequestParam(required = false) UUID reseauId) {
        return ApiResponse.ok(regionalAlertsService.getAlertes(reseauId));
    }
}
