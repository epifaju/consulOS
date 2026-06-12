package com.consulos.usager;

import com.consulos.common.ApiResponse;
import com.consulos.common.PageResponse;
import com.consulos.usager.dto.DossierSummaryDTO;
import com.consulos.usager.dto.DoublonGroupDTO;
import com.consulos.usager.dto.UsagerCreateDTO;
import com.consulos.usager.dto.UsagerCreateResponseDTO;
import com.consulos.usager.dto.UsagerDetailDTO;
import com.consulos.usager.dto.UsagerSummaryDTO;
import com.consulos.usager.dto.UsagerUpdateDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/usagers")
@RequiredArgsConstructor
public class UsagerController {

    private final UsagerService usagerService;

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT','SUPERVISEUR','CAISSIER','ADMIN')")
    public ApiResponse<PageResponse<UsagerSummaryDTO>> list(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "nom", direction = Sort.Direction.ASC) Pageable pageable) {
        UUID organisationId = UsagerSecurityUtils.getOrganisationId();
        return ApiResponse.ok(usagerService.search(q, organisationId, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<ApiResponse<UsagerCreateResponseDTO>> create(
            @Valid @RequestBody UsagerCreateDTO dto) {
        UUID organisationId = UsagerSecurityUtils.getOrganisationId();
        UsagerCreateResponseDTO result = usagerService.create(dto, organisationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT','SUPERVISEUR','CAISSIER','ADMIN')")
    public ApiResponse<UsagerDetailDTO> getById(@PathVariable UUID id) {
        UUID organisationId = UsagerSecurityUtils.getOrganisationId();
        return ApiResponse.ok(usagerService.findById(id, organisationId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ApiResponse<UsagerDetailDTO> update(@PathVariable UUID id,
                                               @Valid @RequestBody UsagerUpdateDTO dto) {
        UUID organisationId = UsagerSecurityUtils.getOrganisationId();
        return ApiResponse.ok(usagerService.update(id, dto, organisationId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        UUID organisationId = UsagerSecurityUtils.getOrganisationId();
        usagerService.softDelete(id, organisationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('AGENT','SUPERVISEUR','CAISSIER','ACCUEIL','ADMIN')")
    public ApiResponse<List<UsagerSummaryDTO>> search(@RequestParam String q) {
        UUID organisationId = UsagerSecurityUtils.getOrganisationId();
        return ApiResponse.ok(usagerService.searchQuick(q, organisationId));
    }

    @GetMapping("/doublons")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ApiResponse<List<DoublonGroupDTO>> doublons(
            @RequestParam String nom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateNaissance,
            @RequestParam String nationalite,
            @RequestParam(required = false) UUID excludeId) {
        UUID organisationId = UsagerSecurityUtils.getOrganisationId();
        return ApiResponse.ok(usagerService.findDoublons(nom, dateNaissance, nationalite, organisationId, excludeId));
    }

    @GetMapping("/{id}/dossiers")
    @PreAuthorize("hasAnyRole('AGENT','SUPERVISEUR','ADMIN')")
    public ApiResponse<List<DossierSummaryDTO>> dossiers(@PathVariable UUID id) {
        UUID organisationId = UsagerSecurityUtils.getOrganisationId();
        return ApiResponse.ok(usagerService.getHistorique(id, organisationId));
    }
}
