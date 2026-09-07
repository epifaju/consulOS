package com.consulos.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    Page<NotificationEntity> findByOrganisationIdAndRecipientUserIdOrderByCreatedAtDesc(
            UUID organisationId,
            UUID recipientUserId,
            Pageable pageable
    );

    long countByOrganisationIdAndRecipientUserIdAndLuFalse(UUID organisationId, UUID recipientUserId);

    Optional<NotificationEntity> findByIdAndOrganisationIdAndRecipientUserId(
            UUID id,
            UUID organisationId,
            UUID recipientUserId
    );

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE NotificationEntity n
            SET n.lu = true, n.luAt = :luAt, n.updatedAt = CURRENT_TIMESTAMP
            WHERE n.organisationId = :organisationId
              AND n.recipientUserId = :recipientUserId
              AND n.lu = false
            """)
    int markAllRead(
            @Param("organisationId") UUID organisationId,
            @Param("recipientUserId") UUID recipientUserId,
            @Param("luAt") LocalDateTime luAt
    );
}
