package com.consulos.document;

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
@Table(name = "documents")
public class DocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organisation_id", nullable = false)
    private UUID organisationId;

    @Column(name = "dossier_id")
    private UUID dossierId;

    @Column(name = "usager_id")
    private UUID usagerId;

    @Column(name = "nom_fichier", nullable = false)
    private String nomFichier;

    @Column(name = "nom_original", nullable = false)
    private String nomOriginal;

    @Column(name = "type_document", nullable = false, length = 100)
    private String typeDocument;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "taille_octets")
    private Long tailleOctets;

    @Column(name = "chemin_stockage", nullable = false, length = 500)
    private String cheminStockage;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_controle", length = 30)
    private StatutControle statutControle = StatutControle.RECU;

    private Integer version = 1;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private Boolean actif = true;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

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
