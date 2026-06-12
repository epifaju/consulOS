package com.consulos.rendezvous;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JourFermetureRepository extends JpaRepository<JourFermetureEntity, UUID> {

    boolean existsByOrganisationIdAndDateFermeture(UUID organisationId, LocalDate dateFermeture);

    List<JourFermetureEntity> findByOrganisationIdOrderByDateFermetureAsc(UUID organisationId);

    Optional<JourFermetureEntity> findByIdAndOrganisationId(UUID id, UUID organisationId);
}
