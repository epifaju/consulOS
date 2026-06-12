package com.consulos.document;

import com.consulos.common.ApiResponse;
import com.consulos.document.dto.DocumentDTO;
import com.consulos.document.dto.DownloadUrlDTO;
import com.consulos.document.dto.StatutControleDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<ApiResponse<DocumentDTO>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) UUID dossierId,
            @RequestParam(required = false) UUID usagerId,
            @RequestParam String typeDocument,
            @RequestParam(required = false) String notes
    ) {
        UUID organisationId = DocumentSecurityUtils.getOrganisationId();
        UUID userId = DocumentSecurityUtils.getUserId();
        DocumentDTO created = documentService.upload(
                file, dossierId, usagerId, typeDocument, notes, userId, organisationId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT','SUPERVISEUR','ADMIN')")
    public ApiResponse<DocumentDTO> getById(@PathVariable UUID id) {
        UUID organisationId = DocumentSecurityUtils.getOrganisationId();
        return ApiResponse.ok(documentService.findById(id, organisationId));
    }

    @GetMapping("/{id}/download-url")
    @PreAuthorize("hasAnyRole('AGENT','SUPERVISEUR','CAISSIER','ADMIN')")
    public ApiResponse<DownloadUrlDTO> getDownloadUrl(@PathVariable UUID id) {
        UUID organisationId = DocumentSecurityUtils.getOrganisationId();
        return ApiResponse.ok(documentService.getDownloadUrl(id, organisationId));
    }

    @PutMapping("/{id}/statut-controle")
    @PreAuthorize("hasAnyRole('AGENT','SUPERVISEUR','ADMIN')")
    public ApiResponse<DocumentDTO> updateStatutControle(
            @PathVariable UUID id,
            @Valid @RequestBody StatutControleDTO dto
    ) {
        UUID organisationId = DocumentSecurityUtils.getOrganisationId();
        return ApiResponse.ok(documentService.updateStatutControle(id, dto, organisationId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        UUID organisationId = DocumentSecurityUtils.getOrganisationId();
        documentService.softDelete(id, organisationId);
        return ResponseEntity.noContent().build();
    }
}
