package com.consulos.rendezvous;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RendezvousRepository extends JpaRepository<RendezvousEntity, UUID>, JpaSpecificationExecutor<RendezvousEntity> {

    Optional<RendezvousEntity> findByIdAndOrganisationId(UUID id, UUID organisationId);

    @Query("""
            SELECT COUNT(r) FROM RendezvousEntity r
            WHERE r.organisationId = :organisationId
              AND r.dateHeure >= :slotStart
              AND r.dateHeure < :slotEnd
              AND r.statut NOT IN ('ANNULE', 'REPORTE')
            """)
    long countActiveInSlot(
            @Param("organisationId") UUID organisationId,
            @Param("slotStart") OffsetDateTime slotStart,
            @Param("slotEnd") OffsetDateTime slotEnd
    );

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM RendezvousEntity r
            WHERE r.agentId = :agentId
              AND r.dateHeure = :dateHeure
              AND r.statut NOT IN ('ANNULE', 'REPORTE')
            """)
    boolean existsByAgentIdAndDateHeureAndStatutNotIn(
            @Param("agentId") UUID agentId,
            @Param("dateHeure") OffsetDateTime dateHeure
    );

    @Query("""
            SELECT r FROM RendezvousEntity r
            WHERE r.organisationId = :organisationId
              AND r.dateHeure >= :debut
              AND r.dateHeure < :fin
              AND (:agentId IS NULL OR r.agentId = :agentId)
            ORDER BY r.dateHeure
            """)
    List<RendezvousEntity> findAgenda(
            @Param("organisationId") UUID organisationId,
            @Param("debut") OffsetDateTime debut,
            @Param("fin") OffsetDateTime fin,
            @Param("agentId") UUID agentId
    );

    @Query("""
            SELECT r FROM RendezvousEntity r
            WHERE r.statut = 'CONFIRME'
              AND r.rappelEnvoye = false
              AND r.dateHeure >= :debut
              AND r.dateHeure < :fin
            """)
    List<RendezvousEntity> findPendingReminders(
            @Param("debut") OffsetDateTime debut,
            @Param("fin") OffsetDateTime fin
    );
}
