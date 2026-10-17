package com.consulos.dossier.checklist;

import com.consulos.common.ApiResponse;
import com.consulos.dossier.DossierSecurityUtils;
import com.consulos.dossier.checklist.dto.DossierChecklistItemDTO;
import com.consulos.dossier.checklist.dto.DossierChecklistUpdateDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/dossiers")
@RequiredArgsConstructor
public class DossierChecklistController {

    private final DossierChecklistService checklistService;

    @GetMapping("/{id}/checklist")
    @PreAuthorize("hasAnyRole('AGENT','SUPERVISEUR','CAISSIER','ACCUEIL','ADMIN')")
    public ApiResponse<List<DossierChecklistItemDTO>> list(@PathVariable UUID id) {
        return ApiResponse.ok(checklistService.list(id, DossierSecurityUtils.getOrganisationId()));
    }

    @PutMapping("/{id}/checklist/{code}")
    @PreAuthorize("hasAnyRole('AGENT','SUPERVISEUR','ADMIN')")
    public ApiResponse<List<DossierChecklistItemDTO>> update(
            @PathVariable UUID id,
            @PathVariable String code,
            @Valid @RequestBody DossierChecklistUpdateDTO dto
    ) {
        return ApiResponse.ok(checklistService.update(
                id, code, Boolean.TRUE.equals(dto.valide()), DossierSecurityUtils.getOrganisationId()));
    }
}
