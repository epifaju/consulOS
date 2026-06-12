package com.consulos.encaissement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EncaissementRepository extends JpaRepository<EncaissementEntity, UUID>, JpaSpecificationExecutor<EncaissementEntity> {

    Optional<EncaissementEntity> findByIdAndOrganisationId(UUID id, UUID organisationId);

    @Query(value = "SELECT nextval('encaissement_seq')", nativeQuery = true)
    Long nextEncaissementSequence();

    @Query("""
            SELECT e.modePaiement, COUNT(e), COALESCE(SUM(e.montantPercu), 0)
            FROM EncaissementEntity e
            WHERE e.organisationId = :organisationId
              AND e.createdAt >= :debut
              AND e.createdAt < :fin
              AND e.montantPercu > 0
              AND e.modePaiement IS NOT NULL
            GROUP BY e.modePaiement
            """)
    List<Object[]> sumByModeForDay(
            @Param("organisationId") UUID organisationId,
            @Param("debut") OffsetDateTime debut,
            @Param("fin") OffsetDateTime fin
    );
}
