package com.consulos.reseau;

import com.consulos.auth.entity.OrganisationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Lookup organisations depuis le package réseau (sans modifier OrganisationRepository).
 */
public interface ReseauOrganisationLookupRepository extends JpaRepository<OrganisationEntity, UUID> {

    Optional<OrganisationEntity> findByCode(String code);

    List<OrganisationEntity> findByIdIn(Collection<UUID> ids);
}
