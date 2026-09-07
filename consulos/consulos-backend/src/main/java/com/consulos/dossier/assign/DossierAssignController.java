package com.consulos.dossier.assign;

import com.consulos.common.ApiResponse;
import com.consulos.dossier.DossierSecurityUtils;
import com.consulos.dossier.DossierService;
import com.consulos.dossier.assign.dto.AgentOptionDTO;
import com.consulos.dossier.assign.dto.AssignAgentDTO;
import com.consulos.dossier.dto.DossierDetailDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "03 — Dossiers — Assignation", description = "Affectation d'un agent à un dossier")
@RestController
@RequiredArgsConstructor
public class DossierAssignController {

    private final DossierService dossierService;
    private final DossierAgentLookupService dossierAgentLookupService;

    @GetMapping("/dossier-assign/agents")
    @PreAuthorize("hasAnyRole('AGENT','SUPERVISEUR','ADMIN')")
    @Operation(summary = "Lister les agents assignables")
    public ApiResponse<List<AgentOptionDTO>> listAgents() {
        UUID organisationId = DossierSecurityUtils.getOrganisationId();
        return ApiResponse.ok(dossierAgentLookupService.listAssignableAgents(organisationId));
    }

    @PostMapping("/dossiers/{id}/assign-agent")
    @PreAuthorize("hasAnyRole('AGENT','SUPERVISEUR','ADMIN')")
    @Operation(summary = "Assigner un agent à un dossier")
    public ApiResponse<DossierDetailDTO> assignAgent(
            @PathVariable UUID id,
            @Valid @RequestBody AssignAgentDTO dto
    ) {
        UUID organisationId = DossierSecurityUtils.getOrganisationId();
        return ApiResponse.ok(dossierService.assignerAgent(id, dto.agentId(), organisationId));
    }
}
