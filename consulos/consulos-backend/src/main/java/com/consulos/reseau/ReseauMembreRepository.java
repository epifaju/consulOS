package com.consulos.reseau;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReseauMembreRepository extends JpaRepository<ReseauMembreEntity, UUID> {

    List<ReseauMembreEntity> findByUserId(UUID userId);

    Optional<ReseauMembreEntity> findByReseauIdAndUserId(UUID reseauId, UUID userId);

    boolean existsByReseauIdAndUserId(UUID reseauId, UUID userId);
}
