package com.consulos.dashboard;

import com.consulos.common.ApiResponse;
import com.consulos.dashboard.dto.AgentChargeDTO;
import com.consulos.dashboard.dto.AlerteDTO;
import com.consulos.dashboard.dto.DashboardStatsDTO;
import com.consulos.dashboard.dto.DossierChartDTO;
import com.consulos.dashboard.dto.RecetteChartPointDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardQueryService dashboardQueryService;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('SUPERVISEUR','ADMIN')")
    public ApiResponse<DashboardStatsDTO> stats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin
    ) {
        UUID organisationId = DashboardSecurityUtils.getOrganisationId();
        LocalDate fin = dateFin != null ? dateFin : LocalDate.now();
        LocalDate debut = dateDebut != null ? dateDebut : fin.minusDays(30);
        return ApiResponse.ok(dashboardQueryService.getDashboardStats(organisationId, debut, fin));
    }

    @GetMapping("/dossiers-chart")
    @PreAuthorize("hasAnyRole('SUPERVISEUR','ADMIN')")
    public ApiResponse<List<DossierChartDTO>> dossiersChart(
            @RequestParam(defaultValue = "MOIS") DashboardPeriode periode
    ) {
        UUID organisationId = DashboardSecurityUtils.getOrganisationId();
        return ApiResponse.ok(dashboardQueryService.getDossiersChart(organisationId, periode));
    }

    @GetMapping("/recettes-chart")
    @PreAuthorize("hasAnyRole('SUPERVISEUR','CAISSIER','ADMIN')")
    public ApiResponse<List<RecetteChartPointDTO>> recettesChart(
            @RequestParam(defaultValue = "MOIS") DashboardPeriode periode
    ) {
        UUID organisationId = DashboardSecurityUtils.getOrganisationId();
        return ApiResponse.ok(dashboardQueryService.getRecettesChart(organisationId, periode));
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('SUPERVISEUR','ADMIN')")
    public ApiResponse<List<AlerteDTO>> alerts() {
        UUID organisationId = DashboardSecurityUtils.getOrganisationId();
        return ApiResponse.ok(dashboardQueryService.getAlertesActives(organisationId));
    }

    @GetMapping("/agents-charge")
    @PreAuthorize("hasAnyRole('SUPERVISEUR','ADMIN')")
    public ApiResponse<List<AgentChargeDTO>> agentsCharge() {
        UUID organisationId = DashboardSecurityUtils.getOrganisationId();
        return ApiResponse.ok(dashboardQueryService.getAgentsCharge(organisationId));
    }
}
