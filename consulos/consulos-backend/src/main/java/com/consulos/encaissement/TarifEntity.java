package com.consulos.encaissement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "tarifs")
public class TarifEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organisation_id", nullable = false)
    private UUID organisationId;

    @Column(name = "type_service_id")
    private UUID typeServiceId;

    @Column(nullable = false, length = 200)
    private String libelle;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montant;

    @Column(length = 3)
    private String devise = "EUR";

    private Boolean actif = true;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
