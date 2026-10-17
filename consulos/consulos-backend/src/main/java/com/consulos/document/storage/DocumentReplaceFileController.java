package com.consulos.document.storage;

import com.consulos.common.ApiResponse;
import com.consulos.document.DocumentSecurityUtils;
import com.consulos.document.dto.DocumentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentReplaceFileController {

    private final DocumentReplaceFileService replaceFileService;

    @PostMapping(value = "/{id}/replace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('AGENT','SUPERVISEUR','ADMIN')")
    public ApiResponse<DocumentDTO> replace(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file
    ) {
        UUID organisationId = DocumentSecurityUtils.getOrganisationId();
        return ApiResponse.ok(replaceFileService.replaceFile(id, file, organisationId));
    }
}
