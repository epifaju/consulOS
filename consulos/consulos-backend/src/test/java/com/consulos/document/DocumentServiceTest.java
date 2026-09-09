package com.consulos.document;

import com.consulos.common.BusinessException;
import com.consulos.document.dto.DocumentDTO;
import com.consulos.document.dto.DownloadUrlDTO;
import com.consulos.document.dto.StatutControleDTO;
import com.consulos.dossier.DossierEntity;
import com.consulos.dossier.DossierRepository;
import com.consulos.usager.UsagerEntity;
import com.consulos.usager.UsagerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DossierRepository dossierRepository;
    @Mock
    private UsagerRepository usagerRepository;
    @Mock
    private MinioStorageService minioStorageService;
    @Mock
    private DocumentMapper documentMapper;

    @InjectMocks
    private DocumentService documentService;

    private UUID organisationId;
    private UUID otherOrganisationId;
    private UUID dossierId;
    private UUID usagerId;
    private UUID uploaderUserId;
    private UUID documentId;
    private DocumentEntity documentEntity;
    private DocumentDTO documentDTO;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        otherOrganisationId = UUID.randomUUID();
        dossierId = UUID.randomUUID();
        usagerId = UUID.randomUUID();
        uploaderUserId = UUID.randomUUID();
        documentId = UUID.randomUUID();

        documentEntity = new DocumentEntity();
        documentEntity.setId(documentId);
        documentEntity.setOrganisationId(organisationId);
        documentEntity.setDossierId(dossierId);
        documentEntity.setUsagerId(usagerId);
        documentEntity.setNomFichier("passeport.pdf");
        documentEntity.setNomOriginal("passeport.pdf");
        documentEntity.setTypeDocument("PASSEPORT");
        documentEntity.setMimeType("application/pdf");
        documentEntity.setTailleOctets(1024L);
        documentEntity.setCheminStockage("orgs/" + organisationId + "/dossiers/" + dossierId + "/obj-passeport.pdf");
        documentEntity.setStatutControle(StatutControle.RECU);
        documentEntity.setVersion(1);
        documentEntity.setNotes(null);
        documentEntity.setActif(true);
        documentEntity.setCreatedAt(OffsetDateTime.parse("2026-09-09T10:00:00Z"));
        documentEntity.setCreatedBy(uploaderUserId);

        documentDTO = new DocumentDTO(
                documentId,
                dossierId,
                usagerId,
                "passeport.pdf",
                "passeport.pdf",
                "PASSEPORT",
                "application/pdf",
                1024L,
                StatutControle.RECU,
                1,
                null,
                documentEntity.getCreatedAt()
        );
    }

    @Test
    void upload_shouldPersistDocumentWithRecuStatus() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "passeport.pdf",
                "application/pdf",
                new byte[]{1, 2, 3}
        );
        DossierEntity dossier = new DossierEntity();
        dossier.setId(dossierId);
        dossier.setOrganisationId(organisationId);
        UsagerEntity usager = new UsagerEntity();
        usager.setId(usagerId);
        usager.setOrganisationId(organisationId);
        usager.setActif(true);

        when(dossierRepository.findByIdAndOrganisationIdAndActifTrue(dossierId, organisationId))
                .thenReturn(Optional.of(dossier));
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));
        when(minioStorageService.uploadFile(file, organisationId, dossierId))
                .thenReturn(documentEntity.getCheminStockage());
        when(documentRepository.save(any(DocumentEntity.class))).thenAnswer(invocation -> {
            DocumentEntity saved = invocation.getArgument(0);
            saved.setId(documentId);
            saved.setCreatedAt(documentEntity.getCreatedAt());
            return saved;
        });
        when(documentMapper.toDto(any(DocumentEntity.class))).thenReturn(documentDTO);

        DocumentDTO result = documentService.upload(
                file, dossierId, usagerId, "PASSEPORT", "scan", uploaderUserId, organisationId
        );

        ArgumentCaptor<DocumentEntity> captor = ArgumentCaptor.forClass(DocumentEntity.class);
        verify(documentRepository).save(captor.capture());
        DocumentEntity saved = captor.getValue();

        assertThat(saved.getOrganisationId()).isEqualTo(organisationId);
        assertThat(saved.getDossierId()).isEqualTo(dossierId);
        assertThat(saved.getUsagerId()).isEqualTo(usagerId);
        assertThat(saved.getNomOriginal()).isEqualTo("passeport.pdf");
        assertThat(saved.getTypeDocument()).isEqualTo("PASSEPORT");
        assertThat(saved.getMimeType()).isEqualTo("application/pdf");
        assertThat(saved.getTailleOctets()).isEqualTo(3L);
        assertThat(saved.getCheminStockage()).isEqualTo(documentEntity.getCheminStockage());
        assertThat(saved.getStatutControle()).isEqualTo(StatutControle.RECU);
        assertThat(saved.getNotes()).isEqualTo("scan");
        assertThat(saved.getCreatedBy()).isEqualTo(uploaderUserId);
        assertThat(saved.getActif()).isTrue();
        assertThat(result).isEqualTo(documentDTO);
    }

    @Test
    void upload_withoutFilename_shouldFallbackToDocument() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "",
                "application/pdf",
                new byte[]{1}
        );
        when(minioStorageService.uploadFile(file, organisationId, null)).thenReturn("orgs/key");
        when(documentRepository.save(any(DocumentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(documentMapper.toDto(any(DocumentEntity.class))).thenReturn(documentDTO);

        documentService.upload(file, null, null, "AUTRE", null, uploaderUserId, organisationId);

        ArgumentCaptor<DocumentEntity> captor = ArgumentCaptor.forClass(DocumentEntity.class);
        verify(documentRepository).save(captor.capture());
        assertThat(captor.getValue().getNomOriginal()).isEqualTo("document");
        assertThat(captor.getValue().getNomFichier()).isEqualTo("document");
        verify(dossierRepository, never()).findByIdAndOrganisationIdAndActifTrue(any(), any());
        verify(usagerRepository, never()).findById(any());
    }

    @Test
    void upload_whenDossierNotInOrganisation_shouldThrow() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "a.pdf", "application/pdf", new byte[]{1}
        );
        when(dossierRepository.findByIdAndOrganisationIdAndActifTrue(dossierId, organisationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.upload(
                file, dossierId, null, "PASSEPORT", null, uploaderUserId, organisationId
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("DOSSIER_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(minioStorageService, never()).uploadFile(any(), any(), any());
        verify(documentRepository, never()).save(any());
    }

    @Test
    void upload_whenUsagerWrongOrganisation_shouldThrow() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "a.pdf", "application/pdf", new byte[]{1}
        );
        UsagerEntity usager = new UsagerEntity();
        usager.setId(usagerId);
        usager.setOrganisationId(otherOrganisationId);
        usager.setActif(true);

        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        assertThatThrownBy(() -> documentService.upload(
                file, null, usagerId, "PASSEPORT", null, uploaderUserId, organisationId
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("USAGER_NOT_FOUND"));

        verify(minioStorageService, never()).uploadFile(any(), any(), any());
    }

    @Test
    void upload_whenUsagerInactive_shouldThrow() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "a.pdf", "application/pdf", new byte[]{1}
        );
        UsagerEntity usager = new UsagerEntity();
        usager.setId(usagerId);
        usager.setOrganisationId(organisationId);
        usager.setActif(false);

        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        assertThatThrownBy(() -> documentService.upload(
                file, null, usagerId, "PASSEPORT", null, uploaderUserId, organisationId
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("USAGER_NOT_FOUND"));
    }

    @Test
    void findById_shouldReturnMappedDto() {
        when(documentRepository.findByIdAndOrganisationIdAndActifTrue(documentId, organisationId))
                .thenReturn(Optional.of(documentEntity));
        when(documentMapper.toDto(documentEntity)).thenReturn(documentDTO);

        assertThat(documentService.findById(documentId, organisationId)).isEqualTo(documentDTO);
    }

    @Test
    void findById_whenOtherOrganisation_shouldThrowNotFound() {
        when(documentRepository.findByIdAndOrganisationIdAndActifTrue(documentId, otherOrganisationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.findById(documentId, otherOrganisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("DOCUMENT_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    void getDownloadUrl_shouldReturnPresignedUrlWithExpiry() {
        when(documentRepository.findByIdAndOrganisationIdAndActifTrue(documentId, organisationId))
                .thenReturn(Optional.of(documentEntity));
        when(minioStorageService.generatePresignedDownloadUrl(documentEntity.getCheminStockage()))
                .thenReturn("https://minio.local/presigned");

        OffsetDateTime before = OffsetDateTime.now();
        DownloadUrlDTO result = documentService.getDownloadUrl(documentId, organisationId);
        OffsetDateTime after = OffsetDateTime.now();

        assertThat(result.url()).isEqualTo("https://minio.local/presigned");
        assertThat(result.expiresAt()).isAfter(before.plusMinutes(14));
        assertThat(result.expiresAt()).isBefore(after.plusMinutes(16));
    }

    @Test
    void updateStatutControle_shouldUpdateStatutAndNotes() {
        when(documentRepository.findByIdAndOrganisationIdAndActifTrue(documentId, organisationId))
                .thenReturn(Optional.of(documentEntity));
        when(documentRepository.save(any(DocumentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DocumentDTO updatedDto = new DocumentDTO(
                documentId, dossierId, usagerId, "passeport.pdf", "passeport.pdf", "PASSEPORT",
                "application/pdf", 1024L, StatutControle.VALIDE, 1, "ok", documentEntity.getCreatedAt()
        );
        when(documentMapper.toDto(any(DocumentEntity.class))).thenReturn(updatedDto);

        DocumentDTO result = documentService.updateStatutControle(
                documentId,
                new StatutControleDTO(StatutControle.VALIDE, "ok"),
                organisationId
        );

        assertThat(documentEntity.getStatutControle()).isEqualTo(StatutControle.VALIDE);
        assertThat(documentEntity.getNotes()).isEqualTo("ok");
        assertThat(result.statutControle()).isEqualTo(StatutControle.VALIDE);
        verify(documentRepository).save(documentEntity);
    }

    @Test
    void updateStatutControle_whenNotesNull_shouldKeepExistingNotes() {
        documentEntity.setNotes("existing");
        when(documentRepository.findByIdAndOrganisationIdAndActifTrue(documentId, organisationId))
                .thenReturn(Optional.of(documentEntity));
        when(documentRepository.save(any(DocumentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(documentMapper.toDto(any(DocumentEntity.class))).thenReturn(documentDTO);

        documentService.updateStatutControle(
                documentId,
                new StatutControleDTO(StatutControle.LISIBLE, null),
                organisationId
        );

        assertThat(documentEntity.getStatutControle()).isEqualTo(StatutControle.LISIBLE);
        assertThat(documentEntity.getNotes()).isEqualTo("existing");
    }

    @Test
    void softDelete_shouldDeactivateAndDeleteMinioObject() {
        when(documentRepository.findByIdAndOrganisationIdAndActifTrue(documentId, organisationId))
                .thenReturn(Optional.of(documentEntity));
        when(documentRepository.save(any(DocumentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        documentService.softDelete(documentId, organisationId);

        assertThat(documentEntity.getActif()).isFalse();
        verify(documentRepository).save(documentEntity);
        verify(minioStorageService).deleteFile(documentEntity.getCheminStockage());
    }

    @Test
    void softDelete_whenNotFound_shouldNotTouchStorage() {
        when(documentRepository.findByIdAndOrganisationIdAndActifTrue(documentId, organisationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.softDelete(documentId, organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("DOCUMENT_NOT_FOUND"));

        verify(minioStorageService, never()).deleteFile(any());
        verify(documentRepository, never()).save(any());
    }

    @Test
    void listByDossier_shouldReturnMappedDocuments() {
        DossierEntity dossier = new DossierEntity();
        dossier.setId(dossierId);
        when(dossierRepository.findByIdAndOrganisationIdAndActifTrue(dossierId, organisationId))
                .thenReturn(Optional.of(dossier));
        when(documentRepository.findByDossierIdAndOrganisationIdAndActifTrueOrderByCreatedAtDesc(
                dossierId, organisationId
        )).thenReturn(List.of(documentEntity));
        when(documentMapper.toDto(documentEntity)).thenReturn(documentDTO);

        List<DocumentDTO> result = documentService.listByDossier(dossierId, organisationId);

        assertThat(result).containsExactly(documentDTO);
    }

    @Test
    void listByDossier_whenDossierMissing_shouldThrow() {
        when(dossierRepository.findByIdAndOrganisationIdAndActifTrue(dossierId, organisationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.listByDossier(dossierId, organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("DOSSIER_NOT_FOUND"));

        verify(documentRepository, never())
                .findByDossierIdAndOrganisationIdAndActifTrueOrderByCreatedAtDesc(any(), any());
    }
}
