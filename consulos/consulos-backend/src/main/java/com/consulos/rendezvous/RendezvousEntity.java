package com.consulos.rendezvous;

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

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "rendezvous")
public class RendezvousEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organisation_id", nullable = false)
    private UUID organisationId;

    @Column(name = "usager_id", nullable = false)
    private UUID usagerId;

    @Column(name = "dossier_id")
    private UUID dossierId;

    @Column(name = "type_service_id")
    private UUID typeServiceId;

    @Column(name = "agent_id")
    private UUID agentId;

    @Column(name = "date_heure", nullable = false)
    private OffsetDateTime dateHeure;

    @Column(name = "duree_minutes")
    private Integer dureeMinutes = 30;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RdvStatut statut = RdvStatut.CONFIRME;

    @Column(columnDefinition = "TEXT")
    private String motif;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "rappel_envoye")
    private Boolean rappelEnvoye = false;

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
