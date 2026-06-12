package com.consulos.rendezvous;

import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class RdvSpecifications {

    private RdvSpecifications() {
    }

    public static Specification<RendezvousEntity> forOrganisation(UUID organisationId) {
        return (root, query, cb) -> cb.equal(root.get("organisationId"), organisationId);
    }

    public static Specification<RendezvousEntity> withStatut(RdvStatut statut) {
        if (statut == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("statut"), statut);
    }

    public static Specification<RendezvousEntity> withAgent(UUID agentId) {
        if (agentId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("agentId"), agentId);
    }

    public static Specification<RendezvousEntity> withUsager(UUID usagerId) {
        if (usagerId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("usagerId"), usagerId);
    }

    public static Specification<RendezvousEntity> withTypeService(UUID typeServiceId) {
        if (typeServiceId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("typeServiceId"), typeServiceId);
    }

    public static Specification<RendezvousEntity> betweenDates(OffsetDateTime debut, OffsetDateTime fin) {
        if (debut == null && fin == null) {
            return null;
        }
        return (root, query, cb) -> {
            if (debut != null && fin != null) {
                return cb.between(root.get("dateHeure"), debut, fin);
            }
            if (debut != null) {
                return cb.greaterThanOrEqualTo(root.get("dateHeure"), debut);
            }
            return cb.lessThanOrEqualTo(root.get("dateHeure"), fin);
        };
    }
}
