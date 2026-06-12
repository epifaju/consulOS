package com.consulos.encaissement;

import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class EncaissementSpecifications {

    private EncaissementSpecifications() {
    }

    public static Specification<EncaissementEntity> forOrganisation(UUID organisationId) {
        return (root, query, cb) -> cb.equal(root.get("organisationId"), organisationId);
    }

    public static Specification<EncaissementEntity> withStatut(StatutEncaissement statut) {
        if (statut == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("statut"), statut);
    }

    public static Specification<EncaissementEntity> withModePaiement(ModePaiement mode) {
        if (mode == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("modePaiement"), mode);
    }

    public static Specification<EncaissementEntity> withCaissier(UUID caissierId) {
        if (caissierId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("caissierId"), caissierId);
    }

    public static Specification<EncaissementEntity> betweenDates(OffsetDateTime debut, OffsetDateTime fin) {
        if (debut == null && fin == null) {
            return null;
        }
        return (root, query, cb) -> {
            if (debut != null && fin != null) {
                return cb.between(root.get("createdAt"), debut, fin);
            }
            if (debut != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), debut);
            }
            return cb.lessThanOrEqualTo(root.get("createdAt"), fin);
        };
    }
}
