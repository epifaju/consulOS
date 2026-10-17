package com.consulos.rbac;

import java.util.List;
import java.util.Map;

/**
 * Matrice RBAC unique (Vague 3) — alignée sur les {@code @PreAuthorize} réels.
 * Remplace fonctionnellement {@code RolePermissionsCatalog} et
 * {@code MvpRoleAuthorizationReference} pour l'UI admin et le frontend.
 */
public final class MvpRbacMatrix {

    private MvpRbacMatrix() {
    }

    public static final Map<String, List<String>> PERMISSIONS = Map.of(
            "ROLE_ADMIN", List.of(
                    "usagers:read", "usagers:write", "usagers:delete", "usagers:rgpd-export",
                    "dossiers:read", "dossiers:write", "dossiers:validate", "dossiers:assign",
                    "documents:read", "documents:write", "documents:delete",
                    "rendezvous:read", "rendezvous:write",
                    "encaissements:read", "encaissements:write", "encaissements:export",
                    "dashboard:read", "dashboard:recettes",
                    "reporting:read", "reseau:read", "reseau:write",
                    "admin:read", "admin:write", "audit:read",
                    "tarifs:write", "parametres:write",
                    "notifications:read", "ocr:read"
            ),
            "ROLE_SUPERVISEUR", List.of(
                    "usagers:read", "usagers:rgpd-export",
                    "dossiers:read", "dossiers:validate", "dossiers:assign",
                    "documents:read", "documents:write",
                    "rendezvous:read",
                    "encaissements:read", "encaissements:export",
                    "dashboard:read", "dashboard:recettes",
                    "reporting:read", "reseau:read",
                    "audit:read", "parametres:read",
                    "notifications:read", "ocr:read"
            ),
            "ROLE_AGENT", List.of(
                    "usagers:read", "usagers:write", "usagers:rgpd-export",
                    "dossiers:read", "dossiers:write", "dossiers:assign",
                    "documents:read", "documents:write",
                    "rendezvous:read", "rendezvous:write",
                    "encaissements:read", "encaissements:write",
                    "dashboard:limited",
                    "notifications:read", "ocr:read"
            ),
            "ROLE_CAISSIER", List.of(
                    "usagers:read",
                    "dossiers:read",
                    "documents:read",
                    "encaissements:read", "encaissements:write", "encaissements:export",
                    "dashboard:recettes",
                    "notifications:read"
            ),
            "ROLE_ACCUEIL", List.of(
                    "usagers:read",
                    "dossiers:read",
                    "rendezvous:read", "rendezvous:write",
                    "notifications:read"
            ),
            "ROLE_ADMIN_RESEAU", List.of(
                    "dashboard:read",
                    "reporting:read",
                    "reseau:read", "reseau:write",
                    "notifications:read"
            )
    );

    public static final Map<String, List<String>> NAV = Map.of(
            "/", List.of("ROLE_ADMIN", "ROLE_SUPERVISEUR", "ROLE_AGENT", "ROLE_CAISSIER", "ROLE_ACCUEIL", "ROLE_ADMIN_RESEAU"),
            "/usagers", List.of("ROLE_ADMIN", "ROLE_SUPERVISEUR", "ROLE_AGENT", "ROLE_CAISSIER", "ROLE_ACCUEIL"),
            "/dossiers", List.of("ROLE_ADMIN", "ROLE_SUPERVISEUR", "ROLE_AGENT", "ROLE_CAISSIER", "ROLE_ACCUEIL"),
            "/rendezvous", List.of("ROLE_ADMIN", "ROLE_SUPERVISEUR", "ROLE_AGENT", "ROLE_ACCUEIL"),
            "/documents", List.of("ROLE_ADMIN", "ROLE_SUPERVISEUR", "ROLE_AGENT", "ROLE_CAISSIER"),
            "/caisse", List.of("ROLE_ADMIN", "ROLE_SUPERVISEUR", "ROLE_CAISSIER"),
            "/reporting", List.of("ROLE_ADMIN", "ROLE_SUPERVISEUR"),
            "/regional", List.of("ROLE_ADMIN_RESEAU", "ROLE_ADMIN", "ROLE_SUPERVISEUR"),
            "/administration", List.of("ROLE_ADMIN")
    );

    public static String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "";
        }
        return role.startsWith("ROLE_") ? role : "ROLE_" + role;
    }

    public static List<String> permissionsFor(String role) {
        return PERMISSIONS.getOrDefault(normalizeRole(role), List.of());
    }

    public static boolean hasPermission(String role, String permission) {
        return permissionsFor(role).contains(permission);
    }

    public static boolean canAccessNav(String path, String role) {
        List<String> allowed = NAV.get(path);
        return allowed != null && allowed.contains(normalizeRole(role));
    }
}
