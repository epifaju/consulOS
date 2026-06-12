package com.consulos.dossier;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "dossier_historique")
public class DossierHistoriqueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "dossier_id", nullable = false)
    private UUID dossierId;

    @Column(name = "statut_de", length = 30)
    private String statutDe;

    @Column(name = "statut_vers", nullable = false, length = 30)
    private String statutVers;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
