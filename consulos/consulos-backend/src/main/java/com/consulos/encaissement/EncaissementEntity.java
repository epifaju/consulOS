package com.consulos.encaissement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "encaissements")
public class EncaissementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organisation_id", nullable = false)
    private UUID organisationId;

    @Column(name = "dossier_id")
    private UUID dossierId;

    @Column(name = "usager_id", nullable = false)
    private UUID usagerId;

    @Column(name = "numero_recu", nullable = false, unique = true, length = 30)
    private String numeroRecu;

    @Column(name = "montant_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal montantTotal;

    @Column(name = "montant_percu", precision = 10, scale = 2)
    private BigDecimal montantPercu = BigDecimal.ZERO;

    @Column(length = 3)
    private String devise = "EUR";

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement", length = 30)
    private ModePaiement modePaiement;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StatutEncaissement statut = StatutEncaissement.DU;

    @Column(name = "date_paiement")
    private OffsetDateTime datePaiement;

    @Column(name = "caissier_id")
    private UUID caissierId;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
