package com.consulos.reseau;

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
@Table(name = "reseau_membres")
public class ReseauMembreEntity extends BaseEntity {

    @Column(name = "reseau_id", nullable = false)
    private UUID reseauId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** ADMIN | LECTEUR */
    @Column(name = "role_reseau", nullable = false, length = 30)
    private String roleReseau;
}
