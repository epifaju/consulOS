package com.consulos.portal;

import com.consulos.dossier.DossierEntity;
import com.consulos.usager.UsagerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PortalDossierLookupRepository extends JpaRepository<DossierEntity, UUID> {

    @Query("""
            SELECT d FROM DossierEntity d
            WHERE UPPER(d.reference) = UPPER(:reference)
              AND d.actif = true
            """)
    Optional<DossierEntity> findActiveByReference(@Param("reference") String reference);
}
