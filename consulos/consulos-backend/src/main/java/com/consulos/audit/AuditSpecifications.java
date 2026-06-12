package com.consulos.audit;

import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class AuditSpecifications {

    private AuditSpecifications() {
    }

    public static Specification<AuditLogEntity> forOrganisation(UUID organisationId) {
        return (root, query, cb) -> cb.equal(root.get("organisationId"), organisationId);
    }

    public static Specification<AuditLogEntity> withAction(String action) {
        if (action == null || action.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("action"), action);
    }

    public static Specification<AuditLogEntity> withEntiteType(String entiteType) {
        if (entiteType == null || entiteType.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("entiteType"), entiteType);
    }

    public static Specification<AuditLogEntity> withUserId(UUID userId) {
        if (userId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    public static Specification<AuditLogEntity> betweenDates(OffsetDateTime debut, OffsetDateTime fin) {
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
