package com.consulos.dossier.assign;

import com.consulos.auth.entity.UserEntity;
import com.consulos.dossier.assign.dto.AgentOptionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DossierAgentLookupService {

    private static final List<String> ASSIGNABLE_ROLE_CODES = List.of(
            "ROLE_AGENT",
            "ROLE_SUPERVISEUR"
    );

    private final DossierAssignAgentRepository dossierAssignAgentRepository;

    @Transactional(readOnly = true)
    public List<AgentOptionDTO> listAssignableAgents(UUID organisationId) {
        return dossierAssignAgentRepository.findAssignableAgents(organisationId, ASSIGNABLE_ROLE_CODES)
                .stream()
                .map(this::toOption)
                .toList();
    }

    private AgentOptionDTO toOption(UserEntity user) {
        String roleCode = user.getRole() != null ? user.getRole().getCode() : null;
        return new AgentOptionDTO(
                user.getId(),
                user.getNom(),
                user.getPrenom(),
                user.getEmail(),
                roleCode
        );
    }
}
