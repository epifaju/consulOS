package com.consulos.dossier;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DossierRepository extends JpaRepository<DossierEntity, UUID>, JpaSpecificationExecutor<DossierEntity> {

    Optional<DossierEntity> findByIdAndOrganisationIdAndActifTrue(UUID id, UUID organisationId);

    List<DossierEntity> findByUsagerIdAndOrganisationIdAndActifTrue(UUID usagerId, UUID organisationId);

    @Query(value = "SELECT nextval('dossier_seq')", nativeQuery = true)
    Long nextDossierSequence();

    @Query("""
            SELECT d.statut, COUNT(d)
            FROM DossierEntity d
            WHERE d.organisationId = :organisationId AND d.actif = true
            GROUP BY d.statut
            """)
    List<Object[]> countByStatut(UUID organisationId);

    @Query("""
            SELECT ts.libelle, COUNT(d)
            FROM DossierEntity d
            JOIN TypeServiceEntity ts ON d.typeServiceId = ts.id
            WHERE d.organisationId = :organisationId AND d.actif = true
            GROUP BY ts.libelle
            """)
    List<Object[]> countByTypeService(UUID organisationId);
}
