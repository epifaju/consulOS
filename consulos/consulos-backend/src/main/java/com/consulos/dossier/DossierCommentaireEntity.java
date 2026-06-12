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
@Table(name = "dossier_commentaires")
public class DossierCommentaireEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "dossier_id", nullable = false)
    private UUID dossierId;

    @Column(name = "auteur_id")
    private UUID auteurId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenu;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
