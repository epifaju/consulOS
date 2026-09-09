package com.consulos.usager.query;

import com.consulos.common.BusinessException;
import com.consulos.document.DocumentEntity;
import com.consulos.document.StatutControle;
import com.consulos.dossier.DossierEntity;
import com.consulos.dossier.DossierRepository;
import com.consulos.usager.UsagerEntity;
import com.consulos.usager.UsagerRepository;
import com.consulos.usager.query.dto.UsagerDocumentSummaryDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsagerDocumentListQueryServiceEdgeCasesTest {

    @Mock
    private UsagerRepository usagerRepository;
    @Mock
    private UsagerDocumentQueryRepository usagerDocumentQueryRepository;
    @Mock
    private DossierRepository dossierRepository;

    @InjectMocks
    private UsagerDocumentListQueryService service;

    private UUID organisationId;
    private UUID usagerId;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        usagerId = UUID.randomUUID();
    }

    @Test
    void listDocuments_whenWrongOrganisation_shouldThrowUsagerNotFound() {
        UsagerEntity usager = activeUsager();
        usager.setOrganisationId(UUID.randomUUID());
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        assertThatThrownBy(() -> service.listDocuments(usagerId, organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("USAGER_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verifyNoInteractions(usagerDocumentQueryRepository, dossierRepository);
    }

    @Test
    void listDocuments_whenUsagerInactive_shouldThrowUsagerNotFound() {
        UsagerEntity usager = activeUsager();
        usager.setActif(false);
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        assertThatThrownBy(() -> service.listDocuments(usagerId, organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("USAGER_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verifyNoInteractions(usagerDocumentQueryRepository, dossierRepository);
    }

    @Test
    void listDocuments_whenDossierMissingOrWrongOrg_shouldNullReference() {
        UUID dossierId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        DocumentEntity doc = baseDocument(documentId, dossierId);
        DossierEntity foreignOrMissing = new DossierEntity();
        foreignOrMissing.setId(dossierId);
        foreignOrMissing.setOrganisationId(UUID.randomUUID());
        foreignOrMissing.setUsagerId(usagerId);
        foreignOrMissing.setReference("DOS-FOREIGN");
        foreignOrMissing.setActif(true);

        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(activeUsager()));
        when(usagerDocumentQueryRepository.findByUsager(organisationId, usagerId)).thenReturn(List.of(doc));
        when(dossierRepository.findAllById(anyCollection())).thenReturn(List.of(foreignOrMissing));

        List<UsagerDocumentSummaryDTO> result = service.listDocuments(usagerId, organisationId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().dossierId()).isEqualTo(dossierId);
        assertThat(result.getFirst().dossierReference()).isNull();
    }

    @Test
    void listDocuments_whenDossierIdNull_shouldNullReference() {
        UUID documentId = UUID.randomUUID();
        DocumentEntity doc = baseDocument(documentId, null);

        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(activeUsager()));
        when(usagerDocumentQueryRepository.findByUsager(organisationId, usagerId)).thenReturn(List.of(doc));

        List<UsagerDocumentSummaryDTO> result = service.listDocuments(usagerId, organisationId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().dossierId()).isNull();
        assertThat(result.getFirst().dossierReference()).isNull();
        verify(dossierRepository, never()).findAllById(anyCollection());
    }

    private DocumentEntity baseDocument(UUID documentId, UUID dossierId) {
        DocumentEntity doc = new DocumentEntity();
        doc.setId(documentId);
        doc.setOrganisationId(organisationId);
        doc.setDossierId(dossierId);
        doc.setUsagerId(usagerId);
        doc.setNomFichier("id.pdf");
        doc.setNomOriginal("carte-identite.pdf");
        doc.setTypeDocument("IDENTITE");
        doc.setMimeType("application/pdf");
        doc.setTailleOctets(2048L);
        doc.setStatutControle(StatutControle.RECU);
        doc.setVersion(1);
        doc.setCreatedAt(OffsetDateTime.parse("2026-09-09T10:00:00Z"));
        return doc;
    }

    private UsagerEntity activeUsager() {
        UsagerEntity usager = new UsagerEntity();
        usager.setId(usagerId);
        usager.setOrganisationId(organisationId);
        usager.setActif(true);
        usager.setNom("Sow");
        usager.setPrenom("Fatou");
        usager.setEmail("fatou@example.com");
        return usager;
    }
}
