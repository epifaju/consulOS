package com.consulos.rendezvous;

import com.consulos.common.ApiResponse;
import com.consulos.common.PageResponse;
import com.consulos.rendezvous.dto.CreneauDTO;
import com.consulos.rendezvous.dto.RdvAgendaItemDTO;
import com.consulos.rendezvous.dto.RdvCreateDTO;
import com.consulos.rendezvous.dto.RdvDetailDTO;
import com.consulos.rendezvous.dto.RdvStatutUpdateDTO;
import com.consulos.rendezvous.dto.RdvSummaryDTO;
import com.consulos.rendezvous.dto.RdvUpdateDTO;
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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/rendezvous")
@RequiredArgsConstructor
public class RdvController {

    private final RdvService rdvService;
    private final DisponibiliteService disponibiliteService;

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT','ACCUEIL','SUPERVISEUR','ADMIN')")
    public ApiResponse<PageResponse<RdvSummaryDTO>> list(
            @RequestParam(required = false) RdvStatut statut,
            @RequestParam(required = false) UUID agentId,
            @RequestParam(required = false) UUID usagerId,
            @RequestParam(required = false) UUID typeServiceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateFin,
            @PageableDefault(size = 20, sort = "dateHeure", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        UUID organisationId = RdvSecurityUtils.getOrganisationId();
        return ApiResponse.ok(rdvService.list(
                organisationId, statut, agentId, usagerId, typeServiceId, dateDebut, dateFin, pageable
        ));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('AGENT','ACCUEIL','ADMIN')")
    public ResponseEntity<ApiResponse<RdvDetailDTO>> create(@Valid @RequestBody RdvCreateDTO dto) {
        UUID organisationId = RdvSecurityUtils.getOrganisationId();
        RdvDetailDTO created = rdvService.create(dto, organisationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }

    @GetMapping("/disponibilites")
    @PreAuthorize("hasAnyRole('AGENT','ACCUEIL','SUPERVISEUR','ADMIN')")
    public ApiResponse<List<CreneauDTO>> disponibilites(
            @RequestParam(required = false) UUID typeServiceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        UUID organisationId = RdvSecurityUtils.getOrganisationId();
        return ApiResponse.ok(disponibiliteService.getDisponibilites(organisationId, typeServiceId, date));
    }

    @GetMapping("/agenda")
    @PreAuthorize("hasAnyRole('AGENT','ACCUEIL','SUPERVISEUR','ADMIN')")
    public ApiResponse<List<RdvAgendaItemDTO>> agenda(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) UUID agentId
    ) {
        UUID organisationId = RdvSecurityUtils.getOrganisationId();
        return ApiResponse.ok(rdvService.getAgenda(organisationId, dateDebut, dateFin, agentId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT','ACCUEIL','SUPERVISEUR','ADMIN')")
    public ApiResponse<RdvDetailDTO> getById(@PathVariable UUID id) {
        UUID organisationId = RdvSecurityUtils.getOrganisationId();
        return ApiResponse.ok(rdvService.findById(id, organisationId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT','ACCUEIL','ADMIN')")
    public ApiResponse<RdvDetailDTO> update(@PathVariable UUID id, @Valid @RequestBody RdvUpdateDTO dto) {
        UUID organisationId = RdvSecurityUtils.getOrganisationId();
        return ApiResponse.ok(rdvService.update(id, dto, organisationId));
    }

    @PostMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('AGENT','ACCUEIL','ADMIN')")
    public ApiResponse<RdvDetailDTO> updateStatut(
            @PathVariable UUID id,
            @Valid @RequestBody RdvStatutUpdateDTO dto
    ) {
        UUID organisationId = RdvSecurityUtils.getOrganisationId();
        return ApiResponse.ok(rdvService.updateStatut(id, dto.statut(), dto.motif(), organisationId));
    }
}
