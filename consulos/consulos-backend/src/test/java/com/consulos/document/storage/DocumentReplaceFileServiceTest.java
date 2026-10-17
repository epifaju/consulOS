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
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentReplaceFileServiceTest {

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
        document.setNomFichier("old.pdf");
        document.setNomOriginal("old.pdf");
        document.setCheminStockage("orgs/" + organisationId + "/old.pdf");
        document.setVersion(1);
        document.setMimeType("application/pdf");
        document.setTailleOctets(10L);
    }

    @Test
    void replaceFile_shouldUploadNewObjectAndBumpVersion() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "new.pdf", "", new byte[]{1, 2, 3});
        when(documentRepository.findByIdAndOrganisationIdAndActifTrue(documentId, organisationId))
                .thenReturn(Optional.of(document));
        when(minioStorageService.uploadFile(any(), eq(organisationId), eq(dossierId)))
                .thenReturn("orgs/" + organisationId + "/new.pdf");
        when(documentRepository.save(any(DocumentEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentDTO dto = service.replaceFile(documentId, file, organisationId);

        assertThat(dto.nomOriginal()).isEqualTo("new.pdf");
        assertThat(dto.version()).isEqualTo(2);
        assertThat(dto.tailleOctets()).isEqualTo(3L);
        assertThat(dto.mimeType()).isEqualTo("application/pdf");
        verify(minioStorageService).deleteFile("orgs/" + organisationId + "/old.pdf");
    }

    @Test
    void replaceFile_whenEmpty_shouldThrow() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> service.replaceFile(documentId, file, organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("DOCUMENT_FILE_EMPTY");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
        verify(minioStorageService, never()).uploadFile(any(), any(), any());
    }

    @Test
    void replaceFile_whenNotFound_shouldThrow() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "new.pdf", "application/pdf", new byte[]{1});
        when(documentRepository.findByIdAndOrganisationIdAndActifTrue(documentId, organisationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.replaceFile(documentId, file, organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("DOCUMENT_NOT_FOUND"));
    }
}
