package com.consulos.encaissement;

import com.consulos.common.ApiResponse;
import com.consulos.common.PageResponse;
import com.consulos.encaissement.dto.EncaissementCreateDTO;
import com.consulos.encaissement.dto.EncaissementDetailDTO;
import com.consulos.encaissement.dto.EncaissementSummaryDTO;
import com.consulos.encaissement.dto.JournalCaisseDTO;
import com.consulos.encaissement.dto.PaiementDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/encaissements")
@RequiredArgsConstructor
public class EncaissementController {

    private final EncaissementService encaissementService;

    @GetMapping
    @PreAuthorize("hasAnyRole('CAISSIER','SUPERVISEUR','ADMIN')")
    public ApiResponse<PageResponse<EncaissementSummaryDTO>> list(
            @RequestParam(required = false) StatutEncaissement statut,
            @RequestParam(required = false) ModePaiement modePaiement,
            @RequestParam(required = false) UUID caissier,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateFin,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID organisationId = EncaissementSecurityUtils.getOrganisationId();
        return ApiResponse.ok(encaissementService.list(
                organisationId, statut, modePaiement, caissier, dateDebut, dateFin, pageable
        ));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CAISSIER','AGENT','ADMIN')")
    public ResponseEntity<ApiResponse<EncaissementDetailDTO>> create(@Valid @RequestBody EncaissementCreateDTO dto) {
        UUID organisationId = EncaissementSecurityUtils.getOrganisationId();
        UUID userId = EncaissementSecurityUtils.getUserId();
        EncaissementDetailDTO created = encaissementService.create(dto, userId, organisationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }

    @GetMapping("/journal-caisse")
    @PreAuthorize("hasAnyRole('CAISSIER','SUPERVISEUR','ADMIN')")
    public ApiResponse<JournalCaisseDTO> journalCaisse(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        UUID organisationId = EncaissementSecurityUtils.getOrganisationId();
        return ApiResponse.ok(encaissementService.getJournalCaisse(date, organisationId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CAISSIER','SUPERVISEUR','ADMIN')")
    public ApiResponse<EncaissementDetailDTO> getById(@PathVariable UUID id) {
        UUID organisationId = EncaissementSecurityUtils.getOrganisationId();
        return ApiResponse.ok(encaissementService.findById(id, organisationId));
    }

    @PostMapping("/{id}/paiement")
    @PreAuthorize("hasAnyRole('CAISSIER','ADMIN')")
    public ApiResponse<EncaissementDetailDTO> enregistrerPaiement(
            @PathVariable UUID id,
            @Valid @RequestBody PaiementDTO dto
    ) {
        UUID organisationId = EncaissementSecurityUtils.getOrganisationId();
        return ApiResponse.ok(encaissementService.enregistrerPaiement(id, dto, organisationId));
    }

    @GetMapping("/{id}/recu/pdf")
    @PreAuthorize("hasAnyRole('CAISSIER','SUPERVISEUR','ADMIN')")
    public ResponseEntity<byte[]> getRecuPdf(@PathVariable UUID id) {
        UUID organisationId = EncaissementSecurityUtils.getOrganisationId();
        byte[] pdf = encaissementService.getRecu(id, organisationId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"recu-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
