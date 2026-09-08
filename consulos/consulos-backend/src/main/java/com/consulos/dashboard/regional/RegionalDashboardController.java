package com.consulos.dashboard.regional;

import com.consulos.common.ApiResponse;
import com.consulos.dashboard.regional.dto.RegionalPosteKpiDTO;
import com.consulos.dashboard.regional.dto.RegionalStatsDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/regional")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "consulos.reseau", name = "enabled", havingValue = "true")
@Tag(name = "15 — Réseau multi-postes", description = "Tableau de bord régional (Vague B)")
public class RegionalDashboardController {

    private final RegionalDashboardService regionalDashboardService;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN_RESEAU','ADMIN','SUPERVISEUR')")
    @Operation(summary = "KPIs agrégés du périmètre réseau")
    public ApiResponse<RegionalStatsDTO> stats(
            @RequestParam(required = false) UUID reseauId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin
    ) {
        return ApiResponse.ok(regionalDashboardService.getStats(reseauId, dateDebut, dateFin));
    }

    @GetMapping("/postes")
    @PreAuthorize("hasAnyRole('ADMIN_RESEAU','ADMIN','SUPERVISEUR')")
    @Operation(summary = "KPIs par poste du périmètre")
    public ApiResponse<List<RegionalPosteKpiDTO>> postes(@RequestParam(required = false) UUID reseauId) {
        return ApiResponse.ok(regionalDashboardService.listPostesKpis(reseauId));
    }

    @GetMapping("/postes/{organisationId}/stats")
    @PreAuthorize("hasAnyRole('ADMIN_RESEAU','ADMIN','SUPERVISEUR')")
    @Operation(summary = "Détail KPI d'un poste (404 si hors périmètre)")
    public ApiResponse<RegionalPosteKpiDTO> posteStats(
            @PathVariable UUID organisationId,
            @RequestParam(required = false) UUID reseauId
    ) {
        return ApiResponse.ok(regionalDashboardService.getPosteStats(organisationId, reseauId));
    }
}
