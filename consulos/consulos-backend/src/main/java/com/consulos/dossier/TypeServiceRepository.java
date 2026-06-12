package com.consulos.dossier;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TypeServiceRepository extends JpaRepository<TypeServiceEntity, UUID> {

    List<TypeServiceEntity> findByOrganisationIdAndActifTrue(UUID organisationId);

    Optional<TypeServiceEntity> findByIdAndOrganisationIdAndActifTrue(UUID id, UUID organisationId);
}
