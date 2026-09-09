package com.consulos.administration;

import com.consulos.administration.dto.RoleDTO;
import com.consulos.auth.entity.RoleEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminRoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    private AdminRoleService adminRoleService;

    @BeforeEach
    void setUp() {
        adminRoleService = new AdminRoleService(roleRepository, new ObjectMapper());
    }

    @Test
    void list_shouldPreferCatalogPermissionsOverDb() {
        RoleEntity role = new RoleEntity();
        role.setId(UUID.randomUUID());
        role.setCode("ROLE_AGENT");
        role.setLibelle("Agent");
        role.setPermissions("[\"legacy:perm\"]");
        when(roleRepository.findAll()).thenReturn(List.of(role));

        List<RoleDTO> result = adminRoleService.list();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().code()).isEqualTo("ROLE_AGENT");
        assertThat(result.getFirst().permissions())
                .contains("dossiers:write", "usagers:read", "documents:write");
        assertThat(result.getFirst().permissions()).doesNotContain("legacy:perm");
    }

    @Test
    void list_whenUnknownRole_shouldFallbackToDbJson() {
        RoleEntity role = new RoleEntity();
        role.setId(UUID.randomUUID());
        role.setCode("ROLE_CUSTOM");
        role.setLibelle("Custom");
        role.setPermissions("[\"custom:read\"]");
        when(roleRepository.findAll()).thenReturn(List.of(role));

        List<RoleDTO> result = adminRoleService.list();

        assertThat(result.getFirst().permissions()).containsExactly("custom:read");
    }

    @Test
    void list_whenUnknownRoleAndInvalidJson_shouldReturnEmptyPermissions() {
        RoleEntity role = new RoleEntity();
        role.setId(UUID.randomUUID());
        role.setCode("ROLE_BROKEN");
        role.setLibelle("Broken");
        role.setPermissions("{not-json");
        when(roleRepository.findAll()).thenReturn(List.of(role));

        List<RoleDTO> result = adminRoleService.list();

        assertThat(result.getFirst().permissions()).isEmpty();
    }
}
