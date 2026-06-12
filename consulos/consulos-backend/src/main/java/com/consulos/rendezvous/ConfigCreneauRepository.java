package com.consulos.rendezvous;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConfigCreneauRepository extends JpaRepository<ConfigCreneauEntity, UUID> {

    List<ConfigCreneauEntity> findByOrganisationIdAndJourSemaineAndActifTrue(
            UUID organisationId,
            Integer jourSemaine
    );

    List<ConfigCreneauEntity> findByOrganisationIdAndActifTrue(UUID organisationId);

    List<ConfigCreneauEntity> findByOrganisationId(UUID organisationId);
}
