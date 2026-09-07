package com.consulos.sms;

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
@Table(name = "sms_logs")
public class SmsLogEntity {

    @Id
    private UUID id;

    @Column(name = "organisation_id", nullable = false)
    private UUID organisationId;

    @Column(nullable = false, length = 30)
    private String recipient;

    @Column(name = "message_body", nullable = false, columnDefinition = "TEXT")
    private String messageBody;

    @Column(nullable = false, length = 40)
    private String purpose;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "provider_ref", length = 100)
    private String providerRef;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "related_rdv_id")
    private UUID relatedRdvId;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
