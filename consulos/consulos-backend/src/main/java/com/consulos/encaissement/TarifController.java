package com.consulos.encaissement;

import com.consulos.common.ApiResponse;
import com.consulos.encaissement.dto.TarifDTO;
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
@RequestMapping("/tarifs")
@RequiredArgsConstructor
public class TarifController {

    private final TarifService tarifService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<TarifDTO>> list() {
        UUID organisationId = EncaissementSecurityUtils.getOrganisationId();
        return ApiResponse.ok(tarifService.list(organisationId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TarifDTO>> create(@Valid @RequestBody TarifDTO dto) {
        UUID organisationId = EncaissementSecurityUtils.getOrganisationId();
        TarifDTO created = tarifService.create(dto, organisationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TarifDTO> update(@PathVariable UUID id, @Valid @RequestBody TarifDTO dto) {
        UUID organisationId = EncaissementSecurityUtils.getOrganisationId();
        return ApiResponse.ok(tarifService.update(id, dto, organisationId));
    }
}
