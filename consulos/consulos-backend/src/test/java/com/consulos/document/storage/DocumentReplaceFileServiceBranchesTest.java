package com.consulos.document.storage;

import com.consulos.common.BusinessException;
import com.consulos.document.DocumentEntity;
import com.consulos.document.DocumentMapper;
import com.consulos.document.DocumentRepository;
import com.consulos.document.MinioStorageService;
import com.consulos.document.dto.DocumentDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentReplaceFileServiceBranchesTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private MinioStorageService minioStorageService;

    private DocumentReplaceFileService service;
    private UUID documentId;
    private UUID organisationId;
    private UUID dossierId;
    private DocumentEntity document;

    @BeforeEach
    void setUp() {
        service = new DocumentReplaceFileService(documentRepository, minioStorageService, new DocumentMapper());
        documentId = UUID.randomUUID();
        organisationId = UUID.randomUUID();
        dossierId = UUID.randomUUID();
        document = new DocumentEntity();
        document.setId(documentId);
        document.setOrganisationId(organisationId);
        document.setDossierId(dossierId);
        document.setNomOriginal("kept.pdf");
        document.setCheminStockage("orgs/" + organisationId + "/old.pdf");
        document.setVersion(null);
    }

    @Test
    void replaceFile_whenFileNull_shouldThrowEmpty() {
        assertThatThrownBy(() -> service.replaceFile(documentId, null, organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo("DOCUMENT_FILE_EMPTY"));
        verify(minioStorageService, never()).uploadFile(any(), any(), any());
    }

    @Test
    void replaceFile_whenOriginalNameBlank_shouldKeepPreviousName() {
        MockMultipartFile file = new MockMultipartFile("file", "", "application/pdf", new byte[]{1});
        when(documentRepository.findByIdAndOrganisationIdAndActifTrue(documentId, organisationId))
                .thenReturn(Optional.of(document));
        when(minioStorageService.uploadFile(any(), eq(organisationId), eq(dossierId)))
                .thenReturn("orgs/" + organisationId + "/new.pdf");
        when(documentRepository.save(any(DocumentEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentDTO dto = service.replaceFile(documentId, file, organisationId);

        assertThat(dto.nomOriginal()).isEqualTo("kept.pdf");
        assertThat(dto.version()).isEqualTo(1);
        verify(minioStorageService).deleteFile("orgs/" + organisationId + "/old.pdf");
    }

    @Test
    void replaceFile_whenSameKey_shouldNotDelete() {
        MockMultipartFile file = new MockMultipartFile("file", "same.pdf", "application/pdf", new byte[]{1});
        String key = document.getCheminStockage();
        when(documentRepository.findByIdAndOrganisationIdAndActifTrue(documentId, organisationId))
                .thenReturn(Optional.of(document));
        when(minioStorageService.uploadFile(any(), eq(organisationId), eq(dossierId))).thenReturn(key);
        when(documentRepository.save(any(DocumentEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        service.replaceFile(documentId, file, organisationId);

        verify(minioStorageService, never()).deleteFile(any());
    }

    @Test
    void replaceFile_whenDeleteOldFails_shouldStillReturnDto() {
        MockMultipartFile file = new MockMultipartFile("file", "new.pdf", "application/pdf", new byte[]{9});
        when(documentRepository.findByIdAndOrganisationIdAndActifTrue(documentId, organisationId))
                .thenReturn(Optional.of(document));
        when(minioStorageService.uploadFile(any(), eq(organisationId), eq(dossierId)))
                .thenReturn("orgs/" + organisationId + "/new.pdf");
        when(documentRepository.save(any(DocumentEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("minio down")).when(minioStorageService).deleteFile(any());

        DocumentDTO dto = service.replaceFile(documentId, file, organisationId);

        assertThat(dto.nomOriginal()).isEqualTo("new.pdf");
        verify(minioStorageService).deleteFile("orgs/" + organisationId + "/old.pdf");
    }
}
