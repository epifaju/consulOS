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
class DocumentListQueryServiceTest {

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
    void list_emptyPage_neverLoadsDossiersOrUsagers() {
        when(documentListQueryRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResponse<DocumentListItemDTO> result = service.list(
                organisationId, null, null, null, null, null, null, pageable);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        verify(dossierRepository, never()).findAllById(anyCollection());
        verify(usagerRepository, never()).findAllById(anyCollection());
    }

    @Test
    void list_happyPath_enrichesDossierAndUsager() {
        UUID dossierId = UUID.randomUUID();
        UUID usagerId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-09-09T10:00:00Z");

        DocumentEntity doc = new DocumentEntity();
        doc.setId(documentId);
        doc.setOrganisationId(organisationId);
        doc.setDossierId(dossierId);
        doc.setUsagerId(usagerId);
        doc.setNomFichier("passeport.pdf");
        doc.setNomOriginal("passeport.pdf");
        doc.setTypeDocument("PASSEPORT");
        doc.setMimeType("application/pdf");
        doc.setTailleOctets(1024L);
        doc.setStatutControle(StatutControle.RECU);
        doc.setVersion(1);
        doc.setCreatedAt(createdAt);

        DossierEntity dossier = new DossierEntity();
        dossier.setId(dossierId);
        dossier.setOrganisationId(organisationId);
        dossier.setUsagerId(usagerId);
        dossier.setReference("DOS-2026-001");
        dossier.setActif(true);

        UsagerEntity usager = new UsagerEntity();
        usager.setId(usagerId);
        usager.setOrganisationId(organisationId);
        usager.setActif(true);
        usager.setNom("Diallo");
        usager.setPrenom("Awa");

        when(documentListQueryRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(doc), pageable, 1));
        when(dossierRepository.findAllById(anyCollection())).thenReturn(List.of(dossier));
        when(usagerRepository.findAllById(anyCollection())).thenReturn(List.of(usager));

        PageResponse<DocumentListItemDTO> result = service.list(
                organisationId, null, null, null, null, null, null, pageable);

        assertThat(result.content()).hasSize(1);
        DocumentListItemDTO item = result.content().getFirst();
        assertThat(item.id()).isEqualTo(documentId);
        assertThat(item.dossierId()).isEqualTo(dossierId);
        assertThat(item.dossierReference()).isEqualTo("DOS-2026-001");
        assertThat(item.usagerId()).isEqualTo(usagerId);
        assertThat(item.usagerNom()).isEqualTo("Diallo");
        assertThat(item.usagerPrenom()).isEqualTo("Awa");
        assertThat(item.nomFichier()).isEqualTo("passeport.pdf");
        assertThat(item.statutControle()).isEqualTo(StatutControle.RECU);
    }

    @Test
    void list_wrongOrgDossier_filtered_dossierReferenceNull() {
        UUID dossierId = UUID.randomUUID();
        UUID otherOrgId = UUID.randomUUID();
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

        DossierEntity foreignDossier = new DossierEntity();
        foreignDossier.setId(dossierId);
        foreignDossier.setOrganisationId(otherOrgId);
        foreignDossier.setUsagerId(UUID.randomUUID());
        foreignDossier.setReference("DOS-FOREIGN");
        foreignDossier.setActif(true);

        when(documentListQueryRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(doc), pageable, 1));
        when(dossierRepository.findAllById(anyCollection())).thenReturn(List.of(foreignDossier));

        PageResponse<DocumentListItemDTO> result = service.list(
                organisationId, null, null, null, null, null, null, pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().dossierReference()).isNull();
        assertThat(result.content().getFirst().usagerId()).isNull();
        verify(usagerRepository, never()).findAllById(anyCollection());
    }
}
