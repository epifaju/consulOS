package com.consulos.auth.repository;

import com.consulos.auth.entity.UserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    @EntityGraph(attributePaths = {"role"})
    Optional<UserEntity> findByEmail(String email);

    @Modifying
    @Query("UPDATE UserEntity u SET u.derniereConnexion = :connexion, u.updatedAt = :connexion WHERE u.id = :userId")
    void updateDerniereConnexion(@Param("userId") UUID userId, @Param("connexion") OffsetDateTime connexion);

    @Modifying
    @Query("UPDATE UserEntity u SET u.passwordHash = :passwordHash, u.updatedAt = :updatedAt WHERE u.id = :userId")
    void updatePassword(@Param("userId") UUID userId,
                        @Param("passwordHash") String passwordHash,
                        @Param("updatedAt") OffsetDateTime updatedAt);
}
