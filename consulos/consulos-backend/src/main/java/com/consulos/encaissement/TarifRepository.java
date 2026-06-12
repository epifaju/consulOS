package com.consulos.encaissement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TarifRepository extends JpaRepository<TarifEntity, UUID> {

    List<TarifEntity> findByOrganisationIdAndActifTrue(UUID organisationId);

    Optional<TarifEntity> findByIdAndOrganisationId(UUID id, UUID organisationId);

    Optional<TarifEntity> findByTypeServiceIdAndOrganisationIdAndActifTrue(UUID typeServiceId, UUID organisationId);
}
