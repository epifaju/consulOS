package com.consulos.administration;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RolePermissionsCatalogTest {

    @Test
    void permissions_shouldDefineFiveMvpRoles() {
        assertThat(RolePermissionsCatalog.PERMISSIONS.keySet()).containsExactlyInAnyOrder(
                "ROLE_ADMIN",
                "ROLE_SUPERVISEUR",
                "ROLE_AGENT",
                "ROLE_CAISSIER",
                "ROLE_ACCUEIL"
        );
    }

    @Test
    void admin_shouldIncludeUserAndOrgWritePermissions() {
        assertThat(RolePermissionsCatalog.PERMISSIONS.get("ROLE_ADMIN"))
                .contains("users:write", "organisation:write", "tarifs:write", "audit:read");
    }

    @Test
    void caissier_shouldOnlyHaveEncaissementAndRecettesPermissions() {
        assertThat(RolePermissionsCatalog.PERMISSIONS.get("ROLE_CAISSIER"))
                .containsExactlyInAnyOrder("encaissements:read", "encaissements:write", "dashboard:recettes");
    }

    @Test
    void unknownRole_shouldNotBePresentInCatalog() {
        assertThat(RolePermissionsCatalog.PERMISSIONS.getOrDefault("ROLE_UNKNOWN", List.of())).isEmpty();
    }
}
