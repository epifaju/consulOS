package com.consulos.document;

import com.consulos.common.ApiResponse;
import com.consulos.document.dto.DocumentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/dossiers")
@RequiredArgsConstructor
public class DossierDocumentController {

    private final DocumentService documentService;

    @GetMapping("/{dossierId}/documents")
    @PreAuthorize("hasAnyRole('AGENT','SUPERVISEUR','CAISSIER','ADMIN')")
    public ApiResponse<List<DocumentDTO>> listByDossier(@PathVariable UUID dossierId) {
        UUID organisationId = DocumentSecurityUtils.getOrganisationId();
        return ApiResponse.ok(documentService.listByDossier(dossierId, organisationId));
    }
}
