package com.consulos.security;

import java.util.List;
import java.util.Map;

/**
 * Référence MVP des autorisations par rôle (alignée sur les @PreAuthorize des contrôleurs).
 * Ne remplace pas {@link com.consulos.administration.RolePermissionsCatalog} legacy.
 */
public final class MvpRoleAuthorizationReference {

    private MvpRoleAuthorizationReference() {
    }

    public static final Map<String, List<String>> ROLE_CAPABILITIES = Map.of(
            "ROLE_ADMIN", List.of(
                    "usagers:read", "usagers:write", "usagers:delete", "usagers:rgpd-export",
                    "dossiers:read", "dossiers:write", "dossiers:validate",
                    "documents:read", "documents:write",
                    "rendezvous:read", "rendezvous:write",
                    "encaissements:read", "encaissements:write", "encaissements:export",
                    "dashboard:read", "admin:read", "admin:write", "audit:read", "tarifs:write"
            ),
            "ROLE_SUPERVISEUR", List.of(
                    "usagers:read", "usagers:rgpd-export",
                    "dossiers:read", "dossiers:validate",
                    "documents:read",
                    "rendezvous:read",
                    "encaissements:read", "encaissements:export",
                    "dashboard:read", "audit:read"
            ),
            "ROLE_AGENT", List.of(
                    "usagers:read", "usagers:write", "usagers:rgpd-export",
                    "dossiers:read", "dossiers:write",
                    "documents:read", "documents:write",
                    "rendezvous:read", "rendezvous:write",
                    "encaissements:read", "encaissements:write",
                    "dashboard:limited"
            ),
            "ROLE_CAISSIER", List.of(
                    "usagers:read",
                    "dossiers:read",
                    "documents:read",
                    "encaissements:read", "encaissements:write", "encaissements:export",
                    "dashboard:recettes"
            ),
            "ROLE_ACCUEIL", List.of(
                    "usagers:read",
                    "dossiers:read",
                    "rendezvous:read", "rendezvous:write"
            )
    );
}
