package com.consulos.portal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PortalAccessTokenRepository extends JpaRepository<PortalAccessTokenEntity, UUID> {

    @Query("""
            SELECT t FROM PortalAccessTokenEntity t
            WHERE t.tokenHash = :tokenHash
              AND t.tokenType = :tokenType
              AND t.consumedAt IS NULL
              AND t.expiresAt > :now
            """)
    Optional<PortalAccessTokenEntity> findValidToken(
            @Param("tokenHash") String tokenHash,
            @Param("tokenType") String tokenType,
            @Param("now") OffsetDateTime now
    );
}
