package com.consulos.document.query;

import com.consulos.common.ApiResponse;
import com.consulos.common.PageResponse;
import com.consulos.document.DocumentSecurityUtils;
import com.consulos.document.StatutControle;
import com.consulos.document.query.dto.DocumentListItemDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Tag(name = "05 — Documents — Requêtes", description = "Liste globale des documents avec filtres")
@RestController
@RequestMapping("/document-queries")
@RequiredArgsConstructor
public class DocumentQueryController {

    private final DocumentListQueryService documentListQueryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT','SUPERVISEUR','CAISSIER','ADMIN')")
    @Operation(summary = "Lister les documents de l'organisation")
    public ApiResponse<PageResponse<DocumentListItemDTO>> list(
            @RequestParam(required = false) String typeDocument,
            @RequestParam(required = false) List<StatutControle> statutControle,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String dossierReference,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateFin,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID organisationId = DocumentSecurityUtils.getOrganisationId();
        return ApiResponse.ok(documentListQueryService.list(
                organisationId,
                typeDocument,
                statutControle,
                search,
                dossierReference,
                dateDebut,
                dateFin,
                pageable
        ));
    }
}
