package com.consulos.dossier;

import com.consulos.common.BaseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "dossiers")
public class DossierEntity extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String reference;

    @Column(name = "usager_id", nullable = false)
    private UUID usagerId;

    @Column(name = "type_service_id", nullable = false)
    private UUID typeServiceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DossierStatut statut = DossierStatut.BROUILLON;

    @Column(name = "agent_id")
    private UUID agentId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DossierPriorite priorite = DossierPriorite.NORMALE;

    @Column(name = "date_soumission")
    private OffsetDateTime dateSoumission;

    @Column(name = "date_echeance")
    private OffsetDateTime dateEcheance;

    @Column(name = "motif_rejet", columnDefinition = "TEXT")
    private String motifRejet;

    @Column(name = "notes_internes", columnDefinition = "TEXT")
    private String notesInternes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode metadata;

    private Boolean actif = true;
}
