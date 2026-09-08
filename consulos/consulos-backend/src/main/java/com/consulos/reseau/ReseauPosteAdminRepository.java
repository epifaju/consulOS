package com.consulos.reseau;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Requêtes admin postes (fichier neuf — ne pas étendre ReseauPosteRepository). */
public interface ReseauPosteAdminRepository extends JpaRepository<ReseauPosteEntity, UUID> {

    Optional<ReseauPosteEntity> findByReseauIdAndOrganisationId(UUID reseauId, UUID organisationId);

    boolean existsByReseauIdAndOrganisationId(UUID reseauId, UUID organisationId);

    void deleteByReseauIdAndOrganisationId(UUID reseauId, UUID organisationId);
}
