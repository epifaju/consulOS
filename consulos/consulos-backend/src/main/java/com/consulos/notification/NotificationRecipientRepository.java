package com.consulos.notification;

import com.consulos.auth.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRecipientRepository extends JpaRepository<UserEntity, UUID> {

    @Query("""
            SELECT u FROM UserEntity u
            JOIN FETCH u.role r
            WHERE u.organisationId = :organisationId
              AND u.actif = true
              AND r.code IN :roleCodes
            """)
    List<UserEntity> findActiveByOrgAndRoles(
            @Param("organisationId") UUID organisationId,
            @Param("roleCodes") Collection<String> roleCodes
    );

    Optional<UserEntity> findByIdAndOrganisationIdAndActifTrue(UUID id, UUID organisationId);
}
