package com.consulos.dossier.checklist;

import com.consulos.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "dossier_checklist_statuts")
public class DossierChecklistStatutEntity extends BaseEntity {

    @Column(name = "dossier_id", nullable = false)
    private UUID dossierId;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false)
    private Boolean valide = false;
}
