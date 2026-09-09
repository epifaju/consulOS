package com.consulos.usager;

import com.consulos.usager.dto.UsagerCreateDTO;
import com.consulos.usager.dto.UsagerDetailDTO;
import com.consulos.usager.dto.UsagerSummaryDTO;
import com.consulos.usager.dto.UsagerUpdateDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UsagerMapperTest {

    private final UsagerMapper mapper = new UsagerMapperImpl();

    @Test
    void toSummary_shouldMapEntityFields() {
        UUID id = UUID.randomUUID();
        UsagerEntity entity = baseEntity(id);

        UsagerSummaryDTO dto = mapper.toSummary(entity);

        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.numero()).isEqualTo("GNB-2026-00001");
        assertThat(dto.nom()).isEqualTo("Camara");
        assertThat(dto.prenom()).isEqualTo("Mamadou");
        assertThat(dto.nationalite()).isEqualTo("GNB");
        assertThat(dto.typeUsager()).isEqualTo(TypeUsager.DIASPORA);
        assertThat(dto.telephone()).isEqualTo("+33612345678");
        assertThat(dto.email()).isEqualTo("m@test.local");
        assertThat(dto.actif()).isTrue();
    }

    @Test
    void toDetail_shouldMapEntityFields() {
        UUID id = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UsagerEntity entity = baseEntity(id);
        entity.setOrganisationId(orgId);
        entity.setLieuNaissance("Conakry");
        entity.setAdresse("12 rue Test");
        entity.setNumeroDocument("P123");
        entity.setTypeDocument(TypeDocument.PASSEPORT);
        entity.setMetadata(Map.of("key", "value"));
        entity.setCreatedAt(LocalDateTime.of(2026, 1, 2, 10, 0));
        entity.setUpdatedAt(LocalDateTime.of(2026, 1, 3, 11, 0));

        UsagerDetailDTO dto = mapper.toDetail(entity);

        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.numero()).isEqualTo("GNB-2026-00001");
        assertThat(dto.nom()).isEqualTo("Camara");
        assertThat(dto.prenom()).isEqualTo("Mamadou");
        assertThat(dto.dateNaissance()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(dto.lieuNaissance()).isEqualTo("Conakry");
        assertThat(dto.nationalite()).isEqualTo("GNB");
        assertThat(dto.typeUsager()).isEqualTo(TypeUsager.DIASPORA);
        assertThat(dto.telephone()).isEqualTo("+33612345678");
        assertThat(dto.email()).isEqualTo("m@test.local");
        assertThat(dto.adresse()).isEqualTo("12 rue Test");
        assertThat(dto.numeroDocument()).isEqualTo("P123");
        assertThat(dto.typeDocument()).isEqualTo(TypeDocument.PASSEPORT);
        assertThat(dto.metadata()).containsEntry("key", "value");
        assertThat(dto.actif()).isTrue();
        assertThat(dto.organisationId()).isEqualTo(orgId);
        assertThat(dto.createdAt()).isEqualTo(LocalDateTime.of(2026, 1, 2, 10, 0));
        assertThat(dto.updatedAt()).isEqualTo(LocalDateTime.of(2026, 1, 3, 11, 0));
    }

    @Test
    void toEntity_shouldSetActifTrueEmptyMetadataAndIgnoreIdOrgNumero() {
        UsagerCreateDTO dto = new UsagerCreateDTO(
                "Camara",
                "Mamadou",
                LocalDate.of(1990, 1, 1),
                "Conakry",
                "GNB",
                TypeUsager.MANDATAIRE,
                "+33612345678",
                "m@test.local",
                "12 rue Test",
                "P123",
                TypeDocument.PASSEPORT
        );

        UsagerEntity entity = mapper.toEntity(dto);

        assertThat(entity.getNom()).isEqualTo("Camara");
        assertThat(entity.getPrenom()).isEqualTo("Mamadou");
        assertThat(entity.getDateNaissance()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(entity.getLieuNaissance()).isEqualTo("Conakry");
        assertThat(entity.getNationalite()).isEqualTo("GNB");
        assertThat(entity.getTypeUsager()).isEqualTo(TypeUsager.MANDATAIRE);
        assertThat(entity.getTelephone()).isEqualTo("+33612345678");
        assertThat(entity.getEmail()).isEqualTo("m@test.local");
        assertThat(entity.getAdresse()).isEqualTo("12 rue Test");
        assertThat(entity.getNumeroDocument()).isEqualTo("P123");
        assertThat(entity.getTypeDocument()).isEqualTo(TypeDocument.PASSEPORT);
        assertThat(entity.getActif()).isTrue();
        assertThat(entity.getMetadata()).isInstanceOf(HashMap.class).isEmpty();
        assertThat(entity.getId()).isNull();
        assertThat(entity.getOrganisationId()).isNull();
        assertThat(entity.getNumero()).isNull();
    }

    @Test
    void updateEntity_shouldIgnoreNullDtoFields() {
        UsagerEntity entity = baseEntity(UUID.randomUUID());
        entity.setLieuNaissance("Conakry");
        entity.setNationalite("GNB");
        entity.setTypeUsager(TypeUsager.DIASPORA);
        entity.setTelephone("+33612345678");
        entity.setEmail("m@test.local");
        entity.setAdresse("12 rue Test");
        entity.setNumeroDocument("P123");
        entity.setTypeDocument(TypeDocument.PASSEPORT);

        UsagerUpdateDTO partial = new UsagerUpdateDTO(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "nouveau@test.local",
                null,
                null,
                null
        );

        mapper.updateEntity(entity, partial);

        assertThat(entity.getNom()).isEqualTo("Camara");
        assertThat(entity.getPrenom()).isEqualTo("Mamadou");
        assertThat(entity.getDateNaissance()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(entity.getLieuNaissance()).isEqualTo("Conakry");
        assertThat(entity.getNationalite()).isEqualTo("GNB");
        assertThat(entity.getTypeUsager()).isEqualTo(TypeUsager.DIASPORA);
        assertThat(entity.getTelephone()).isEqualTo("+33612345678");
        assertThat(entity.getEmail()).isEqualTo("nouveau@test.local");
        assertThat(entity.getAdresse()).isEqualTo("12 rue Test");
        assertThat(entity.getNumeroDocument()).isEqualTo("P123");
        assertThat(entity.getTypeDocument()).isEqualTo(TypeDocument.PASSEPORT);
        assertThat(entity.getActif()).isTrue();
        assertThat(entity.getNumero()).isEqualTo("GNB-2026-00001");
    }

    private static UsagerEntity baseEntity(UUID id) {
        UsagerEntity entity = new UsagerEntity();
        entity.setId(id);
        entity.setOrganisationId(UUID.randomUUID());
        entity.setNumero("GNB-2026-00001");
        entity.setNom("Camara");
        entity.setPrenom("Mamadou");
        entity.setDateNaissance(LocalDate.of(1990, 1, 1));
        entity.setNationalite("GNB");
        entity.setTypeUsager(TypeUsager.DIASPORA);
        entity.setTelephone("+33612345678");
        entity.setEmail("m@test.local");
        entity.setActif(true);
        entity.setMetadata(new HashMap<>());
        return entity;
    }
}
