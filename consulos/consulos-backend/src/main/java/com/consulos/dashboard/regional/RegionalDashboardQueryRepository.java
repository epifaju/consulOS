package com.consulos.dashboard.regional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Repository
@ConditionalOnProperty(prefix = "consulos.reseau", name = "enabled", havingValue = "true")
public class RegionalDashboardQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public long countDossiersByStatuts(Collection<UUID> orgIds, List<String> statuts) {
        if (orgIds == null || orgIds.isEmpty()) {
            return 0L;
        }
        String sql = """
                SELECT COUNT(*) FROM dossiers d
                WHERE d.organisation_id IN (:orgIds)
                  AND d.actif = true
                  AND d.statut IN (:statuts)
                """;
        Number result = (Number) entityManager.createNativeQuery(sql)
                .setParameter("orgIds", orgIds)
                .setParameter("statuts", statuts)
                .getSingleResult();
        return result.longValue();
    }

    public long countDossiersEnRetard(Collection<UUID> orgIds) {
        if (orgIds == null || orgIds.isEmpty()) {
            return 0L;
        }
        String sql = """
                SELECT COUNT(*) FROM dossiers d
                WHERE d.organisation_id IN (:orgIds)
                  AND d.actif = true
                  AND d.date_echeance < NOW()
                  AND d.statut NOT IN ('CLOTURE', 'REJETE')
                """;
        return ((Number) entityManager.createNativeQuery(sql)
                .setParameter("orgIds", orgIds)
                .getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> rdvStatsForDay(Collection<UUID> orgIds, OffsetDateTime debut, OffsetDateTime fin) {
        if (orgIds == null || orgIds.isEmpty()) {
            return Collections.emptyList();
        }
        String sql = """
                SELECT r.statut, COUNT(*)
                FROM rendezvous r
                WHERE r.organisation_id IN (:orgIds)
                  AND r.date_heure >= :debut
                  AND r.date_heure < :fin
                GROUP BY r.statut
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter("orgIds", orgIds)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getResultList();
    }

    public BigDecimal sumRecettesJour(Collection<UUID> orgIds, LocalDate date) {
        if (orgIds == null || orgIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        String sql = """
                SELECT COALESCE(SUM(e.montant_percu), 0)
                FROM encaissements e
                WHERE e.organisation_id IN (:orgIds)
                  AND e.montant_percu > 0
                  AND e.created_at >= :debut
                  AND e.created_at < :fin
                """;
        Object result = entityManager.createNativeQuery(sql)
                .setParameter("orgIds", orgIds)
                .setParameter("debut", date.atStartOfDay().atOffset(OffsetDateTime.now().getOffset()))
                .setParameter("fin", date.plusDays(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset()))
                .getSingleResult();
        return new BigDecimal(result.toString());
    }

    public BigDecimal sumRecettesPeriode(Collection<UUID> orgIds, LocalDate debut, LocalDate fin) {
        if (orgIds == null || orgIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        String sql = """
                SELECT COALESCE(SUM(e.montant_percu), 0)
                FROM encaissements e
                WHERE e.organisation_id IN (:orgIds)
                  AND e.montant_percu > 0
                  AND e.created_at >= :debut
                  AND e.created_at < :fin
                """;
        Object result = entityManager.createNativeQuery(sql)
                .setParameter("orgIds", orgIds)
                .setParameter("debut", debut.atStartOfDay().atOffset(OffsetDateTime.now().getOffset()))
                .setParameter("fin", fin.plusDays(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset()))
                .getSingleResult();
        return new BigDecimal(result.toString());
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> postesKpis(
            Collection<UUID> orgIds,
            OffsetDateTime rdvDebut,
            OffsetDateTime rdvFin,
            LocalDate recettesDate
    ) {
        if (orgIds == null || orgIds.isEmpty()) {
            return Collections.emptyList();
        }
        String sql = """
                SELECT o.id, o.code, o.nom,
                       COALESCE(dossiers.ouverts, 0) AS dossiers_ouverts,
                       COALESCE(dossiers.en_revue, 0) AS en_revue,
                       COALESCE(rdvs.nb, 0) AS rdv_jour,
                       COALESCE(encaissements.montant, 0) AS recettes_jour
                FROM organisations o
                LEFT JOIN (
                    SELECT organisation_id,
                           COUNT(*) FILTER (WHERE statut IN ('BROUILLON','SOUMIS','INCOMPLET','EN_REVUE','VALIDE')) AS ouverts,
                           COUNT(*) FILTER (WHERE statut = 'EN_REVUE') AS en_revue
                    FROM dossiers
                    WHERE organisation_id IN (:orgIds)
                      AND actif = true
                    GROUP BY organisation_id
                ) dossiers ON dossiers.organisation_id = o.id
                LEFT JOIN (
                    SELECT organisation_id, COUNT(*) AS nb
                    FROM rendezvous
                    WHERE organisation_id IN (:orgIds)
                      AND date_heure >= :rdvDebut
                      AND date_heure < :rdvFin
                    GROUP BY organisation_id
                ) rdvs ON rdvs.organisation_id = o.id
                LEFT JOIN (
                    SELECT organisation_id, COALESCE(SUM(montant_percu), 0) AS montant
                    FROM encaissements
                    WHERE organisation_id IN (:orgIds)
                      AND montant_percu > 0
                      AND created_at >= :recettesDebut
                      AND created_at < :recettesFin
                    GROUP BY organisation_id
                ) encaissements ON encaissements.organisation_id = o.id
                WHERE o.id IN (:orgIds)
                ORDER BY o.code
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter("orgIds", orgIds)
                .setParameter("rdvDebut", rdvDebut)
                .setParameter("rdvFin", rdvFin)
                .setParameter("recettesDebut", recettesDate.atStartOfDay().atOffset(OffsetDateTime.now().getOffset()))
                .setParameter("recettesFin", recettesDate.plusDays(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset()))
                .getResultList();
    }
}
