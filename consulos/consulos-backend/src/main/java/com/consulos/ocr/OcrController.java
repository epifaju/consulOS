package com.consulos.ocr;

import com.consulos.auth.ConsulosUserDetails;
import com.consulos.common.ApiResponse;
import com.consulos.common.BusinessException;
import com.consulos.ocr.dto.DocumentOcrResultDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/ocr")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "consulos.ocr", name = "enabled", havingValue = "true")
public class OcrController {

    private final OcrService ocrService;

    @GetMapping("/documents/{documentId}")
    @PreAuthorize("hasAnyRole('AGENT','SUPERVISEUR','ADMIN')")
    public ApiResponse<DocumentOcrResultDTO> getDocumentOcr(@PathVariable UUID documentId) {
        return ApiResponse.ok(ocrService.getResult(documentId, organisationId()));
    }

    private UUID organisationId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof ConsulosUserDetails user)) {
            throw new BusinessException("UNAUTHORIZED", "Utilisateur non authentifié", HttpStatus.UNAUTHORIZED);
        }
        return user.getOrganisationId();
    }
}
