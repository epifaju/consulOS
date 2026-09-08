package com.consulos.document.content;

import com.consulos.document.DocumentSecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Streaming document via l'API (évite d'ouvrir l'URL MinIO prescrite dans le navigateur,
 * qui affiche du XML brut en cas de NoSuchKey).
 */
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentContentController {

    private final DocumentContentService documentContentService;

    @GetMapping("/{id}/content")
    @PreAuthorize("hasAnyRole('AGENT','SUPERVISEUR','CAISSIER','ADMIN')")
    public ResponseEntity<ByteArrayResource> download(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "attachment") String disposition
    ) {
        UUID organisationId = DocumentSecurityUtils.getOrganisationId();
        DocumentContentPayload payload = documentContentService.open(id, organisationId);

        String type = "inline".equalsIgnoreCase(disposition) ? "inline" : "attachment";
        ContentDisposition contentDisposition = ContentDisposition.builder(type)
                .filename(payload.fileName(), StandardCharsets.UTF_8)
                .build();

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(payload.mimeType());
        } catch (Exception ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        ByteArrayResource body = new ByteArrayResource(payload.bytes());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(mediaType)
                .contentLength(payload.bytes().length)
                .body(body);
    }
}
