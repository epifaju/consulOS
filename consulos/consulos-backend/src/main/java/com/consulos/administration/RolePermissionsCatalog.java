package com.consulos.administration;

import java.util.List;
import java.util.Map;

public final class RolePermissionsCatalog {

    private RolePermissionsCatalog() {
    }

    public static final Map<String, List<String>> PERMISSIONS = Map.of(
            "ROLE_ADMIN", List.of(
                    "users:read", "users:write", "roles:read", "organisation:write",
                    "audit:read", "tarifs:write", "parametres:write", "dashboard:read"
            ),
            "ROLE_SUPERVISEUR", List.of(
                    "dossiers:read", "dossiers:validate", "dashboard:read", "audit:read"
            ),
            "ROLE_AGENT", List.of(
                    "dossiers:read", "dossiers:write", "usagers:read", "usagers:write", "documents:write"
            ),
            "ROLE_CAISSIER", List.of(
                    "encaissements:read", "encaissements:write", "dashboard:recettes"
            ),
            "ROLE_ACCUEIL", List.of(
                    "usagers:read", "rendezvous:read", "rendezvous:write"
            )
    );
}
