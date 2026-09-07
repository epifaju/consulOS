package com.consulos.document.query;

import com.consulos.document.DocumentEntity;
import com.consulos.document.StatutControle;
import com.consulos.dossier.DossierEntity;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class DocumentSpecifications {

    private DocumentSpecifications() {
    }

    public static Specification<DocumentEntity> forOrganisation(UUID organisationId) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("organisationId"), organisationId),
                cb.equal(root.get("actif"), true)
        );
    }

    public static Specification<DocumentEntity> withTypeDocument(String typeDocument) {
        if (!StringUtils.hasText(typeDocument)) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("typeDocument"), typeDocument);
    }

    public static Specification<DocumentEntity> withStatuts(List<StatutControle> statuts) {
        if (statuts == null || statuts.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> root.get("statutControle").in(statuts);
    }

    public static Specification<DocumentEntity> withSearch(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("nomOriginal")), pattern);
    }

    public static Specification<DocumentEntity> withDossierReference(String reference, UUID organisationId) {
        if (!StringUtils.hasText(reference)) {
            return null;
        }
        String pattern = "%" + reference.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            Subquery<UUID> subquery = query.subquery(UUID.class);
            var dossierRoot = subquery.from(DossierEntity.class);
            subquery.select(dossierRoot.get("id"));
            subquery.where(
                    cb.equal(dossierRoot.get("organisationId"), organisationId),
                    cb.equal(dossierRoot.get("actif"), true),
                    cb.like(cb.lower(dossierRoot.get("reference")), pattern)
            );
            return root.get("dossierId").in(subquery);
        };
    }

    public static Specification<DocumentEntity> withCreatedBetween(OffsetDateTime debut, OffsetDateTime fin) {
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
