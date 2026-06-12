package com.consulos.dossier;

import com.consulos.common.ApiResponse;
import com.consulos.common.PageResponse;
import com.consulos.dossier.dto.CommentaireCreateDTO;
import com.consulos.dossier.dto.CommentaireDTO;
import com.consulos.dossier.dto.DossierCreateDTO;
import com.consulos.dossier.dto.DossierDetailDTO;
import com.consulos.dossier.dto.DossierStatsDTO;
import com.consulos.dossier.dto.DossierSummaryDTO;
import com.consulos.dossier.dto.DossierTransitionDTO;
import com.consulos.dossier.dto.DossierUpdateDTO;
import com.consulos.dossier.dto.HistoriqueDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/dossiers")
@RequiredArgsConstructor
public class DossierController {

    private final DossierService dossierService;

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT','SUPERVISEUR','CAISSIER','ADMIN')")
    public ApiResponse<PageResponse<DossierSummaryDTO>> list(
            @RequestParam(required = false) List<DossierStatut> statut,
            @RequestParam(required = false) UUID typeService,
            @RequestParam(required = false) UUID agentId,
            @RequestParam(required = false) UUID usagerId,
            @RequestParam(required = false) DossierPriorite priorite,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateFin,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID organisationId = DossierSecurityUtils.getOrganisationId();
        return ApiResponse.ok(dossierService.list(
                organisationId, statut, typeService, agentId, usagerId, priorite, dateDebut, dateFin, pageable
        ));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('AGENT','SUPERVISEUR','ADMIN')")
    public ResponseEntity<ApiResponse<DossierDetailDTO>> create(@Valid @RequestBody DossierCreateDTO dto) {
        UUID organisationId = DossierSecurityUtils.getOrganisationId();
        UUID userId = DossierSecurityUtils.getUserId();
        DossierDetailDTO created = dossierService.create(dto, organisationId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('SUPERVISEUR','ADMIN')")
    public ApiResponse<DossierStatsDTO> stats() {
        UUID organisationId = DossierSecurityUtils.getOrganisationId();
        return ApiResponse.ok(dossierService.getStats(organisationId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT','SUPERVISEUR','CAISSIER','ACCUEIL','ADMIN')")
    public ApiResponse<DossierDetailDTO> getById(@PathVariable UUID id) {
        UUID organisationId = DossierSecurityUtils.getOrganisationId();
        return ApiResponse.ok(dossierService.findById(id, organisationId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT','SUPERVISEUR','ADMIN')")
    public ApiResponse<DossierDetailDTO> update(@PathVariable UUID id, @Valid @RequestBody DossierUpdateDTO dto) {
        UUID organisationId = DossierSecurityUtils.getOrganisationId();
        UUID userId = DossierSecurityUtils.getUserId();
        return ApiResponse.ok(dossierService.update(id, dto, organisationId, userId));
    }

    @PostMapping("/{id}/transition")
    @PreAuthorize("hasAnyRole('AGENT','SUPERVISEUR','ADMIN')")
    public ApiResponse<DossierDetailDTO> transition(
            @PathVariable UUID id,
            @Valid @RequestBody DossierTransitionDTO dto
    ) {
        UUID organisationId = DossierSecurityUtils.getOrganisationId();
        UUID userId = DossierSecurityUtils.getUserId();
        String userRole = DossierSecurityUtils.getUserRole();
        return ApiResponse.ok(dossierService.transition(
                id, dto.statut(), dto.motif(), userId, userRole, organisationId
        ));
    }

    @GetMapping("/{id}/historique")
    @PreAuthorize("hasAnyRole('AGENT','SUPERVISEUR','ADMIN')")
    public ApiResponse<List<HistoriqueDTO>> historique(@PathVariable UUID id) {
        UUID organisationId = DossierSecurityUtils.getOrganisationId();
        return ApiResponse.ok(dossierService.getHistorique(id, organisationId));
    }

    @PostMapping("/{id}/commentaires")
    @PreAuthorize("hasAnyRole('AGENT','SUPERVISEUR','ADMIN')")
    public ResponseEntity<ApiResponse<CommentaireDTO>> addCommentaire(
            @PathVariable UUID id,
            @Valid @RequestBody CommentaireCreateDTO dto
    ) {
        UUID organisationId = DossierSecurityUtils.getOrganisationId();
        UUID userId = DossierSecurityUtils.getUserId();
        CommentaireDTO created = dossierService.ajouterCommentaire(id, dto, userId, organisationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }
}
