package com.consulos.reporting;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class ReportingQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public long countDossiersCrees(UUID organisationId, OffsetDateTime debut, OffsetDateTime fin) {
        String sql = """
                SELECT COUNT(*) FROM dossiers d
                WHERE d.organisation_id = :orgId
                  AND d.actif = true
                  AND d.created_at >= :debut
                  AND d.created_at < :fin
                """;
        return ((Number) entityManager.createNativeQuery(sql)
                .setParameter("orgId", organisationId)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> dossiersCreesParStatut(UUID organisationId, OffsetDateTime debut, OffsetDateTime fin) {
        String sql = """
                SELECT d.statut, COUNT(*)
                FROM dossiers d
                WHERE d.organisation_id = :orgId
                  AND d.actif = true
                  AND d.created_at >= :debut
                  AND d.created_at < :fin
                GROUP BY d.statut
                ORDER BY d.statut
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter("orgId", organisationId)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getResultList();
    }

    public long countDocuments(UUID organisationId, OffsetDateTime debut, OffsetDateTime fin) {
        String sql = """
                SELECT COUNT(*) FROM documents doc
                WHERE doc.organisation_id = :orgId
                  AND doc.actif = true
                  AND doc.created_at >= :debut
                  AND doc.created_at < :fin
                """;
        return ((Number) entityManager.createNativeQuery(sql)
                .setParameter("orgId", organisationId)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getSingleResult()).longValue();
    }

    public BigDecimal avgDelaiHeures(UUID organisationId, OffsetDateTime debut, OffsetDateTime fin) {
        String sql = """
                SELECT AVG(EXTRACT(EPOCH FROM (d.updated_at - d.date_soumission)) / 3600)
                FROM dossiers d
                WHERE d.organisation_id = :orgId
                  AND d.actif = true
                  AND d.date_soumission IS NOT NULL
                  AND d.created_at >= :debut
                  AND d.created_at < :fin
                """;
        Object result = entityManager.createNativeQuery(sql)
                .setParameter("orgId", organisationId)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getSingleResult();
        if (result == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(result.toString());
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> encaissementsParMode(UUID organisationId, OffsetDateTime debut, OffsetDateTime fin) {
        String sql = """
                SELECT COALESCE(e.mode_paiement, 'INCONNU'), COUNT(*), COALESCE(SUM(e.montant_percu), 0)
                FROM encaissements e
                WHERE e.organisation_id = :orgId
                  AND e.created_at >= :debut
                  AND e.created_at < :fin
                GROUP BY e.mode_paiement
                ORDER BY 3 DESC
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter("orgId", organisationId)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> encaissementsParService(UUID organisationId, OffsetDateTime debut, OffsetDateTime fin) {
        String sql = """
                SELECT COALESCE(ts.code, 'AUTRE'), COALESCE(SUM(e.montant_percu), 0), COUNT(*)
                FROM encaissements e
                LEFT JOIN dossiers d ON d.id = e.dossier_id
                LEFT JOIN types_service ts ON ts.id = d.type_service_id
                WHERE e.organisation_id = :orgId
                  AND e.created_at >= :debut
                  AND e.created_at < :fin
                GROUP BY ts.code
                ORDER BY 2 DESC
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter("orgId", organisationId)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getResultList();
    }

    public Object[] encaissementTotaux(UUID organisationId, OffsetDateTime debut, OffsetDateTime fin) {
        String sql = """
                SELECT COALESCE(SUM(e.montant_percu), 0), COUNT(*)
                FROM encaissements e
                WHERE e.organisation_id = :orgId
                  AND e.created_at >= :debut
                  AND e.created_at < :fin
                """;
        return (Object[]) entityManager.createNativeQuery(sql)
                .setParameter("orgId", organisationId)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> rdvParStatut(UUID organisationId, OffsetDateTime debut, OffsetDateTime fin) {
        String sql = """
                SELECT r.statut, COUNT(*)
                FROM rendezvous r
                WHERE r.organisation_id = :orgId
                  AND r.date_heure >= :debut
                  AND r.date_heure < :fin
                GROUP BY r.statut
                ORDER BY r.statut
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter("orgId", organisationId)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> listDossiersCsv(UUID organisationId, OffsetDateTime debut, OffsetDateTime fin) {
        String sql = """
                SELECT d.reference, d.statut, COALESCE(ts.code, ''), d.created_at,
                       COALESCE(u.prenom, ''), COALESCE(u.nom, '')
                FROM dossiers d
                LEFT JOIN types_service ts ON ts.id = d.type_service_id
                LEFT JOIN usagers u ON u.id = d.usager_id
                WHERE d.organisation_id = :orgId
                  AND d.actif = true
                  AND d.created_at >= :debut
                  AND d.created_at < :fin
                ORDER BY d.created_at DESC
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter("orgId", organisationId)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> listRdvCsv(UUID organisationId, OffsetDateTime debut, OffsetDateTime fin) {
        String sql = """
                SELECT r.date_heure, r.statut, COALESCE(ts.code, ''),
                       COALESCE(u.prenom, ''), COALESCE(u.nom, ''),
                       COALESCE(ag.prenom, ''), COALESCE(ag.nom, '')
                FROM rendezvous r
                LEFT JOIN types_service ts ON ts.id = r.type_service_id
                LEFT JOIN usagers u ON u.id = r.usager_id
                LEFT JOIN users ag ON ag.id = r.agent_id
                WHERE r.organisation_id = :orgId
                  AND r.date_heure >= :debut
                  AND r.date_heure < :fin
                ORDER BY r.date_heure
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter("orgId", organisationId)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getResultList();
    }
}
