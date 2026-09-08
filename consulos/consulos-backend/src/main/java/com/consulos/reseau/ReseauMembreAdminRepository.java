package com.consulos.reseau;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Requêtes membres par réseau (fichier neuf). */
public interface ReseauMembreAdminRepository extends JpaRepository<ReseauMembreEntity, UUID> {

    List<ReseauMembreEntity> findByReseauIdOrderByCreatedAtAsc(UUID reseauId);
}
