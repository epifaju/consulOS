package com.consulos.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organisation_id")
    private UUID organisationId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "entite_type", length = 100)
    private String entiteType;

    @Column(name = "entite_id")
    private UUID entiteId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ancien_etat", columnDefinition = "jsonb")
    private String ancienEtat;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "nouvel_etat", columnDefinition = "jsonb")
    private String nouvelEtat;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
