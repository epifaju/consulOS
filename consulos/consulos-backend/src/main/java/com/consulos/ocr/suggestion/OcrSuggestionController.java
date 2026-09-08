package com.consulos.ocr.suggestion;

import com.consulos.auth.ConsulosUserDetails;
import com.consulos.common.ApiResponse;
import com.consulos.common.BusinessException;
import com.consulos.ocr.dto.OcrFieldSuggestionDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "14 — OCR", description = "Suggestions pré-remplissage (approfondissement)")
public class OcrSuggestionController {

    private final OcrSuggestionService ocrSuggestionService;

    @GetMapping("/documents/{documentId}/suggestions")
    @PreAuthorize("hasAnyRole('AGENT','SUPERVISEUR','ADMIN')")
    @Operation(summary = "Champs structurés extraits pour pré-remplir un formulaire usager")
    public ApiResponse<OcrFieldSuggestionDTO> suggestions(@PathVariable UUID documentId) {
        return ApiResponse.ok(ocrSuggestionService.suggest(documentId, organisationId()));
    }

    private UUID organisationId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof ConsulosUserDetails user)) {
            throw new BusinessException("UNAUTHORIZED", "Utilisateur non authentifié", HttpStatus.UNAUTHORIZED);
        }
        return user.getOrganisationId();
    }
}
