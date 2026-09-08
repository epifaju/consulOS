package com.consulos.reseau;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReseauRepository extends JpaRepository<ReseauEntity, UUID> {

    Optional<ReseauEntity> findByCode(String code);

    List<ReseauEntity> findByIdInAndActifTrue(List<UUID> ids);
}
