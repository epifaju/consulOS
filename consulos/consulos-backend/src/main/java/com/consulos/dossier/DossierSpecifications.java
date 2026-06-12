package com.consulos.dossier;

import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class DossierSpecifications {

    private DossierSpecifications() {
    }

    public static Specification<DossierEntity> forOrganisation(UUID organisationId) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("organisationId"), organisationId),
                cb.equal(root.get("actif"), true)
        );
    }

    public static Specification<DossierEntity> withStatuts(List<DossierStatut> statuts) {
        if (statuts == null || statuts.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> root.get("statut").in(statuts);
    }

    public static Specification<DossierEntity> withTypeService(UUID typeServiceId) {
        if (typeServiceId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("typeServiceId"), typeServiceId);
    }

    public static Specification<DossierEntity> withAgent(UUID agentId) {
        if (agentId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("agentId"), agentId);
    }

    public static Specification<DossierEntity> withUsager(UUID usagerId) {
        if (usagerId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("usagerId"), usagerId);
    }

    public static Specification<DossierEntity> withPriorite(DossierPriorite priorite) {
        if (priorite == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("priorite"), priorite);
    }

    public static Specification<DossierEntity> withCreatedBetween(OffsetDateTime debut, OffsetDateTime fin) {
        if (debut == null && fin == null) {
            return null;
        }
        return (root, query, cb) -> {
            if (debut != null && fin != null) {
                return cb.between(root.get("createdAt"), debut.toLocalDateTime(), fin.toLocalDateTime());
            }
            if (debut != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), debut.toLocalDateTime());
            }
            return cb.lessThanOrEqualTo(root.get("createdAt"), fin.toLocalDateTime());
        };
    }
}
