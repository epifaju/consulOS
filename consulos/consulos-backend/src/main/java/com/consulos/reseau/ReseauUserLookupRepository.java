package com.consulos.reseau;

import com.consulos.auth.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ReseauUserLookupRepository extends JpaRepository<UserEntity, UUID> {

    List<UserEntity> findByIdIn(Collection<UUID> ids);
}
