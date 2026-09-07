package com.consulos.portal;

import com.consulos.usager.UsagerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PortalUsagerLookupRepository extends JpaRepository<UsagerEntity, UUID> {

    Optional<UsagerEntity> findByIdAndOrganisationIdAndActifTrue(UUID id, UUID organisationId);
}
