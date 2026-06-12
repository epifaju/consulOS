package com.consulos.usager.rgpd;

import com.consulos.document.DocumentEntity;
import com.consulos.encaissement.EncaissementEntity;
import com.consulos.rendezvous.RendezvousEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UsagerRgpdRelatedDataRepository extends JpaRepository<EncaissementEntity, UUID> {

    @Query("""
            SELECT e FROM EncaissementEntity e
            WHERE e.organisationId = :organisationId AND e.usagerId = :usagerId
            ORDER BY e.createdAt DESC
            """)
    List<EncaissementEntity> findEncaissementsByUsager(
            @Param("organisationId") UUID organisationId,
            @Param("usagerId") UUID usagerId
    );

    @Query("""
            SELECT r FROM RendezvousEntity r
            WHERE r.organisationId = :organisationId AND r.usagerId = :usagerId
            ORDER BY r.dateHeure DESC
            """)
    List<RendezvousEntity> findRendezvousByUsager(
            @Param("organisationId") UUID organisationId,
            @Param("usagerId") UUID usagerId
    );

    @Query("""
            SELECT d FROM DocumentEntity d
            WHERE d.organisationId = :organisationId
              AND d.usagerId = :usagerId
              AND d.actif = true
            ORDER BY d.createdAt DESC
            """)
    List<DocumentEntity> findDocumentsByUsager(
            @Param("organisationId") UUID organisationId,
            @Param("usagerId") UUID usagerId
    );
}
