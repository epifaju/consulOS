package com.consulos.usager.query;

import com.consulos.common.ApiResponse;
import com.consulos.usager.UsagerSecurityUtils;
import com.consulos.usager.query.dto.UsagerDocumentSummaryDTO;
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

@Tag(name = "02 — Usagers — Requêtes", description = "Dossiers liés, documents et historique d'activité usager")
@RestController
@RequestMapping("/usager-queries")
@RequiredArgsConstructor
public class UsagerDocumentQueryController {

    private final UsagerDocumentListQueryService usagerDocumentListQueryService;

    @GetMapping("/{id}/documents")
    @PreAuthorize("hasAnyRole('AGENT','SUPERVISEUR','CAISSIER','ADMIN')")
    @Operation(summary = "Lister les documents d'un usager")
    public ApiResponse<List<UsagerDocumentSummaryDTO>> documents(@PathVariable UUID id) {
        UUID organisationId = UsagerSecurityUtils.getOrganisationId();
        return ApiResponse.ok(usagerDocumentListQueryService.listDocuments(id, organisationId));
    }
}
