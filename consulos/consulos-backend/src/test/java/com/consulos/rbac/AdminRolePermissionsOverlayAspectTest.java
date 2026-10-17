package com.consulos.rbac;

import com.consulos.administration.dto.RoleDTO;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminRolePermissionsOverlayAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    private final AdminRolePermissionsOverlayAspect aspect = new AdminRolePermissionsOverlayAspect();

    @Test
    void overlay_shouldReplaceLegacyCatalogPermissions() throws Throwable {
        UUID id = UUID.randomUUID();
        when(joinPoint.proceed()).thenReturn(List.of(
                new RoleDTO(id, "ROLE_CAISSIER", "Caissier", List.of("encaissements:read"))
        ));

        @SuppressWarnings("unchecked")
        List<RoleDTO> result = (List<RoleDTO>) aspect.overlayUnifiedPermissions(joinPoint);

        assertThat(result.getFirst().permissions()).contains("usagers:read", "encaissements:write");
        assertThat(result.getFirst().id()).isEqualTo(id);
    }

    @Test
    void overlay_shouldKeepUnknownRolePermissions() throws Throwable {
        UUID id = UUID.randomUUID();
        when(joinPoint.proceed()).thenReturn(List.of(
                new RoleDTO(id, "ROLE_CUSTOM", "Custom", List.of("custom:read"))
        ));

        @SuppressWarnings("unchecked")
        List<RoleDTO> result = (List<RoleDTO>) aspect.overlayUnifiedPermissions(joinPoint);

        assertThat(result.getFirst().permissions()).containsExactly("custom:read");
    }
}
