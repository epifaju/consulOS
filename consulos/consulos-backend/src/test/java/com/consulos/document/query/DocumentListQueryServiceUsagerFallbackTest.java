package com.consulos.document.query;

import com.consulos.common.PageResponse;
import com.consulos.document.DocumentEntity;
import com.consulos.document.StatutControle;
import com.consulos.document.query.dto.DocumentListItemDTO;
import com.consulos.dossier.DossierEntity;
import com.consulos.dossier.DossierRepository;
import com.consulos.usager.UsagerEntity;
import com.consulos.usager.UsagerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentListQueryServiceUsagerFallbackTest {

    @Mock
    private DocumentListQueryRepository documentListQueryRepository;
    @Mock
    private DossierRepository dossierRepository;
    @Mock
    private UsagerRepository usagerRepository;

    @InjectMocks
    private DocumentListQueryService service;

    private UUID organisationId;
    private PageRequest pageable;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        pageable = PageRequest.of(0, 20);
    }

    @Test
    void list_whenDocUsagerIdNull_shouldResolveUsagerFromDossier() {
        UUID dossierId = UUID.randomUUID();
        UUID usagerId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        DocumentEntity doc = new DocumentEntity();
        doc.setId(documentId);
        doc.setOrganisationId(organisationId);
        doc.setDossierId(dossierId);
        doc.setUsagerId(null);
        doc.setNomFichier("scan.pdf");
        doc.setNomOriginal("scan.pdf");
        doc.setTypeDocument("AUTRE");
        doc.setMimeType("application/pdf");
        doc.setTailleOctets(512L);
        doc.setStatutControle(StatutControle.RECU);
        doc.setVersion(1);
        doc.setCreatedAt(OffsetDateTime.parse("2026-09-09T11:00:00Z"));

        DossierEntity dossier = new DossierEntity();
        dossier.setId(dossierId);
        dossier.setOrganisationId(organisationId);
        dossier.setUsagerId(usagerId);
        dossier.setReference("DOS-2026-050");
        dossier.setActif(true);

        UsagerEntity usager = new UsagerEntity();
        usager.setId(usagerId);
        usager.setOrganisationId(organisationId);
        usager.setActif(true);
        usager.setNom("Bah");
        usager.setPrenom("Ousmane");

        when(documentListQueryRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(doc), pageable, 1));
        when(dossierRepository.findAllById(anyCollection())).thenReturn(List.of(dossier));
        when(usagerRepository.findAllById(anyCollection())).thenReturn(List.of(usager));

        PageResponse<DocumentListItemDTO> result = service.list(
                organisationId, null, null, null, null, null, null, pageable);

        DocumentListItemDTO item = result.content().getFirst();
        assertThat(item.dossierReference()).isEqualTo("DOS-2026-050");
        assertThat(item.usagerId()).isEqualTo(usagerId);
        assertThat(item.usagerNom()).isEqualTo("Bah");
        assertThat(item.usagerPrenom()).isEqualTo("Ousmane");
    }

    @Test
    void list_whenInactiveDossierOrUsager_shouldFilterEnrichment() {
        UUID dossierId = UUID.randomUUID();
        UUID usagerId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        DocumentEntity doc = new DocumentEntity();
        doc.setId(documentId);
        doc.setOrganisationId(organisationId);
        doc.setDossierId(dossierId);
        doc.setUsagerId(usagerId);
        doc.setNomFichier("piece.pdf");
        doc.setNomOriginal("piece.pdf");
        doc.setTypeDocument("IDENTITE");
        doc.setMimeType("application/pdf");
        doc.setTailleOctets(256L);
        doc.setStatutControle(StatutControle.RECU);
        doc.setVersion(1);
        doc.setCreatedAt(OffsetDateTime.parse("2026-09-09T12:00:00Z"));

        DossierEntity inactiveDossier = new DossierEntity();
        inactiveDossier.setId(dossierId);
        inactiveDossier.setOrganisationId(organisationId);
        inactiveDossier.setUsagerId(usagerId);
        inactiveDossier.setReference("DOS-INACTIVE");
        inactiveDossier.setActif(false);

        UsagerEntity inactiveUsager = new UsagerEntity();
        inactiveUsager.setId(usagerId);
        inactiveUsager.setOrganisationId(organisationId);
        inactiveUsager.setActif(false);
        inactiveUsager.setNom("Keita");
        inactiveUsager.setPrenom("Sékou");

        when(documentListQueryRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(doc), pageable, 1));
        when(dossierRepository.findAllById(anyCollection())).thenReturn(List.of(inactiveDossier));
        when(usagerRepository.findAllById(anyCollection())).thenReturn(List.of(inactiveUsager));

        PageResponse<DocumentListItemDTO> result = service.list(
                organisationId, null, null, null, null, null, null, pageable);

        DocumentListItemDTO item = result.content().getFirst();
        assertThat(item.dossierId()).isEqualTo(dossierId);
        assertThat(item.dossierReference()).isNull();
        assertThat(item.usagerId()).isEqualTo(usagerId);
        assertThat(item.usagerNom()).isNull();
        assertThat(item.usagerPrenom()).isNull();
    }

    @Test
    void list_whenNoDossierIdAndNoUsagerId_shouldSkipLookups() {
        UUID documentId = UUID.randomUUID();

        DocumentEntity doc = new DocumentEntity();
        doc.setId(documentId);
        doc.setOrganisationId(organisationId);
        doc.setDossierId(null);
        doc.setUsagerId(null);
        doc.setNomFichier("orphan.pdf");
        doc.setNomOriginal("orphan.pdf");
        doc.setTypeDocument("AUTRE");
        doc.setMimeType("application/pdf");
        doc.setTailleOctets(128L);
        doc.setStatutControle(StatutControle.RECU);
        doc.setVersion(1);
        doc.setCreatedAt(OffsetDateTime.parse("2026-09-09T13:00:00Z"));

        when(documentListQueryRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(doc), pageable, 1));

        PageResponse<DocumentListItemDTO> result = service.list(
                organisationId, null, null, null, null, null, null, pageable);

        DocumentListItemDTO item = result.content().getFirst();
        assertThat(item.id()).isEqualTo(documentId);
        assertThat(item.dossierId()).isNull();
        assertThat(item.dossierReference()).isNull();
        assertThat(item.usagerId()).isNull();
        assertThat(item.usagerNom()).isNull();
        verify(dossierRepository, never()).findAllById(anyCollection());
        verify(usagerRepository, never()).findAllById(anyCollection());
    }
}
