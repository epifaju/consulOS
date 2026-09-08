package com.consulos.dashboard.regional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Repository
@ConditionalOnProperty(prefix = "consulos.reseau", name = "enabled", havingValue = "true")
public class RegionalAlertsQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<UUID> dossiersBloquesEnRevue(Collection<UUID> orgIds) {
        if (isEmpty(orgIds)) {
            return Collections.emptyList();
        }
        String sql = """
                SELECT d.id FROM dossiers d
                WHERE d.organisation_id IN (:orgIds)
                  AND d.actif = true
                  AND d.statut = 'EN_REVUE'
                  AND d.updated_at < NOW() - INTERVAL '7 days'
                """;
        return mapUuidList(entityManager.createNativeQuery(sql)
                .setParameter("orgIds", orgIds)
                .getResultList());
    }

    @SuppressWarnings("unchecked")
    public List<UUID> dossiersEcheanceDepassee(Collection<UUID> orgIds) {
        if (isEmpty(orgIds)) {
            return Collections.emptyList();
        }
        String sql = """
                SELECT d.id FROM dossiers d
                WHERE d.organisation_id IN (:orgIds)
                  AND d.actif = true
                  AND d.date_echeance < NOW()
                  AND d.statut NOT IN ('CLOTURE', 'REJETE')
                """;
        return mapUuidList(entityManager.createNativeQuery(sql)
                .setParameter("orgIds", orgIds)
                .getResultList());
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> agentsSurcharges(Collection<UUID> orgIds) {
        if (isEmpty(orgIds)) {
            return Collections.emptyList();
        }
        String sql = """
                SELECT d.organisation_id, d.agent_id, COUNT(*)
                FROM dossiers d
                WHERE d.organisation_id IN (:orgIds)
                  AND d.actif = true
                  AND d.statut = 'EN_REVUE'
                  AND d.agent_id IS NOT NULL
                GROUP BY d.organisation_id, d.agent_id
                HAVING COUNT(*) > 20
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter("orgIds", orgIds)
                .getResultList();
    }

    /**
     * Postes avec activité dossiers ouverts mais recettes du jour à zéro.
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> postesCaisseSilencieuse(Collection<UUID> orgIds, LocalDate date) {
        if (isEmpty(orgIds)) {
            return Collections.emptyList();
        }
        String sql = """
                SELECT o.id, o.code
                FROM organisations o
                INNER JOIN (
                    SELECT organisation_id, COUNT(*) AS nb
                    FROM dossiers
                    WHERE organisation_id IN (:orgIds)
                      AND actif = true
                      AND statut IN ('SOUMIS', 'INCOMPLET', 'EN_REVUE', 'VALIDE')
                    GROUP BY organisation_id
                    HAVING COUNT(*) > 0
                ) d ON d.organisation_id = o.id
                LEFT JOIN (
                    SELECT organisation_id, COALESCE(SUM(montant_percu), 0) AS montant
                    FROM encaissements
                    WHERE organisation_id IN (:orgIds)
                      AND montant_percu > 0
                      AND created_at >= :debut
                      AND created_at < :fin
                    GROUP BY organisation_id
                ) e ON e.organisation_id = o.id
                WHERE o.id IN (:orgIds)
                  AND COALESCE(e.montant, 0) = 0
                ORDER BY o.code
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter("orgIds", orgIds)
                .setParameter("debut", date.atStartOfDay().atOffset(OffsetDateTime.now().getOffset()))
                .setParameter("fin", date.plusDays(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset()))
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<String> posteCodesForDossierIds(Collection<UUID> dossierIds) {
        if (isEmpty(dossierIds)) {
            return Collections.emptyList();
        }
        String sql = """
                SELECT DISTINCT o.code
                FROM dossiers d
                JOIN organisations o ON o.id = d.organisation_id
                WHERE d.id IN (:dossierIds)
                ORDER BY o.code
                """;
        List<Object> rows = entityManager.createNativeQuery(sql)
                .setParameter("dossierIds", dossierIds)
                .getResultList();
        List<String> codes = new ArrayList<>();
        for (Object row : rows) {
            codes.add(String.valueOf(row));
        }
        return codes;
    }

    private static List<UUID> mapUuidList(List<Object> rows) {
        List<UUID> ids = new ArrayList<>();
        for (Object row : rows) {
            ids.add((UUID) row);
        }
        return ids;
    }

    private static boolean isEmpty(Collection<?> values) {
        return values == null || values.isEmpty();
    }
}
