package com.consulos.dossier;

import com.consulos.dossier.dto.TypeServiceDTO;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TypeServiceMapperTest {

    private final TypeServiceMapper mapper = new TypeServiceMapperImpl();

    @Test
    void toDto_shouldMapEntityFields() {
        UUID id = UUID.randomUUID();
        TypeServiceEntity entity = new TypeServiceEntity();
        entity.setId(id);
        entity.setCode("PASSEPORT");
        entity.setLibelle("Passeport");
        entity.setDelaiJours(15);
        entity.setTarifDefaut(new BigDecimal("50.00"));
        entity.setChecklist(JsonNodeFactory.instance.arrayNode());

        TypeServiceDTO dto = mapper.toDto(entity);

        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.code()).isEqualTo("PASSEPORT");
        assertThat(dto.libelle()).isEqualTo("Passeport");
        assertThat(dto.delaiJours()).isEqualTo(15);
        assertThat(dto.tarifDefaut()).isEqualByComparingTo("50.00");
        assertThat(dto.checklist()).isNotNull();
    }
}
