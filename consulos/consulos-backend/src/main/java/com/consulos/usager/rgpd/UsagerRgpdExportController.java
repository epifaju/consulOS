package com.consulos.usager.rgpd;

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

@Tag(name = "Usagers — RGPD", description = "Export des données personnelles (portabilité RGPD)")
@RestController
@RequestMapping("/usagers")
@RequiredArgsConstructor
public class UsagerRgpdExportController {

    private final UsagerRgpdExportService exportService;

    @GetMapping("/{id}/export-rgpd")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISEUR','AGENT')")
    @Operation(summary = "Exporter les données personnelles d'un usager (JSON RGPD)")
    public ResponseEntity<byte[]> exportRgpd(@PathVariable UUID id) {
        UUID organisationId = UsagerSecurityUtils.getOrganisationId();
        byte[] json = exportService.exportJson(id, organisationId);
        String filename = exportService.suggestedFilename(id, organisationId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }
}
