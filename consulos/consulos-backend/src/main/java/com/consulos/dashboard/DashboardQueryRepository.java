package com.consulos.dashboard;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class DashboardQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public long countDossiersByStatuts(UUID organisationId, List<String> statuts) {
        String sql = """
                SELECT COUNT(*) FROM dossiers d
                WHERE d.organisation_id = :orgId
                  AND d.actif = true
                  AND d.statut IN (:statuts)
                """;
        Number result = (Number) entityManager.createNativeQuery(sql)
                .setParameter("orgId", organisationId)
                .setParameter("statuts", statuts)
                .getSingleResult();
        return result.longValue();
    }

    public BigDecimal avgDelaiHeures(UUID organisationId, LocalDate debut, LocalDate fin) {
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
                .setParameter("debut", debut.atStartOfDay().atOffset(OffsetDateTime.now().getOffset()))
                .setParameter("fin", fin.plusDays(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset()))
                .getSingleResult();
        if (result == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(result.toString());
    }

    public long countDossiersEnRetard(UUID organisationId) {
        String sql = """
                SELECT COUNT(*) FROM dossiers d
                WHERE d.organisation_id = :orgId
                  AND d.actif = true
                  AND d.date_echeance < NOW()
                  AND d.statut NOT IN ('CLOTURE', 'REJETE')
                """;
        return ((Number) entityManager.createNativeQuery(sql)
                .setParameter("orgId", organisationId)
                .getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> dossiersParJour(UUID organisationId, LocalDate debut, LocalDate fin) {
        String sql = """
                SELECT DATE(d.created_at) AS jour, COUNT(*)
                FROM dossiers d
                WHERE d.organisation_id = :orgId
                  AND d.actif = true
                  AND d.created_at >= :debut
                  AND d.created_at < :fin
                GROUP BY DATE(d.created_at)
                ORDER BY jour
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter("orgId", organisationId)
                .setParameter("debut", debut.atStartOfDay().atOffset(OffsetDateTime.now().getOffset()))
                .setParameter("fin", fin.plusDays(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset()))
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> recettesParService(UUID organisationId, LocalDate debut, LocalDate fin) {
        String sql = """
                SELECT COALESCE(ts.code, 'AUTRE'), COALESCE(SUM(e.montant_percu), 0), COUNT(*)
                FROM encaissements e
                LEFT JOIN dossiers d ON d.id = e.dossier_id
                LEFT JOIN types_service ts ON ts.id = d.type_service_id
                WHERE e.organisation_id = :orgId
                  AND e.montant_percu > 0
                  AND e.created_at >= :debut
                  AND e.created_at < :fin
                GROUP BY ts.code
                ORDER BY 2 DESC
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter("orgId", organisationId)
                .setParameter("debut", debut.atStartOfDay().atOffset(OffsetDateTime.now().getOffset()))
                .setParameter("fin", fin.plusDays(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset()))
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> rdvStatsForDay(UUID organisationId, OffsetDateTime debut, OffsetDateTime fin) {
        String sql = """
                SELECT r.statut, COUNT(*)
                FROM rendezvous r
                WHERE r.organisation_id = :orgId
                  AND r.date_heure >= :debut
                  AND r.date_heure < :fin
                GROUP BY r.statut
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter("orgId", organisationId)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<UUID> dossiersBloquesEnRevue(UUID organisationId) {
        String sql = """
                SELECT d.id FROM dossiers d
                WHERE d.organisation_id = :orgId
                  AND d.actif = true
                  AND d.statut = 'EN_REVUE'
                  AND d.updated_at < NOW() - INTERVAL '7 days'
                """;
        List<Object> rows = entityManager.createNativeQuery(sql)
                .setParameter("orgId", organisationId)
                .getResultList();
        List<UUID> ids = new ArrayList<>();
        for (Object row : rows) {
            ids.add((UUID) row);
        }
        return ids;
    }

    @SuppressWarnings("unchecked")
    public List<UUID> dossiersEcheanceDepassee(UUID organisationId) {
        String sql = """
                SELECT d.id FROM dossiers d
                WHERE d.organisation_id = :orgId
                  AND d.actif = true
                  AND d.date_echeance < NOW()
                  AND d.statut NOT IN ('CLOTURE', 'REJETE')
                """;
        List<Object> rows = entityManager.createNativeQuery(sql)
                .setParameter("orgId", organisationId)
                .getResultList();
        List<UUID> ids = new ArrayList<>();
        for (Object row : rows) {
            ids.add((UUID) row);
        }
        return ids;
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> agentsSurcharges(UUID organisationId) {
        String sql = """
                SELECT d.agent_id, COUNT(*)
                FROM dossiers d
                WHERE d.organisation_id = :orgId
                  AND d.actif = true
                  AND d.statut = 'EN_REVUE'
                  AND d.agent_id IS NOT NULL
                GROUP BY d.agent_id
                HAVING COUNT(*) > 20
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter("orgId", organisationId)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> agentsCharge(UUID organisationId, OffsetDateTime rdvDebut, OffsetDateTime rdvFin) {
        String sql = """
                SELECT u.id, u.nom, u.prenom,
                       COALESCE(dossiers.nb, 0) AS nb_dossiers,
                       COALESCE(rdvs.nb, 0) AS nb_rdv
                FROM users u
                LEFT JOIN (
                    SELECT agent_id, COUNT(*) AS nb
                    FROM dossiers
                    WHERE organisation_id = :orgId
                      AND actif = true
                      AND statut IN ('SOUMIS', 'INCOMPLET', 'EN_REVUE', 'VALIDE')
                      AND agent_id IS NOT NULL
                    GROUP BY agent_id
                ) dossiers ON dossiers.agent_id = u.id
                LEFT JOIN (
                    SELECT agent_id, COUNT(*) AS nb
                    FROM rendezvous
                    WHERE organisation_id = :orgId
                      AND date_heure >= :rdvDebut
                      AND date_heure < :rdvFin
                      AND agent_id IS NOT NULL
                    GROUP BY agent_id
                ) rdvs ON rdvs.agent_id = u.id
                WHERE u.organisation_id = :orgId
                  AND u.actif = true
                  AND (COALESCE(dossiers.nb, 0) > 0 OR COALESCE(rdvs.nb, 0) > 0)
                ORDER BY nb_dossiers DESC, nb_rdv DESC
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter("orgId", organisationId)
                .setParameter("rdvDebut", rdvDebut)
                .setParameter("rdvFin", rdvFin)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> dossierChartData(UUID organisationId, LocalDate debut, LocalDate fin) {
        String sql = """
                SELECT ts.code, d.statut, COUNT(*)
                FROM dossiers d
                JOIN types_service ts ON ts.id = d.type_service_id
                WHERE d.organisation_id = :orgId
                  AND d.actif = true
                  AND d.created_at >= :debut
                  AND d.created_at < :fin
                GROUP BY ts.code, d.statut
                ORDER BY ts.code, d.statut
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter("orgId", organisationId)
                .setParameter("debut", debut.atStartOfDay().atOffset(OffsetDateTime.now().getOffset()))
                .setParameter("fin", fin.plusDays(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset()))
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> recetteChartData(UUID organisationId, LocalDate debut, LocalDate fin) {
        String sql = """
                SELECT DATE(e.created_at), e.mode_paiement, COALESCE(SUM(e.montant_percu), 0)
                FROM encaissements e
                WHERE e.organisation_id = :orgId
                  AND e.montant_percu > 0
                  AND e.mode_paiement IS NOT NULL
                  AND e.created_at >= :debut
                  AND e.created_at < :fin
                GROUP BY DATE(e.created_at), e.mode_paiement
                ORDER BY DATE(e.created_at), e.mode_paiement
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter("orgId", organisationId)
                .setParameter("debut", debut.atStartOfDay().atOffset(OffsetDateTime.now().getOffset()))
                .setParameter("fin", fin.plusDays(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset()))
                .getResultList();
    }

    public BigDecimal sumRecettesJour(UUID organisationId, LocalDate date) {
        String sql = """
                SELECT COALESCE(SUM(e.montant_percu), 0)
                FROM encaissements e
                WHERE e.organisation_id = :orgId
                  AND e.montant_percu > 0
                  AND e.created_at >= :debut
                  AND e.created_at < :fin
                """;
        Object result = entityManager.createNativeQuery(sql)
                .setParameter("orgId", organisationId)
                .setParameter("debut", date.atStartOfDay().atOffset(OffsetDateTime.now().getOffset()))
                .setParameter("fin", date.plusDays(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset()))
                .getSingleResult();
        return new BigDecimal(result.toString());
    }

    public void refreshMaterializedView() {
        entityManager.createNativeQuery("REFRESH MATERIALIZED VIEW CONCURRENTLY dashboard_daily_stats").executeUpdate();
    }
}
