package com.consulos.usager.query;

import com.consulos.document.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UsagerDocumentQueryRepository extends JpaRepository<DocumentEntity, UUID> {

    @Query("""
            SELECT d FROM DocumentEntity d
            WHERE d.organisationId = :organisationId
              AND d.actif = true
              AND (
                  d.usagerId = :usagerId
                  OR d.dossierId IN (
                      SELECT dos.id FROM DossierEntity dos
                      WHERE dos.usagerId = :usagerId
                        AND dos.organisationId = :organisationId
                        AND dos.actif = true
                  )
              )
            ORDER BY d.createdAt DESC
            """)
    List<DocumentEntity> findByUsager(
            @Param("organisationId") UUID organisationId,
            @Param("usagerId") UUID usagerId
    );
}
