package com.consulos.reporting.regional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Repository
@ConditionalOnProperty(prefix = "consulos.reseau", name = "enabled", havingValue = "true")
public class RegionalReportingQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public long countDossiersCrees(Collection<UUID> orgIds, OffsetDateTime debut, OffsetDateTime fin) {
        if (isEmpty(orgIds)) {
            return 0L;
        }
        String sql = """
                SELECT COUNT(*) FROM dossiers d
                WHERE d.organisation_id IN (:orgIds)
                  AND d.actif = true
                  AND d.created_at >= :debut
                  AND d.created_at < :fin
                """;
        return ((Number) entityManager.createNativeQuery(sql)
                .setParameter("orgIds", orgIds)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> dossiersCreesParStatut(Collection<UUID> orgIds, OffsetDateTime debut, OffsetDateTime fin) {
        if (isEmpty(orgIds)) {
            return Collections.emptyList();
        }
        String sql = """
                SELECT d.statut, COUNT(*)
                FROM dossiers d
                WHERE d.organisation_id IN (:orgIds)
                  AND d.actif = true
                  AND d.created_at >= :debut
                  AND d.created_at < :fin
                GROUP BY d.statut
                ORDER BY d.statut
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter("orgIds", orgIds)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getResultList();
    }

    public long countDocuments(Collection<UUID> orgIds, OffsetDateTime debut, OffsetDateTime fin) {
        if (isEmpty(orgIds)) {
            return 0L;
        }
        String sql = """
                SELECT COUNT(*) FROM documents doc
                WHERE doc.organisation_id IN (:orgIds)
                  AND doc.actif = true
                  AND doc.created_at >= :debut
                  AND doc.created_at < :fin
                """;
        return ((Number) entityManager.createNativeQuery(sql)
                .setParameter("orgIds", orgIds)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getSingleResult()).longValue();
    }

    public BigDecimal avgDelaiHeures(Collection<UUID> orgIds, OffsetDateTime debut, OffsetDateTime fin) {
        if (isEmpty(orgIds)) {
            return BigDecimal.ZERO;
        }
        String sql = """
                SELECT AVG(EXTRACT(EPOCH FROM (d.updated_at - d.date_soumission)) / 3600)
                FROM dossiers d
                WHERE d.organisation_id IN (:orgIds)
                  AND d.actif = true
                  AND d.date_soumission IS NOT NULL
                  AND d.created_at >= :debut
                  AND d.created_at < :fin
                """;
        Object result = entityManager.createNativeQuery(sql)
                .setParameter("orgIds", orgIds)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getSingleResult();
        if (result == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(result.toString());
    }

    public Object[] encaissementTotaux(Collection<UUID> orgIds, OffsetDateTime debut, OffsetDateTime fin) {
        if (isEmpty(orgIds)) {
            return new Object[]{BigDecimal.ZERO, 0L};
        }
        String sql = """
                SELECT COALESCE(SUM(e.montant_percu), 0), COUNT(*)
                FROM encaissements e
                WHERE e.organisation_id IN (:orgIds)
                  AND e.created_at >= :debut
                  AND e.created_at < :fin
                """;
        return (Object[]) entityManager.createNativeQuery(sql)
                .setParameter("orgIds", orgIds)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> encaissementsParMode(Collection<UUID> orgIds, OffsetDateTime debut, OffsetDateTime fin) {
        if (isEmpty(orgIds)) {
            return Collections.emptyList();
        }
        String sql = """
                SELECT COALESCE(e.mode_paiement, 'INCONNU'), COUNT(*), COALESCE(SUM(e.montant_percu), 0)
                FROM encaissements e
                WHERE e.organisation_id IN (:orgIds)
                  AND e.created_at >= :debut
                  AND e.created_at < :fin
                GROUP BY e.mode_paiement
                ORDER BY 3 DESC
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter("orgIds", orgIds)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> encaissementsParService(Collection<UUID> orgIds, OffsetDateTime debut, OffsetDateTime fin) {
        if (isEmpty(orgIds)) {
            return Collections.emptyList();
        }
        String sql = """
                SELECT COALESCE(ts.code, 'AUTRE'), COALESCE(SUM(e.montant_percu), 0), COUNT(*)
                FROM encaissements e
                LEFT JOIN dossiers d ON d.id = e.dossier_id
                LEFT JOIN types_service ts ON ts.id = d.type_service_id
                WHERE e.organisation_id IN (:orgIds)
                  AND e.created_at >= :debut
                  AND e.created_at < :fin
                GROUP BY ts.code
                ORDER BY 2 DESC
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter("orgIds", orgIds)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> rdvParStatut(Collection<UUID> orgIds, OffsetDateTime debut, OffsetDateTime fin) {
        if (isEmpty(orgIds)) {
            return Collections.emptyList();
        }
        String sql = """
                SELECT r.statut, COUNT(*)
                FROM rendezvous r
                WHERE r.organisation_id IN (:orgIds)
                  AND r.date_heure >= :debut
                  AND r.date_heure < :fin
                GROUP BY r.statut
                ORDER BY r.statut
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter("orgIds", orgIds)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> postesMensuel(Collection<UUID> orgIds, OffsetDateTime debut, OffsetDateTime fin) {
        if (isEmpty(orgIds)) {
            return Collections.emptyList();
        }
        String sql = """
                SELECT o.id, o.code, o.nom,
                       COALESCE(dossiers.nb, 0) AS dossiers_crees,
                       COALESCE(encaissements.montant, 0) AS montant_percu,
                       COALESCE(encaissements.nb, 0) AS nb_encaissements,
                       COALESCE(rdvs.nb, 0) AS rdv_total
                FROM organisations o
                LEFT JOIN (
                    SELECT organisation_id, COUNT(*) AS nb
                    FROM dossiers
                    WHERE organisation_id IN (:orgIds)
                      AND actif = true
                      AND created_at >= :debut
                      AND created_at < :fin
                    GROUP BY organisation_id
                ) dossiers ON dossiers.organisation_id = o.id
                LEFT JOIN (
                    SELECT organisation_id,
                           COALESCE(SUM(montant_percu), 0) AS montant,
                           COUNT(*) AS nb
                    FROM encaissements
                    WHERE organisation_id IN (:orgIds)
                      AND created_at >= :debut
                      AND created_at < :fin
                    GROUP BY organisation_id
                ) encaissements ON encaissements.organisation_id = o.id
                LEFT JOIN (
                    SELECT organisation_id, COUNT(*) AS nb
                    FROM rendezvous
                    WHERE organisation_id IN (:orgIds)
                      AND date_heure >= :debut
                      AND date_heure < :fin
                    GROUP BY organisation_id
                ) rdvs ON rdvs.organisation_id = o.id
                WHERE o.id IN (:orgIds)
                ORDER BY o.code
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter("orgIds", orgIds)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getResultList();
    }

    private static boolean isEmpty(Collection<UUID> orgIds) {
        return orgIds == null || orgIds.isEmpty();
    }
}
