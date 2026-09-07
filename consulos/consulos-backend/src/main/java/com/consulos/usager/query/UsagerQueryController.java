package com.consulos.usager.query;

import com.consulos.common.ApiResponse;
import com.consulos.usager.UsagerSecurityUtils;
import com.consulos.usager.dto.DossierSummaryDTO;
import com.consulos.usager.query.dto.UsagerActivityItemDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "02 — Usagers — Requêtes", description = "Dossiers liés et historique d'activité usager")
@RestController
@RequestMapping("/usager-queries")
@RequiredArgsConstructor
public class UsagerQueryController {

    private final UsagerDossierListQueryService dossierListQueryService;
    private final UsagerActivityQueryService activityQueryService;

    @GetMapping("/{id}/dossiers")
    @PreAuthorize("hasAnyRole('AGENT','SUPERVISEUR','ADMIN')")
    @Operation(summary = "Lister les dossiers d'un usager")
    public ApiResponse<List<DossierSummaryDTO>> dossiers(@PathVariable UUID id) {
        UUID organisationId = UsagerSecurityUtils.getOrganisationId();
        return ApiResponse.ok(dossierListQueryService.listDossiers(id, organisationId));
    }

    @GetMapping("/{id}/activity")
    @PreAuthorize("hasAnyRole('AGENT','SUPERVISEUR','CAISSIER','ADMIN')")
    @Operation(summary = "Historique d'activité d'un usager (dossiers, RDV, encaissements)")
    public ApiResponse<List<UsagerActivityItemDTO>> activity(@PathVariable UUID id) {
        UUID organisationId = UsagerSecurityUtils.getOrganisationId();
        return ApiResponse.ok(activityQueryService.listActivity(id, organisationId));
    }
}
