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
class UsagerDocumentListQueryServiceTest {

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
    void listDocuments_usagerNotFound() {
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.empty());

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
    void listDocuments_empty() {
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(activeUsager()));
        when(usagerDocumentQueryRepository.findByUsager(organisationId, usagerId)).thenReturn(List.of());

        List<UsagerDocumentSummaryDTO> result = service.listDocuments(usagerId, organisationId);

        assertThat(result).isEmpty();
        verify(dossierRepository, never()).findAllById(anyCollection());
    }

    @Test
    void listDocuments_happy_withDossierReference() {
        UUID dossierId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-09-09T10:00:00Z");

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
        doc.setCreatedAt(createdAt);

        DossierEntity dossier = new DossierEntity();
        dossier.setId(dossierId);
        dossier.setOrganisationId(organisationId);
        dossier.setUsagerId(usagerId);
        dossier.setReference("DOS-2026-042");
        dossier.setActif(true);

        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(activeUsager()));
        when(usagerDocumentQueryRepository.findByUsager(organisationId, usagerId)).thenReturn(List.of(doc));
        when(dossierRepository.findAllById(anyCollection())).thenReturn(List.of(dossier));

        List<UsagerDocumentSummaryDTO> result = service.listDocuments(usagerId, organisationId);

        assertThat(result).hasSize(1);
        UsagerDocumentSummaryDTO item = result.getFirst();
        assertThat(item.id()).isEqualTo(documentId);
        assertThat(item.dossierId()).isEqualTo(dossierId);
        assertThat(item.dossierReference()).isEqualTo("DOS-2026-042");
        assertThat(item.nomOriginal()).isEqualTo("carte-identite.pdf");
        assertThat(item.typeDocument()).isEqualTo("IDENTITE");
        assertThat(item.statutControle()).isEqualTo(StatutControle.RECU);
        assertThat(item.createdAt()).isEqualTo(createdAt);
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
