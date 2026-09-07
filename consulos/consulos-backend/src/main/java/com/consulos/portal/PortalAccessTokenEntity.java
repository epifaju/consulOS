package com.consulos.portal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "portal_access_tokens")
public class PortalAccessTokenEntity {

    @Id
    private UUID id;

    @Column(name = "organisation_id", nullable = false)
    private UUID organisationId;

    @Column(name = "dossier_id", nullable = false)
    private UUID dossierId;

    @Column(name = "usager_id", nullable = false)
    private UUID usagerId;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "token_type", nullable = false, length = 20)
    private String tokenType;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
