package com.consulos.rbac;

import com.consulos.administration.RolePermissionsCatalog;
import com.consulos.security.MvpRoleAuthorizationReference;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MvpRbacMatrixTest {

    @Test
    void matrix_shouldCoverMvpAndReseauRoles() {
        assertThat(MvpRbacMatrix.PERMISSIONS.keySet()).containsExactlyInAnyOrder(
                "ROLE_ADMIN",
                "ROLE_SUPERVISEUR",
                "ROLE_AGENT",
                "ROLE_CAISSIER",
                "ROLE_ACCUEIL",
                "ROLE_ADMIN_RESEAU"
        );
    }

    @Test
    void matrix_shouldBeSupersetOfPreAuthorizeReference() {
        MvpRoleAuthorizationReference.ROLE_CAPABILITIES.forEach((role, capabilities) ->
                assertThat(MvpRbacMatrix.permissionsFor(role))
                        .as(role)
                        .containsAll(capabilities));
    }

    @Test
    void matrix_shouldContainLegacyCatalogCapabilitiesWithSharedNames() {
        RolePermissionsCatalog.PERMISSIONS.forEach((role, capabilities) -> {
            List<String> unified = MvpRbacMatrix.permissionsFor(role);
            capabilities.stream()
                    .filter(cap -> !cap.startsWith("users:") && !cap.equals("roles:read")
                            && !cap.equals("organisation:write"))
                    .forEach(cap -> assertThat(unified).as(role + " " + cap).contains(cap));
        });
    }

    @Test
    void caissier_shouldReadUsagersForCaisse() {
        assertThat(MvpRbacMatrix.hasPermission("ROLE_CAISSIER", "usagers:read")).isTrue();
        assertThat(MvpRbacMatrix.hasPermission("ROLE_CAISSIER", "usagers:write")).isFalse();
    }

    @Test
    void nav_shouldHideAdminFromNonAdmin() {
        assertThat(MvpRbacMatrix.canAccessNav("/administration", "ROLE_AGENT")).isFalse();
        assertThat(MvpRbacMatrix.canAccessNav("/administration", "ROLE_ADMIN")).isTrue();
        assertThat(MvpRbacMatrix.canAccessNav("/caisse", "ROLE_ACCUEIL")).isFalse();
        assertThat(MvpRbacMatrix.canAccessNav("/regional", "ADMIN_RESEAU")).isTrue();
    }

    @Test
    void unknownRole_hasNoPermissions() {
        assertThat(MvpRbacMatrix.permissionsFor("ROLE_UNKNOWN")).isEmpty();
        assertThat(MvpRbacMatrix.hasPermission(null, "admin:read")).isFalse();
    }

    @Test
    void navMap_shouldHaveNineStaffPaths() {
        assertThat(MvpRbacMatrix.NAV).hasSize(9);
        assertThat(MvpRbacMatrix.NAV.keySet()).contains("/", "/usagers", "/administration");
    }
}
