package com.consulos.administration;

import com.consulos.administration.dto.RoleDTO;
import com.consulos.auth.entity.RoleEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminRoleService {

    private final RoleRepository roleRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<RoleDTO> list() {
        return roleRepository.findAll().stream().map(this::toDto).toList();
    }

    private RoleDTO toDto(RoleEntity role) {
        List<String> permissions = RolePermissionsCatalog.PERMISSIONS.getOrDefault(role.getCode(), List.of());
        if (permissions.isEmpty()) {
            permissions = parseDbPermissions(role.getPermissions());
        }
        return new RoleDTO(role.getId(), role.getCode(), role.getLibelle(), permissions);
    }

    private List<String> parseDbPermissions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }
}
