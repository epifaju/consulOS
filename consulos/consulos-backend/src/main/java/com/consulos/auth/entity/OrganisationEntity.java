package com.consulos.auth.entity;

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
@Table(name = "organisations")
public class OrganisationEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 200)
    private String nom;

    @Column(name = "pays_code", nullable = false, length = 3)
    private String paysCode;

    @Column(name = "langue_defaut", length = 5)
    private String langueDefaut;

    @Column(name = "devise_defaut", length = 3)
    private String deviseDefaut;

    private Boolean actif;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
