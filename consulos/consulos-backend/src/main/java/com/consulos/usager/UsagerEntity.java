package com.consulos.usager;

import com.consulos.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "usagers")
public class UsagerEntity extends BaseEntity {

    @Column(length = 30, unique = true)
    private String numero;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(name = "lieu_naissance", length = 200)
    private String lieuNaissance;

    @Column(length = 3)
    private String nationalite;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_usager", length = 30)
    private TypeUsager typeUsager = TypeUsager.DIASPORA;

    @Column(length = 30)
    private String telephone;

    @Column(length = 255)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String adresse;

    @Column(name = "numero_document", length = 50)
    private String numeroDocument;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_document", length = 30)
    private TypeDocument typeDocument;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    private Boolean actif = true;
}
