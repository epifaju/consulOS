package com.consulos.dossier.assign;

import com.consulos.auth.entity.RoleEntity;
import com.consulos.auth.entity.UserEntity;
import com.consulos.dossier.assign.dto.AgentOptionDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DossierAgentLookupServiceTest {

    @Mock
    private DossierAssignAgentRepository dossierAssignAgentRepository;

    @InjectMocks
    private DossierAgentLookupService dossierAgentLookupService;

    @Test
    @SuppressWarnings("unchecked")
    void listAssignableAgents_shouldMapUsersAndRoleCodes() {
        UUID organisationId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();

        RoleEntity role = new RoleEntity();
        role.setCode("ROLE_AGENT");

        UserEntity agent = new UserEntity();
        agent.setId(agentId);
        agent.setNom("Sow");
        agent.setPrenom("Ibrahima");
        agent.setEmail("agent@consulos.local");
        agent.setRole(role);

        when(dossierAssignAgentRepository.findAssignableAgents(eq(organisationId), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(agent));

        List<AgentOptionDTO> result = dossierAgentLookupService.listAssignableAgents(organisationId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(agentId);
        assertThat(result.getFirst().nom()).isEqualTo("Sow");
        assertThat(result.getFirst().prenom()).isEqualTo("Ibrahima");
        assertThat(result.getFirst().email()).isEqualTo("agent@consulos.local");
        assertThat(result.getFirst().roleCode()).isEqualTo("ROLE_AGENT");

        ArgumentCaptor<List<String>> rolesCaptor = ArgumentCaptor.forClass(List.class);
        verify(dossierAssignAgentRepository).findAssignableAgents(eq(organisationId), rolesCaptor.capture());
        assertThat(rolesCaptor.getValue()).containsExactly("ROLE_AGENT", "ROLE_SUPERVISEUR");
    }

    @Test
    void listAssignableAgents_whenRoleNull_shouldReturnNullRoleCode() {
        UUID organisationId = UUID.randomUUID();
        UserEntity agent = new UserEntity();
        agent.setId(UUID.randomUUID());
        agent.setNom("Camara");
        agent.setPrenom("Awa");
        agent.setEmail("awa@consulos.local");
        agent.setRole(null);

        when(dossierAssignAgentRepository.findAssignableAgents(eq(organisationId), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(agent));

        List<AgentOptionDTO> result = dossierAgentLookupService.listAssignableAgents(organisationId);

        assertThat(result.getFirst().roleCode()).isNull();
    }

    @Test
    void listAssignableAgents_whenEmpty_shouldReturnEmptyList() {
        UUID organisationId = UUID.randomUUID();
        when(dossierAssignAgentRepository.findAssignableAgents(eq(organisationId), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of());

        assertThat(dossierAgentLookupService.listAssignableAgents(organisationId)).isEmpty();
    }
}
