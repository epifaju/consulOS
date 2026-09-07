package com.consulos.usager.pdf;

import com.consulos.usager.UsagerSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "02 — Usagers — Fiche PDF", description = "Export PDF de la fiche usager")
@RestController
@RequestMapping("/usagers")
@RequiredArgsConstructor
public class UsagerFichePdfController {

    private final UsagerFichePdfService fichePdfService;

    @GetMapping("/{id}/fiche/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISEUR','AGENT')")
    @Operation(summary = "Exporter la fiche usager en PDF")
    public ResponseEntity<byte[]> exportFichePdf(@PathVariable UUID id) {
        UUID organisationId = UsagerSecurityUtils.getOrganisationId();
        byte[] pdf = fichePdfService.generateFiche(id, organisationId);
        String filename = fichePdfService.suggestedFilename(id, organisationId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
