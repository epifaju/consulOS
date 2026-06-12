package com.consulos.administration;

import com.consulos.administration.dto.OrganisationDTO;
import com.consulos.administration.dto.RoleDTO;
import com.consulos.administration.dto.UserAdminDTO;
import com.consulos.administration.dto.UserCreateDTO;
import com.consulos.administration.dto.UserCreateResponseDTO;
import com.consulos.administration.dto.UserUpdateDTO;
import com.consulos.common.ApiResponse;
import com.consulos.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminUserService adminUserService;
    private final AdminRoleService adminRoleService;
    private final OrganisationService organisationService;

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<UserAdminDTO>> listUsers(
            @PageableDefault(size = 20, sort = "nom", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        UUID organisationId = AdminSecurityUtils.getOrganisationId();
        return ApiResponse.ok(adminUserService.list(organisationId, pageable));
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserAdminDTO> getUser(@PathVariable UUID id) {
        UUID organisationId = AdminSecurityUtils.getOrganisationId();
        return ApiResponse.ok(adminUserService.findById(id, organisationId));
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserCreateResponseDTO>> createUser(@Valid @RequestBody UserCreateDTO dto) {
        UUID organisationId = AdminSecurityUtils.getOrganisationId();
        UUID userId = AdminSecurityUtils.getUserId();
        UserCreateResponseDTO created = adminUserService.create(dto, organisationId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserAdminDTO> updateUser(@PathVariable UUID id, @Valid @RequestBody UserUpdateDTO dto) {
        UUID organisationId = AdminSecurityUtils.getOrganisationId();
        return ApiResponse.ok(adminUserService.update(id, dto, organisationId));
    }

    @PostMapping("/users/{id}/toggle-actif")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserAdminDTO> toggleActif(@PathVariable UUID id) {
        UUID organisationId = AdminSecurityUtils.getOrganisationId();
        return ApiResponse.ok(adminUserService.toggleActif(id, organisationId));
    }

    @PostMapping("/users/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, String>> resetPassword(@PathVariable UUID id) {
        UUID organisationId = AdminSecurityUtils.getOrganisationId();
        String tempPassword = adminUserService.resetPassword(id, organisationId);
        return ApiResponse.ok(Map.of("temporaryPassword", tempPassword));
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        UUID organisationId = AdminSecurityUtils.getOrganisationId();
        UUID currentUserId = AdminSecurityUtils.getUserId();
        adminUserService.delete(id, organisationId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<RoleDTO>> listRoles() {
        return ApiResponse.ok(adminRoleService.list());
    }

    @GetMapping("/organisation")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OrganisationDTO> getOrganisation() {
        UUID organisationId = AdminSecurityUtils.getOrganisationId();
        return ApiResponse.ok(organisationService.get(organisationId));
    }

    @PutMapping("/organisation")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OrganisationDTO> updateOrganisation(@Valid @RequestBody OrganisationDTO dto) {
        UUID organisationId = AdminSecurityUtils.getOrganisationId();
        return ApiResponse.ok(organisationService.update(organisationId, dto));
    }
}
