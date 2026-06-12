package com.consulos.dossier;

import com.consulos.common.BaseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "types_service")
public class TypeServiceEntity extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String libelle;

    @Column(name = "delai_jours")
    private Integer delaiJours = 30;

    @Column(name = "tarif_defaut", precision = 10, scale = 2)
    private BigDecimal tarifDefaut;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "workflow_config", columnDefinition = "jsonb")
    private JsonNode workflowConfig;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode checklist;

    private Boolean actif = true;
}
