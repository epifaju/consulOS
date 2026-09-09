package com.consulos.document;

import com.consulos.document.dto.DocumentDTO;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentMapperTest {

    private final DocumentMapper mapper = new DocumentMapper();

    @Test
    void toDto_shouldMapEntityFields() {
        UUID id = UUID.randomUUID();
        UUID dossierId = UUID.randomUUID();
        UUID usagerId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-09-09T12:00:00Z");

        DocumentEntity entity = new DocumentEntity();
        entity.setId(id);
        entity.setDossierId(dossierId);
        entity.setUsagerId(usagerId);
        entity.setNomFichier("stored.pdf");
        entity.setNomOriginal("scan.pdf");
        entity.setTypeDocument("CNI");
        entity.setMimeType("application/pdf");
        entity.setTailleOctets(2048L);
        entity.setStatutControle(StatutControle.VALIDE);
        entity.setVersion(2);
        entity.setNotes("ok");
        entity.setCreatedAt(createdAt);

        DocumentDTO dto = mapper.toDto(entity);

        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.dossierId()).isEqualTo(dossierId);
        assertThat(dto.usagerId()).isEqualTo(usagerId);
        assertThat(dto.nomFichier()).isEqualTo("stored.pdf");
        assertThat(dto.nomOriginal()).isEqualTo("scan.pdf");
        assertThat(dto.typeDocument()).isEqualTo("CNI");
        assertThat(dto.mimeType()).isEqualTo("application/pdf");
        assertThat(dto.tailleOctets()).isEqualTo(2048L);
        assertThat(dto.statutControle()).isEqualTo(StatutControle.VALIDE);
        assertThat(dto.version()).isEqualTo(2);
        assertThat(dto.notes()).isEqualTo("ok");
        assertThat(dto.createdAt()).isEqualTo(createdAt);
    }
}
