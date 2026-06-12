package com.consulos.dossier;

import com.consulos.common.ApiResponse;
import com.consulos.dossier.dto.TypeServiceDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/types-service")
@RequiredArgsConstructor
public class TypeServiceController {

    private final TypeServiceRepository typeServiceRepository;
    private final TypeServiceMapper typeServiceMapper;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<TypeServiceDTO>> list() {
        UUID organisationId = DossierSecurityUtils.getOrganisationId();
        List<TypeServiceDTO> types = typeServiceRepository.findByOrganisationIdAndActifTrue(organisationId).stream()
                .map(typeServiceMapper::toDto)
                .toList();
        return ApiResponse.ok(types);
    }
}
