package com.consulos.rendezvous;

import com.consulos.common.ApiResponse;
import com.consulos.rendezvous.dto.ConfigCreneauDTO;
import com.consulos.rendezvous.dto.JourFermetureDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/parametres")
@RequiredArgsConstructor
public class ParametresController {

    private final ParametresService parametresService;

    @GetMapping("/creneaux")
    @PreAuthorize("hasAnyRole('SUPERVISEUR','ADMIN')")
    public ApiResponse<List<ConfigCreneauDTO>> listCreneaux() {
        UUID organisationId = RdvSecurityUtils.getOrganisationId();
        return ApiResponse.ok(parametresService.listCreneaux(organisationId));
    }

    @PostMapping("/creneaux")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ConfigCreneauDTO>> createCreneau(@Valid @RequestBody ConfigCreneauDTO dto) {
        UUID organisationId = RdvSecurityUtils.getOrganisationId();
        ConfigCreneauDTO created = parametresService.createCreneau(dto, organisationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }

    @PutMapping("/creneaux/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ConfigCreneauDTO> updateCreneau(
            @PathVariable UUID id,
            @Valid @RequestBody ConfigCreneauDTO dto
    ) {
        UUID organisationId = RdvSecurityUtils.getOrganisationId();
        return ApiResponse.ok(parametresService.updateCreneau(id, dto, organisationId));
    }

    @GetMapping("/jours-fermeture")
    @PreAuthorize("hasAnyRole('SUPERVISEUR','ADMIN')")
    public ApiResponse<List<JourFermetureDTO>> listJoursFermeture() {
        UUID organisationId = RdvSecurityUtils.getOrganisationId();
        return ApiResponse.ok(parametresService.listJoursFermeture(organisationId));
    }

    @PostMapping("/jours-fermeture")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<JourFermetureDTO>> createJourFermeture(
            @Valid @RequestBody JourFermetureDTO dto
    ) {
        UUID organisationId = RdvSecurityUtils.getOrganisationId();
        JourFermetureDTO created = parametresService.createJourFermeture(dto, organisationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }

    @PutMapping("/jours-fermeture/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<JourFermetureDTO> updateJourFermeture(
            @PathVariable UUID id,
            @Valid @RequestBody JourFermetureDTO dto
    ) {
        UUID organisationId = RdvSecurityUtils.getOrganisationId();
        return ApiResponse.ok(parametresService.updateJourFermeture(id, dto, organisationId));
    }
}
