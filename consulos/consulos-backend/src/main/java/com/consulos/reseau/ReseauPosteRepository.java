package com.consulos.reseau;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReseauPosteRepository extends JpaRepository<ReseauPosteEntity, UUID> {

    List<ReseauPosteEntity> findByReseauId(UUID reseauId);

    List<ReseauPosteEntity> findByReseauIdIn(List<UUID> reseauIds);
}
